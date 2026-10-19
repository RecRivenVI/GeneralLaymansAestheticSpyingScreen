package io.github.recrivenvi.glass.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.recrivenvi.glass.GlassMod;
import io.github.recrivenvi.glass.client.GlassClientState;
import io.github.recrivenvi.glass.client.sound.GlassAmbienceSound;
import io.github.recrivenvi.glass.core.GlassPortalTransform;
import io.github.recrivenvi.glass.mixin.client.CameraAccessor;
import io.github.recrivenvi.glass.model.GameRendererProjectionAccess;
import io.github.recrivenvi.glass.model.GlassAnimationTimeline;
import io.github.recrivenvi.glass.model.GlassChannelProjectionPolicy;
import io.github.recrivenvi.glass.model.GlassChannelSnapshotIdentity;
import io.github.recrivenvi.glass.model.GlassExitPlaneCullingDelay;
import io.github.recrivenvi.glass.model.GlassObliqueProjection;
import io.github.recrivenvi.glass.model.GlassProjectionAnimation;
import io.github.recrivenvi.glass.model.GlassProjectionVisualState;
import io.github.recrivenvi.glass.model.GlassWirelessPropagation;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Native framebuffer translation of iChunUtil's stencil portal renderer. Each active channel owns
 * an independent world renderer and color/depth target. The real GLASS quads or wireless triangle
 * strip are then used as the mask in the main target, preserving window-like parallax without
 * relying on stencil support.
 */
public final class Blaze3DViewRenderer implements GlassViewRenderer {
    public static void registerShaders(net.minecraftforge.client.event.RegisterShadersEvent event) {
        GlassProjectionCompositor.registerShader(event);
    }

    private final GlassProjectionCompositor compositor = new GlassProjectionCompositor();
    private final Map<ViewKey, View> observedViews = new HashMap<>();
    private final Map<String, View> lastProjectors = new HashMap<>();
    private final Map<String, View> lastTerminals = new HashMap<>();
    private final Set<String> establishedChannels = new java.util.HashSet<>();
    private final Map<String, GlassChannelCaptureContext> channelContexts = new HashMap<>();
    private final Map<String, GlassAmbienceSound> ambienceSounds = new HashMap<>();
    private final Map<SurfaceKey, SurfaceTransition> surfaceTransitions = new HashMap<>();
    private final Map<GlobalPos, WirelessTransition> wirelessTransitions = new HashMap<>();
    private final Map<GlobalPos, ChannelSwitchAnimation> channelSwitchAnimations = new HashMap<>();
    private final Map<GlobalPos, GlassExitPlaneCullingDelay.State> exitPlaneCullingOverrides =
            new HashMap<>();
    private ClientLevel observedLevel;
    private boolean capturing;
    private final GlassChannelSnapshotIdentity channelSnapshotIdentity =
            new GlassChannelSnapshotIdentity();

    @Override
    public void replaceViews(List<GlassProjectorRenderState> states) {
        if (capturing) return;
        switchLevel(Minecraft.getInstance().level);
        Map<ViewKey, View> previous = new HashMap<>(observedViews);
        Map<ViewKey, View> current = new HashMap<>();
        for (GlassProjectorRenderState state : states) {
            View view = toView(state);
            current.put(view.key(), view);
        }
        double now = currentGameTime();
        Map<GlobalPos, Boolean> previousExitPlaneCulling = new HashMap<>();
        for (GlobalPos network : networks(previous, current)) {
            previousExitPlaneCulling.put(network, effectiveExitPlaneCulling(network, now));
        }
        Set<GlobalPos> handledNetworks = updateChannelSwitchAnimations(previous, current, now);
        Map<GlobalPos, GlassSurfaceTransitionPropagation.Plan> topologyPlans =
                topologyTransitionPlans(previous, current, handledNetworks);
        Map<ViewKey, View> unmatched = new HashMap<>(previous);
        for (View view : current.values()) {
            View old = unmatched.remove(view.key());
            if (!handledNetworks.contains(view.network())) {
                updateSurfaceTransitions(
                        old,
                        view,
                        now,
                        topologyPlans.getOrDefault(
                                view.network(), GlassSurfaceTransitionPropagation.Plan.empty()));
                updateWirelessTransition(old, view, now);
            }
            remember(view);
        }
        for (View removed : unmatched.values()) {
            View closed =
                    new View(
                            removed.network(),
                            removed.dimension(),
                            removed.pos(),
                            removed.facing(),
                            removed.channel(),
                            removed.terminal(),
                            removed.projector(),
                            false,
                            removed.propagationDistances(),
                            removed.activeFaces(),
                            removed.wirelessPositions());
            if (!handledNetworks.contains(removed.network())) {
                updateSurfaceTransitions(
                        removed,
                        closed,
                        now,
                        topologyPlans.getOrDefault(
                                removed.network(), GlassSurfaceTransitionPropagation.Plan.empty()));
                updateWirelessTransition(removed, closed, now);
            }
        }
        updateExitPlaneCulling(previous, current, previousExitPlaneCulling, handledNetworks, now);
        observedViews.clear();
        observedViews.putAll(current);
    }

    private Set<GlobalPos> updateChannelSwitchAnimations(
            Map<ViewKey, View> previous, Map<ViewKey, View> current, double now) {
        Map<GlobalPos, View> previousProjectors = projectors(previous);
        Map<GlobalPos, View> currentProjectors = projectors(current);
        Set<GlobalPos> networks = new java.util.HashSet<>(previousProjectors.keySet());
        networks.addAll(currentProjectors.keySet());
        Set<GlobalPos> handled = new java.util.HashSet<>();
        for (GlobalPos network : networks) {
            View oldProjector = previousProjectors.get(network);
            View newProjector = currentProjectors.get(network);
            List<View> oldViews = networkViews(previous, network);
            List<View> newViews = networkViews(current, network);
            boolean channelSwitch =
                    oldProjector != null
                            && newProjector != null
                            && GlassProjectionVisualState.isHotChannelSwitch(
                                    oldProjector.active(),
                                    oldProjector.channel(),
                                    newProjector.active(),
                                    newProjector.channel());

            if (channelSwitch) {
                scheduleChannelSwitch(oldViews, newViews, now);
                handled.add(network);
            }
        }
        return handled;
    }

    private static Map<GlobalPos, View> projectors(Map<ViewKey, View> views) {
        Map<GlobalPos, View> result = new HashMap<>();
        views.values().stream()
                .filter(View::projector)
                .forEach(view -> result.putIfAbsent(view.network(), view));
        return result;
    }

    private static List<View> networkViews(Map<ViewKey, View> views, GlobalPos network) {
        return views.values().stream().filter(view -> view.network().equals(network)).toList();
    }

    private static Set<GlobalPos> networks(
            Map<ViewKey, View> previous, Map<ViewKey, View> current) {
        Set<GlobalPos> result = new java.util.HashSet<>();
        previous.values().forEach(view -> result.add(view.network()));
        current.values().forEach(view -> result.add(view.network()));
        return result;
    }

    private static Map<GlobalPos, GlassSurfaceTransitionPropagation.Plan> topologyTransitionPlans(
            Map<ViewKey, View> previous,
            Map<ViewKey, View> current,
            Set<GlobalPos> handledNetworks) {
        Map<GlobalPos, View> previousProjectors = projectors(previous);
        Map<GlobalPos, View> currentProjectors = projectors(current);
        Map<GlobalPos, GlassSurfaceTransitionPropagation.Plan> result = new HashMap<>();
        for (GlobalPos network : networks(previous, current)) {
            View oldProjector = previousProjectors.get(network);
            View newProjector = currentProjectors.get(network);
            if (handledNetworks.contains(network)
                    || oldProjector == null
                    || newProjector == null
                    || !oldProjector.active()
                    || !newProjector.active()
                    || !oldProjector.channel().equals(newProjector.channel())
                    || !oldProjector.wirelessPositions().isEmpty()
                    || !newProjector.wirelessPositions().isEmpty()) {
                continue;
            }
            Map<GlassSurfaceTransitionPropagation.Surface, Integer> oldSurfaces =
                    activeSurfaceDistances(previous, network);
            Map<GlassSurfaceTransitionPropagation.Surface, Integer> newSurfaces =
                    activeSurfaceDistances(current, network);
            if (!oldSurfaces.keySet().equals(newSurfaces.keySet())) {
                result.put(
                        network,
                        GlassSurfaceTransitionPropagation.between(oldSurfaces, newSurfaces));
            }
        }
        return Map.copyOf(result);
    }

    private static Map<GlassSurfaceTransitionPropagation.Surface, Integer> activeSurfaceDistances(
            Map<ViewKey, View> views, GlobalPos network) {
        Map<GlassSurfaceTransitionPropagation.Surface, Integer> result = new HashMap<>();
        for (View view : views.values()) {
            if (!view.network().equals(network) || view.terminal() || !view.active()) continue;
            for (Direction face : view.activeFaces()) {
                result.merge(
                        new GlassSurfaceTransitionPropagation.Surface(view.pos(), face),
                        view.propagationDistance(face),
                        Math::min);
            }
        }
        return Map.copyOf(result);
    }

    private static Pair pair(List<View> views) {
        View terminal = views.stream().filter(View::terminal).findFirst().orElse(null);
        View projector = views.stream().filter(View::projector).findFirst().orElse(null);
        return terminal == null || projector == null ? null : new Pair(terminal, projector);
    }

    private static View toView(GlassProjectorRenderState state) {
        return new View(
                state.network,
                state.dimension,
                state.pos.immutable(),
                state.facing,
                state.channel,
                state.terminal,
                state.projector,
                state.active,
                Map.copyOf(state.propagationDistances),
                state.activeFaces.isEmpty()
                        ? EnumSet.noneOf(Direction.class)
                        : EnumSet.copyOf(state.activeFaces),
                List.copyOf(state.wirelessPositions));
    }

    private void remember(View view) {
        if (view.projector() && !view.channel().isEmpty()) {
            lastProjectors.put(networkKey(view), view);
        } else if (view.terminal() && !view.channel().isEmpty()) {
            lastTerminals.put(networkKey(view), view);
        }
    }

    private void updateSurfaceTransitions(
            View previous,
            View current,
            double now,
            GlassSurfaceTransitionPropagation.Plan topologyPlan) {
        for (Direction face : Direction.values()) {
            boolean wasOpen =
                    previous != null
                            && previous.active()
                            && GlassProjectionVisualState.usesBlockSurfaces(
                                    previous.projector(), previous.wirelessPositions().size())
                            && previous.activeFaces().contains(face);
            boolean isOpen =
                    current.active()
                            && GlassProjectionVisualState.usesBlockSurfaces(
                                    current.projector(), current.wirelessPositions().size())
                            && current.activeFaces().contains(face);
            SurfaceKey key = new SurfaceKey(current.network(), current.pos(), face);
            int duration = animationDuration();
            if (wasOpen == isOpen) continue;
            SurfaceTransition existing = surfaceTransitions.get(key);
            String channel =
                    isOpen
                            ? current.channel()
                            : previous == null ? current.channel() : previous.channel();
            int delay = GlassClientState.snapshot().settings().propagationDelayTicks();
            int propagationDistance =
                    isOpen || previous == null
                            ? current.propagationDistance(face)
                            : previous.propagationDistance(face);
            propagationDistance =
                    topologyPlan.distance(
                            new GlassSurfaceTransitionPropagation.Surface(current.pos(), face),
                            isOpen,
                            propagationDistance);
            double startTime = now + (double) propagationDistance * delay;
            GlassAnimationTimeline timeline =
                    existing == null || existing.finished(now)
                            ? GlassAnimationTimeline.startFull(isOpen, startTime, duration)
                            : existing.timeline().interrupt(isOpen, startTime, duration);
            surfaceTransitions.put(
                    key, new SurfaceTransition(current.network(), channel, timeline));
        }
    }

    private void updateWirelessTransition(View previous, View current, double now) {
        if (!current.projector()) return;
        boolean wasOpen =
                previous != null && previous.active() && previous.wirelessPositions().size() >= 2;
        boolean isOpen = current.active() && current.wirelessPositions().size() >= 2;
        if (wasOpen == isOpen) return;
        View geometry = isOpen ? current : previous;
        scheduleWireless(geometry, isOpen, now);
    }

    private void scheduleChannelSwitch(List<View> oldViews, List<View> newViews, double now) {
        View oldProjector = oldViews.stream().filter(View::projector).findFirst().orElse(null);
        View newProjector = newViews.stream().filter(View::projector).findFirst().orElse(null);
        if (oldProjector == null || newProjector == null) return;
        GlobalPos network = newProjector.network();
        ChannelSwitchAnimation previousSwitch = channelSwitchAnimations.remove(network);
        if (previousSwitch != null) previousSwitch.close();
        GlassChannelCaptureContext oldContext = channelContexts.remove(networkKey(network));
        GlassStateSnapshot.Settings settings = GlassClientState.snapshot().settings();
        boolean whiteMaskHandoff = settings.legacyFadeAnimation();
        int delay = settings.propagationDelayTicks();
        int duration =
                GlassProjectionAnimation.duration(
                        whiteMaskHandoff, settings.surfaceAnimationTicks());
        Map<SurfaceKey, GlassAnimationTimeline> oldSurfaceTimelines = new LinkedHashMap<>();
        double oldEndTime = now;
        double endTime = now;

        for (View view : oldViews) {
            if (view.terminal() || !view.active() || !view.wirelessPositions().isEmpty()) continue;
            for (Direction face : view.activeFaces()) {
                SurfaceKey key = new SurfaceKey(network, view.pos(), face);
                SurfaceTransition existing = surfaceTransitions.remove(key);
                double startTime = now + (double) view.propagationDistance(face) * delay;
                GlassAnimationTimeline timeline =
                        existing == null || existing.finished(now)
                                ? GlassAnimationTimeline.startFull(false, startTime, duration)
                                : existing.timeline().interrupt(false, startTime, duration);
                oldSurfaceTimelines.put(key, timeline);
                double handoffTime =
                        GlassProjectionAnimation.channelHandoffTime(whiteMaskHandoff, timeline);
                oldEndTime = Math.max(oldEndTime, handoffTime);
                endTime = Math.max(endTime, handoffTime);
            }
        }

        for (View view : newViews) {
            if (view.terminal() || !view.active() || !view.wirelessPositions().isEmpty()) continue;
            for (Direction face : view.activeFaces()) {
                SurfaceKey key = new SurfaceKey(network, view.pos(), face);
                GlassAnimationTimeline oldTimeline = oldSurfaceTimelines.get(key);
                double startTime =
                        oldTimeline == null
                                ? now
                                        + (double) view.propagationDistance(face) * delay
                                        + (whiteMaskHandoff ? 0 : duration)
                                : GlassProjectionAnimation.channelHandoffTime(
                                        whiteMaskHandoff, oldTimeline);
                GlassAnimationTimeline timeline =
                        GlassAnimationTimeline.startFull(true, startTime, duration);
                surfaceTransitions.put(
                        key, new SurfaceTransition(network, view.channel(), timeline));
                endTime = Math.max(endTime, timeline.endTime());
            }
        }

        WirelessTransition oldWireless = null;
        if (oldProjector.wirelessPositions().size() >= 2) {
            WirelessTransition existing = wirelessTransitions.remove(network);
            int triangleCount = oldProjector.wirelessPositions().size() - 1;
            ArrayList<GlassAnimationTimeline> timelines = new ArrayList<>(triangleCount);
            for (int index = 0; index < triangleCount; index++) {
                double startTime = GlassWirelessPropagation.sourceFirstStartTime(now, index, delay);
                GlassAnimationTimeline old =
                        existing != null && index < existing.timelines().size()
                                ? existing.timelines().get(index)
                                : null;
                GlassAnimationTimeline timeline =
                        old == null || old.finished(now)
                                ? GlassAnimationTimeline.startFull(false, startTime, duration)
                                : old.interrupt(false, startTime, duration);
                timelines.add(timeline);
                double handoffTime =
                        GlassProjectionAnimation.channelHandoffTime(whiteMaskHandoff, timeline);
                oldEndTime = Math.max(oldEndTime, handoffTime);
                endTime = Math.max(endTime, handoffTime);
            }
            oldWireless =
                    new WirelessTransition(
                            network,
                            oldProjector.pos(),
                            oldProjector.facing(),
                            oldProjector.channel(),
                            List.copyOf(oldProjector.wirelessPositions()),
                            List.copyOf(timelines));
        }

        if (newProjector.wirelessPositions().size() >= 2) {
            int triangleCount = newProjector.wirelessPositions().size() - 1;
            ArrayList<GlassAnimationTimeline> timelines = new ArrayList<>(triangleCount);
            for (int index = 0; index < triangleCount; index++) {
                GlassAnimationTimeline old =
                        oldWireless != null && index < oldWireless.timelines().size()
                                ? oldWireless.timelines().get(index)
                                : null;
                double startTime =
                        old == null
                                ? GlassWirelessPropagation.sourceFirstStartTime(now, index, delay)
                                        + (whiteMaskHandoff ? 0 : duration)
                                : GlassProjectionAnimation.channelHandoffTime(
                                        whiteMaskHandoff, old);
                GlassAnimationTimeline timeline =
                        GlassAnimationTimeline.startFull(true, startTime, duration);
                timelines.add(timeline);
                endTime = Math.max(endTime, timeline.endTime());
            }
            wirelessTransitions.put(
                    network,
                    new WirelessTransition(
                            network,
                            newProjector.pos(),
                            newProjector.facing(),
                            newProjector.channel(),
                            List.copyOf(newProjector.wirelessPositions()),
                            List.copyOf(timelines)));
        }

        channelSwitchAnimations.put(
                network,
                new ChannelSwitchAnimation(
                        network,
                        oldContext,
                        pair(oldViews),
                        Map.copyOf(oldSurfaceTimelines),
                        oldWireless,
                        shouldCullExitPlane(oldViews, network),
                        whiteMaskHandoff,
                        oldEndTime,
                        endTime));
    }

    private double scheduleWireless(View geometry, boolean opening, double now) {
        int triangleCount = Math.max(1, geometry.wirelessPositions().size() - 1);
        int delay = GlassClientState.snapshot().settings().propagationDelayTicks();
        int duration = animationDuration();
        WirelessTransition existing = wirelessTransitions.get(geometry.network());
        ArrayList<GlassAnimationTimeline> timelines = new ArrayList<>(triangleCount);
        double endTime = now;
        for (int index = 0; index < triangleCount; index++) {
            double startTime = GlassWirelessPropagation.sourceFirstStartTime(now, index, delay);
            GlassAnimationTimeline old =
                    existing != null && index < existing.timelines().size()
                            ? existing.timelines().get(index)
                            : null;
            GlassAnimationTimeline timeline;
            if (old == null || old.finished(now)) {
                timeline = GlassAnimationTimeline.startFull(opening, startTime, duration);
            } else {
                timeline = old.interrupt(opening, startTime, duration);
            }
            timelines.add(timeline);
            endTime = Math.max(endTime, timeline.endTime());
        }
        wirelessTransitions.put(
                geometry.network(),
                new WirelessTransition(
                        geometry.network(),
                        geometry.pos(),
                        geometry.facing(),
                        geometry.channel(),
                        List.copyOf(geometry.wirelessPositions()),
                        List.copyOf(timelines)));
        return endTime;
    }

    private static int animationDuration() {
        GlassStateSnapshot.Settings settings = GlassClientState.snapshot().settings();
        return GlassProjectionAnimation.duration(
                settings.legacyFadeAnimation(), settings.surfaceAnimationTicks());
    }

    private static GlassProjectionAnimation.Frame animationFrame(
            GlassAnimationTimeline timeline, double now) {
        return GlassProjectionAnimation.frame(
                GlassClientState.snapshot().settings().legacyFadeAnimation(), timeline, now);
    }

    @Override
    public void capture(float partialTick, long finishNanoTime) {
        Minecraft client = Minecraft.getInstance();
        if (capturing || client.level == null || client.player == null) return;
        switchLevel(client.level);
        GameRenderer renderer = client.gameRenderer;
        Camera camera = renderer.getMainCamera();
        Frustum frustum = client.levelRenderer.getFrustum();
        List<Pair> currentPairs = findPairs(client);
        List<Pair> pairs =
                includeClosingPairs(currentPairs).stream()
                        .filter(pair -> isNetworkObserved(client, pair))
                        .filter(pair -> isNetworkVisible(pair.projector().network(), frustum))
                        .toList();
        retainActiveChannels(currentPairs);
        updateAmbience(client, currentPairs);
        if (pairs.isEmpty() && channelSwitchAnimations.isEmpty()) return;
        RenderTarget originalTarget = client.getMainRenderTarget();
        CameraAccessor access = (CameraAccessor) camera;
        Vec3 originalPosition = camera.getPosition();
        float yaw = camera.getYRot(), pitch = camera.getXRot();
        Quaternionf orientation = new Quaternionf(camera.rotation());
        Matrix4f projection =
                new Matrix4f(((GameRendererProjectionAccess) renderer).glass$getLevelProjection());
        capturing = true;
        try {
            for (Pair pair : pairs) {
                GlassChannelCaptureContext channel =
                        channelContexts.computeIfAbsent(
                                networkKey(pair.projector()),
                                ignored -> new GlassChannelCaptureContext());
                try (GlassChannelSnapshotIdentity.Scope ignored = channelSnapshotIdentity.open()) {
                    if (!channel.ensure(
                            client,
                            originalTarget.width,
                            originalTarget.height,
                            pair.terminal().dimension(),
                            pair.terminal().pos())) continue;
                    capturePair(
                            client,
                            camera,
                            access,
                            partialTick,
                            finishNanoTime,
                            originalPosition,
                            orientation,
                            projection,
                            pair,
                            channel,
                            effectiveExitPlaneCulling(pair.projector().network()));
                } catch (RuntimeException exception) {
                    GlassMod.LOGGER.error(
                            "Failed to render GLASS channel {}",
                            pair.terminal().channel(),
                            exception);
                }
            }
            for (ChannelSwitchAnimation animation : channelSwitchAnimations.values()) {
                animation.releaseOldIfFinished(currentGameTime());
                if (animation.oldPair == null
                        || animation.oldContext == null
                        || animation.oldContext.target == null
                        || animation.oldContext.level == null) continue;
                if (!isNetworkObserved(client, animation.oldPair)
                        || !isNetworkVisible(animation.oldPair.projector().network(), frustum))
                    continue;
                try (GlassChannelSnapshotIdentity.Scope ignored = channelSnapshotIdentity.open()) {
                    capturePair(
                            client,
                            camera,
                            access,
                            partialTick,
                            finishNanoTime,
                            originalPosition,
                            orientation,
                            projection,
                            animation.oldPair,
                            animation.oldContext,
                            animation.oldExitPlaneCulling);
                } catch (RuntimeException exception) {
                    GlassMod.LOGGER.error(
                            "Failed to render outgoing GLASS channel {}",
                            animation.oldPair.terminal().channel(),
                            exception);
                }
            }
        } finally {
            access.glass$setPosition(originalPosition);
            setCameraOrientation(access, yaw, pitch, orientation);
            ((io.github.recrivenvi.glass.mixin.client.MinecraftRenderTargetAccessor) client)
                    .glass$setMainRenderTarget(originalTarget);
            originalTarget.bindWrite(true);
            RenderSystem.setProjectionMatrix(
                    projection, com.mojang.blaze3d.vertex.VertexSorting.DISTANCE_TO_ORIGIN);
            capturing = false;
        }
    }

    private List<Pair> includeClosingPairs(List<Pair> currentPairs) {
        Map<String, Pair> result = new LinkedHashMap<>();
        currentPairs.forEach(pair -> result.put(networkKey(pair.projector()), pair));
        Set<GlobalPos> closingNetworks = new java.util.LinkedHashSet<>();
        double now = currentGameTime();
        surfaceTransitions.values().stream()
                .filter(transition -> !transition.finished(now))
                .map(SurfaceTransition::network)
                .forEach(closingNetworks::add);
        wirelessTransitions.values().stream()
                .filter(transition -> !transition.finished(now))
                .map(WirelessTransition::network)
                .forEach(closingNetworks::add);
        closingNetworks.removeAll(channelSwitchAnimations.keySet());
        for (GlobalPos network : closingNetworks) {
            String key = networkKey(network);
            if (result.containsKey(key)) continue;
            View terminal = lastTerminals.get(key);
            View projector = lastProjectors.get(key);
            if (terminal != null && projector != null)
                result.put(key, new Pair(terminal, projector));
        }
        return List.copyOf(result.values());
    }

    private void capturePair(
            Minecraft client,
            Camera camera,
            CameraAccessor access,
            float partialTick,
            long finishNanoTime,
            Vec3 originalPosition,
            Quaternionf originalOrientation,
            Matrix4f originalProjection,
            Pair pair,
            GlassChannelCaptureContext channel,
            boolean cullExitPlane) {
        View terminal = pair.terminal(), projector = pair.projector();
        Direction destinationFace = terminal.facing().getOpposite();
        Vec3 sourceCenter = Vec3.atCenterOf(projector.pos());
        Vec3 destinationCenter = Vec3.atCenterOf(terminal.pos().relative(destinationFace));
        GlassPortalTransform transform =
                GlassPortalTransform.fromPlanes(
                        projector.facing(), Direction.UP, destinationFace, Direction.UP);
        Vec3 transformedPosition =
                transform.transformPosition(originalPosition, sourceCenter, destinationCenter);
        Quaternionf orientation = transform.transformOrientation(originalOrientation);
        var angles = GlassPortalTransform.cameraAngles(orientation);
        access.glass$setPosition(transformedPosition);
        setCameraOrientation(access, channel.unwrapYaw(angles.yaw()), angles.pitch(), orientation);
        Matrix4f view = new Matrix4f().rotation(orientation.conjugate(new Quaternionf()));
        Matrix4f projection = new Matrix4f(originalProjection);
        if (cullExitPlane)
            projection =
                    applyExitPlane(
                            projection,
                            view,
                            transformedPosition,
                            GlassExitPlane.blockAlignedCenter(destinationCenter, destinationFace),
                            destinationFace);
        channel.animateAt(transformedPosition);
        ((io.github.recrivenvi.glass.mixin.client.MinecraftRenderTargetAccessor) client)
                .glass$setMainRenderTarget(channel.target);
        channel.target.bindWrite(true);
        RenderSystem.setProjectionMatrix(
                projection, com.mojang.blaze3d.vertex.VertexSorting.DISTANCE_TO_ORIGIN);
        var pose = new PoseStack();
        pose.mulPoseMatrix(view);
        channel.renderer.prepareCullFrustum(
                pose,
                transformedPosition,
                GlassChannelProjectionPolicy.forOcclusionGraph(originalProjection));
        channel.renderer.renderLevel(
                pose,
                partialTick,
                finishNanoTime,
                false,
                camera,
                client.gameRenderer,
                client.gameRenderer.lightTexture(),
                projection);
    }

    private boolean effectiveExitPlaneCulling(GlobalPos network) {
        return effectiveExitPlaneCulling(network, currentGameTime());
    }

    private boolean effectiveExitPlaneCulling(GlobalPos network, double now) {
        GlassExitPlaneCullingDelay.State override = exitPlaneCullingOverrides.get(network);
        if (override != null) {
            if (override.active(now)) return override.enabled();
            exitPlaneCullingOverrides.remove(network);
        }
        if (hasActiveProjection(observedViews, network)) {
            return shouldCullExitPlane(observedViews.values(), network);
        }
        return false;
    }

    private static boolean shouldCullExitPlane(Iterable<View> views, GlobalPos network) {
        EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
        boolean wireless = false;
        for (View view : views) {
            if (!view.network().equals(network) || !view.active()) continue;
            wireless |= view.projector() && !view.wirelessPositions().isEmpty();
            faces.addAll(view.activeFaces());
        }
        return GlassExitPlane.shouldCull(faces, wireless);
    }

    private void updateExitPlaneCulling(
            Map<ViewKey, View> previous,
            Map<ViewKey, View> current,
            Map<GlobalPos, Boolean> previousEffectiveCulling,
            Set<GlobalPos> handledNetworks,
            double now) {
        for (GlobalPos network : networks(previous, current)) {
            if (handledNetworks.contains(network)) continue;
            boolean target =
                    hasActiveProjection(current, network)
                            && shouldCullExitPlane(current.values(), network);
            boolean displayed = previousEffectiveCulling.getOrDefault(network, false);
            double closingEnd = closingSurfaceEnd(previous, current, network, now);
            GlassExitPlaneCullingDelay.State updated =
                    GlassExitPlaneCullingDelay.update(
                            exitPlaneCullingOverrides.get(network),
                            displayed,
                            target,
                            closingEnd,
                            now);
            if (updated == null) exitPlaneCullingOverrides.remove(network);
            else exitPlaneCullingOverrides.put(network, updated);
        }
    }

    private double closingSurfaceEnd(
            Map<ViewKey, View> previous,
            Map<ViewKey, View> current,
            GlobalPos network,
            double now) {
        Map<GlassSurfaceTransitionPropagation.Surface, Integer> oldSurfaces =
                activeSurfaceDistances(previous, network);
        Set<GlassSurfaceTransitionPropagation.Surface> newSurfaces =
                activeSurfaceDistances(current, network).keySet();
        double result = now;
        for (GlassSurfaceTransitionPropagation.Surface surface : oldSurfaces.keySet()) {
            if (newSurfaces.contains(surface)) continue;
            SurfaceTransition transition =
                    surfaceTransitions.get(new SurfaceKey(network, surface.pos(), surface.face()));
            if (transition != null) result = Math.max(result, transition.timeline().endTime());
        }
        return result;
    }

    private static boolean hasActiveProjection(Map<ViewKey, View> views, GlobalPos network) {
        return views.values().stream()
                .anyMatch(
                        view ->
                                view.network().equals(network)
                                        && !view.terminal()
                                        && view.active());
    }

    private static Matrix4f applyExitPlane(
            org.joml.Matrix4fc projection,
            Matrix4f viewRotation,
            Vec3 cameraPosition,
            Vec3 planeCenter,
            Direction keepDirection) {
        Vector3f normal =
                new Vector3f(
                        keepDirection.getStepX(),
                        keepDirection.getStepY(),
                        keepDirection.getStepZ());
        float distance =
                (float)
                        (normal.x * (cameraPosition.x - planeCenter.x)
                                + normal.y * (cameraPosition.y - planeCenter.y)
                                + normal.z * (cameraPosition.z - planeCenter.z));
        viewRotation.transformDirection(normal);
        return GlassObliqueProjection.applyForwardZ(
                projection, new Vector4f(normal.x, normal.y, normal.z, distance), false);
    }

    private static void setCameraOrientation(
            CameraAccessor camera, float yaw, float pitch, Quaternionf orientation) {
        // 先复用原版入口更新欧拉角元数据并让两个视图矩阵缓存失效，再补上原版入口无法表达的 roll。
        camera.glass$setRotation(yaw, pitch);
        Quaternionf rotation = camera.glass$getRotation().set(orientation).normalize();
        camera.glass$getForwards().set(0.0F, 0.0F, -1.0F).rotate(rotation);
        camera.glass$getUp().set(0.0F, 1.0F, 0.0F).rotate(rotation);
        camera.glass$getLeft().set(-1.0F, 0.0F, 0.0F).rotate(rotation);
    }

    @Override
    public void drawProjector(Matrix4f viewRotation, Vec3 camera, Frustum mainFrustum) {
        Minecraft client = Minecraft.getInstance();
        if (capturing || client.level == null) {
            return;
        }
        List<Pair> pairs = findPairs(client);
        pairs =
                pairs.stream()
                        .filter(pair -> isNetworkObserved(client, pair))
                        .filter(pair -> isNetworkVisible(pair.projector().network(), mainFrustum))
                        .toList();

        ArrayList<PendingDraw> pendingTextures = new ArrayList<>();
        PoseStack poseStack = new PoseStack();
        poseStack.mulPoseMatrix(viewRotation);
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Matrix4f matrix = poseStack.last().pose();
        double now = client.level.getGameTime() + client.getFrameTime();
        appendOutgoingChannels(client, matrix, mainFrustum, now, pendingTextures);
        for (Pair pair : pairs) {
            String key = networkKey(pair.projector());
            GlassChannelCaptureContext channel = channelContexts.get(key);
            if (channel == null || channel.target == null) {
                continue;
            }
            View projector = pair.projector();
            if (projector.active() && projector.wirelessPositions().size() >= 2) {
                GlassStagedVertexBuffer.Draw draw = appendWirelessDraw(matrix, projector, now);
                if (draw != null)
                    pendingTextures.add(
                            new PendingDraw(
                                    GlassProjectionCompositor.WIRELESS_PIPELINE,
                                    channel.target,
                                    draw));
            } else if (projector.wirelessPositions().isEmpty()) {
                GlassStagedVertexBuffer.Draw draw =
                        appendSurfaceDraw(client, matrix, projector.network(), now);
                if (draw != null)
                    pendingTextures.add(
                            new PendingDraw(
                                    GlassProjectionCompositor.SURFACE_PIPELINE,
                                    channel.target,
                                    draw));
            }
        }
        appendClosingSurfaces(client, matrix, now, pendingTextures);
        appendClosingWireless(matrix, now, pendingTextures);
        poseStack.popPose();

        if (!pendingTextures.isEmpty()) {
            compositor.upload();
            for (PendingDraw draw : pendingTextures) {
                compositor.draw(client, draw.pipeline(), draw.target(), draw.draw());
            }
        }
        compositor.endFrame();
        surfaceTransitions.entrySet().removeIf(entry -> entry.getValue().finished(now));
        wirelessTransitions.entrySet().removeIf(entry -> entry.getValue().finished(now));
        channelSwitchAnimations
                .entrySet()
                .removeIf(
                        entry -> {
                            if (!entry.getValue().finished(now)) return false;
                            entry.getValue().close();
                            return true;
                        });
        exitPlaneCullingOverrides.values().removeIf(override -> !override.active(now));
    }

    private void appendOutgoingChannels(
            Minecraft client,
            Matrix4f matrix,
            Frustum mainFrustum,
            double now,
            List<PendingDraw> pendingTextures) {
        VertexFormat surfaceFormat = GlassProjectionCompositor.SURFACE_PIPELINE.getVertexFormat();
        for (ChannelSwitchAnimation animation : channelSwitchAnimations.values()) {
            animation.releaseOldIfFinished(now);
            GlassChannelCaptureContext channel = animation.oldContext;
            if (channel == null || channel.target == null) continue;
            if (animation.oldPair == null
                    || !isNetworkObserved(client, animation.oldPair)
                    || !isNetworkVisible(animation.network, mainFrustum)) continue;
            if (surfaceFormat != null) {
                ArrayList<SurfaceGeometry> geometry = new ArrayList<>();
                for (Map.Entry<SurfaceKey, GlassAnimationTimeline> entry :
                        animation.oldSurfaces.entrySet()) {
                    SurfaceKey key = entry.getKey();
                    GlassProjectionAnimation.Frame frame =
                            animation.outgoingFrame(entry.getValue(), now);
                    if (!frame.visible()
                            || !GlassProjectionMesh.shouldRenderFace(
                                    client.level, key.pos(), key.face())) continue;
                    geometry.add(new SurfaceGeometry(key.pos(), key.face(), frame));
                }
                if (!geometry.isEmpty()) {
                    GlassStagedVertexBuffer.Draw draw =
                            compositor.appendDraw(GlassProjectionCompositor.SURFACE_PIPELINE);
                    if (draw == null) continue;
                    VertexConsumer vertices = compositor.vertices(draw);
                    for (SurfaceGeometry surface : geometry) {
                        GlassProjectionMesh.emitSurfaceQuad(
                                matrix, vertices, surface.pos(), surface.face(), surface.frame());
                    }
                    pendingTextures.add(
                            new PendingDraw(
                                    GlassProjectionCompositor.SURFACE_PIPELINE,
                                    channel.target,
                                    draw));
                }
            }
            WirelessTransition wireless = animation.oldWireless;
            if (wireless != null) {
                GlassStagedVertexBuffer.Draw draw =
                        appendWirelessDraw(
                                matrix,
                                wireless.projectorPos(),
                                wireless.facing(),
                                wireless.positions(),
                                wireless,
                                animation.whiteMaskHandoff,
                                now);
                if (draw != null)
                    pendingTextures.add(
                            new PendingDraw(
                                    GlassProjectionCompositor.WIRELESS_PIPELINE,
                                    channel.target,
                                    draw));
            }
        }
    }

    private GlassStagedVertexBuffer.Draw appendSurfaceDraw(
            Minecraft client, Matrix4f matrix, GlobalPos network, double now) {
        List<View> surfaces =
                observedViews.values().stream()
                        .filter(view -> !view.terminal() && view.wirelessPositions().isEmpty())
                        .filter(View::active)
                        .filter(view -> view.network().equals(network))
                        .sorted(Comparator.comparingLong(view -> view.pos().asLong()))
                        .toList();
        ArrayList<SurfaceGeometry> geometry = new ArrayList<>();
        for (View surface : surfaces) {
            for (Direction face : surface.activeFaces()) {
                if (GlassProjectionMesh.shouldRenderFace(client.level, surface.pos(), face)) {
                    SurfaceTransition transition =
                            surfaceTransitions.get(new SurfaceKey(network, surface.pos(), face));
                    GlassProjectionAnimation.Frame frame =
                            transition == null
                                    ? GlassProjectionAnimation.stable()
                                    : transition.frame(now);
                    if (frame.visible())
                        geometry.add(new SurfaceGeometry(surface.pos(), face, frame));
                }
            }
        }
        if (geometry.isEmpty()) return null;
        VertexFormat format = GlassProjectionCompositor.SURFACE_PIPELINE.getVertexFormat();
        if (format == null) return null;
        GlassStagedVertexBuffer.Draw draw =
                compositor.appendDraw(GlassProjectionCompositor.SURFACE_PIPELINE);
        if (draw == null) return null;
        VertexConsumer vertices = compositor.vertices(draw);
        for (SurfaceGeometry surface : geometry) {
            GlassProjectionMesh.emitSurfaceQuad(
                    matrix, vertices, surface.pos(), surface.face(), surface.frame());
        }
        return draw;
    }

    private void appendClosingSurfaces(
            Minecraft client, Matrix4f matrix, double now, List<PendingDraw> pendingTextures) {
        Map<GlobalPos, List<SurfaceGeometry>> geometryByNetwork = new LinkedHashMap<>();
        for (Map.Entry<SurfaceKey, SurfaceTransition> entry : surfaceTransitions.entrySet()) {
            SurfaceKey key = entry.getKey();
            SurfaceTransition transition = entry.getValue();
            if (transition.finished(now)
                    || isSurfaceCurrentlyActive(key)
                    || !GlassProjectionMesh.shouldRenderFace(client.level, key.pos(), key.face()))
                continue;
            GlassChannelCaptureContext channel =
                    channelContexts.get(networkKey(transition.network()));
            if (channel == null || channel.target == null) continue;
            GlassProjectionAnimation.Frame frame = transition.frame(now);
            if (!frame.visible()) continue;
            geometryByNetwork
                    .computeIfAbsent(transition.network(), ignored -> new ArrayList<>())
                    .add(new SurfaceGeometry(key.pos(), key.face(), frame));
        }
        VertexFormat format = GlassProjectionCompositor.SURFACE_PIPELINE.getVertexFormat();
        if (format == null) return;
        geometryByNetwork.forEach(
                (network, geometry) -> {
                    GlassChannelCaptureContext channel = channelContexts.get(networkKey(network));
                    if (channel != null && channel.target != null) {
                        GlassStagedVertexBuffer.Draw draw =
                                compositor.appendDraw(GlassProjectionCompositor.SURFACE_PIPELINE);
                        if (draw == null) return;
                        VertexConsumer vertices = compositor.vertices(draw);
                        for (SurfaceGeometry surface : geometry) {
                            GlassProjectionMesh.emitSurfaceQuad(
                                    matrix,
                                    vertices,
                                    surface.pos(),
                                    surface.face(),
                                    surface.frame());
                        }
                        pendingTextures.add(
                                new PendingDraw(
                                        GlassProjectionCompositor.SURFACE_PIPELINE,
                                        channel.target,
                                        draw));
                    }
                });
    }

    private GlassStagedVertexBuffer.Draw appendWirelessDraw(
            Matrix4f matrix, View projector, double now) {
        WirelessTransition transition = wirelessTransitions.get(projector.network());
        return appendWirelessDraw(
                matrix,
                projector.pos(),
                projector.facing(),
                projector.wirelessPositions(),
                transition == null ? null : transition,
                false,
                now);
    }

    private void appendClosingWireless(
            Matrix4f matrix, double now, List<PendingDraw> pendingTextures) {
        for (WirelessTransition transition : wirelessTransitions.values()) {
            if (transition.finished(now) || isWirelessCurrentlyActive(transition.network()))
                continue;
            GlassChannelCaptureContext channel =
                    channelContexts.get(networkKey(transition.network()));
            if (channel == null || channel.target == null) continue;
            GlassStagedVertexBuffer.Draw draw =
                    appendWirelessDraw(
                            matrix,
                            transition.projectorPos(),
                            transition.facing(),
                            transition.positions(),
                            transition,
                            false,
                            now);
            if (draw != null)
                pendingTextures.add(
                        new PendingDraw(
                                GlassProjectionCompositor.WIRELESS_PIPELINE, channel.target, draw));
        }
    }

    private GlassStagedVertexBuffer.Draw appendWirelessDraw(
            Matrix4f matrix,
            BlockPos projectorPos,
            Direction facing,
            List<BlockPos> positions,
            WirelessTransition transition,
            boolean outgoingWhiteMaskHandoff,
            double now) {
        int triangleCount = positions.size() - 1;
        if (triangleCount <= 0) return null;
        VertexFormat format = GlassProjectionCompositor.WIRELESS_PIPELINE.getVertexFormat();
        if (format == null) return null;
        ArrayList<Vec3> points = new ArrayList<>(positions.size() + 1);
        Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        points.add(Vec3.atCenterOf(projectorPos).add(normal.scale(GlassSurfaceGeometry.HALF_SIZE)));
        for (BlockPos pos : positions)
            points.add(GlassProjectionMesh.wirelessVertex(projectorPos, facing, pos));

        Vec3 pivot = points.get(0);
        GlassStagedVertexBuffer.Draw draw =
                compositor.appendDraw(GlassProjectionCompositor.WIRELESS_PIPELINE);
        if (draw == null) return null;
        VertexConsumer vertices = compositor.vertices(draw);
        for (int index = 0; index < triangleCount; index++) {
            GlassProjectionAnimation.Frame frame =
                    transition == null
                            ? GlassProjectionAnimation.stable()
                            : outgoingWhiteMaskHandoff
                                    ? transition.outgoingChannelFrame(now, index)
                                    : transition.frame(now, index);
            if (!frame.visible()) continue;
            GlassProjectionMesh.emitGrowingTriangle(
                    matrix,
                    vertices,
                    pivot,
                    points.get(index),
                    points.get(index + 1),
                    points.get(index + 2),
                    frame);
        }
        return draw;
    }

    private boolean isSurfaceCurrentlyActive(SurfaceKey key) {
        return observedViews.values().stream()
                .anyMatch(
                        view ->
                                view.network().equals(key.network())
                                        && view.pos().equals(key.pos())
                                        && !view.terminal()
                                        && view.active()
                                        && view.wirelessPositions().isEmpty()
                                        && view.activeFaces().contains(key.face()));
    }

    private boolean isWirelessCurrentlyActive(GlobalPos network) {
        return observedViews.values().stream()
                .anyMatch(
                        view ->
                                view.network().equals(network)
                                        && view.projector()
                                        && view.active()
                                        && view.wirelessPositions().size() >= 2);
    }

    private boolean isNetworkObserved(Minecraft client, Pair pair) {
        if (client.player == null) return false;
        double maximum = GlassClientState.snapshot().settings().observationDistance();
        double maximumSquared = maximum * maximum;
        return networkPositions(pair.projector().network()).stream()
                .anyMatch(
                        pos -> client.player.distanceToSqr(Vec3.atCenterOf(pos)) <= maximumSquared);
    }

    /** 与同一投影仪相连的所有面都在视锥外时，才剔除整个网络。 */
    private boolean isNetworkVisible(GlobalPos network, Frustum frustum) {
        if (frustum == null) return true;
        return networkPositions(network).stream().anyMatch(pos -> frustum.isVisible(new AABB(pos)));
    }

    private List<BlockPos> networkPositions(GlobalPos network) {
        ArrayList<BlockPos> active = new ArrayList<>();
        for (View view : observedViews.values()) {
            if (!view.active() || !view.network().equals(network) || view.terminal()) continue;
            active.add(view.pos());
            active.addAll(view.wirelessPositions());
        }
        double now = currentGameTime();
        List<BlockPos> closingSurfaces =
                surfaceTransitions.entrySet().stream()
                        .filter(entry -> entry.getKey().network().equals(network))
                        .filter(entry -> !entry.getValue().finished(now))
                        .map(entry -> entry.getKey().pos())
                        .toList();
        ArrayList<BlockPos> closingWireless = new ArrayList<>();
        wirelessTransitions.values().stream()
                .filter(transition -> transition.network().equals(network))
                .filter(transition -> !transition.finished(now))
                .forEach(
                        transition -> {
                            closingWireless.add(transition.projectorPos());
                            closingWireless.addAll(transition.positions());
                        });
        return GlassLiveCapturePositions.combine(active, closingSurfaces, closingWireless);
    }

    private List<Pair> findPairs(Minecraft client) {
        if (client.level == null) return List.of();
        Map<String, View> terminals = new LinkedHashMap<>();
        observedViews.values().stream()
                .filter(View::terminal)
                .filter(view -> !view.channel().isEmpty())
                .sorted(Comparator.comparingLong(view -> view.pos().asLong()))
                .forEach(view -> terminals.putIfAbsent(networkKey(view), view));
        Set<String> activeNetworks = new java.util.LinkedHashSet<>();
        observedViews.values().stream()
                .filter(view -> !view.terminal() && view.active() && !view.channel().isEmpty())
                .sorted(
                        Comparator.comparing((View view) -> networkKey(view))
                                .thenComparingLong(view -> view.pos().asLong()))
                .forEach(view -> activeNetworks.add(networkKey(view)));
        Map<String, View> currentProjectors = new LinkedHashMap<>();
        observedViews.values().stream()
                .filter(View::projector)
                .filter(view -> view.active() && !view.channel().isEmpty())
                .sorted(Comparator.comparingLong(view -> view.pos().asLong()))
                .forEach(
                        projector ->
                                currentProjectors.putIfAbsent(networkKey(projector), projector));
        establishedChannels.removeIf(key -> !activeNetworks.contains(key));

        Map<String, Pair> pairs = new LinkedHashMap<>();
        activeNetworks.forEach(
                key -> {
                    View terminal = terminals.get(key);
                    View projector = currentProjectors.get(key);
                    if (establishedChannels.contains(key)) {
                        if (terminal == null) terminal = lastTerminals.get(key);
                        if (projector == null) projector = lastProjectors.get(key);
                    }
                    if (terminal != null && projector != null) {
                        pairs.put(key, new Pair(terminal, projector));
                        establishedChannels.add(key);
                    }
                });
        return List.copyOf(pairs.values());
    }

    private void retainActiveChannels(List<Pair> pairs) {
        Set<String> active =
                pairs.stream()
                        .map(pair -> networkKey(pair.projector()))
                        .collect(java.util.stream.Collectors.toSet());
        surfaceTransitions.values().stream()
                .filter(transition -> !transition.finished(currentGameTime()))
                .map(transition -> networkKey(transition.network()))
                .forEach(active::add);
        wirelessTransitions.values().stream()
                .filter(transition -> !transition.finished(currentGameTime()))
                .map(transition -> networkKey(transition.network()))
                .forEach(active::add);
        channelContexts
                .entrySet()
                .removeIf(
                        entry -> {
                            if (active.contains(entry.getKey())) return false;
                            entry.getValue().close();
                            return true;
                        });
        ambienceSounds
                .entrySet()
                .removeIf(
                        entry -> {
                            if (active.contains(entry.getKey())) return false;
                            entry.getValue().stopNow();
                            return true;
                        });
    }

    private void updateAmbience(Minecraft client, List<Pair> pairs) {
        if (client.player == null) return;
        for (Pair pair : pairs) {
            String key = networkKey(pair.projector());
            Vec3 nearest;
            if (!pair.projector().wirelessPositions().isEmpty()) {
                ArrayList<BlockPos> positions =
                        new ArrayList<>(pair.projector().wirelessPositions().size() + 1);
                positions.add(pair.projector().pos());
                positions.addAll(pair.projector().wirelessPositions());
                nearest =
                        positions.stream()
                                .map(Vec3::atCenterOf)
                                .min(Comparator.comparingDouble(client.player::distanceToSqr))
                                .orElse(Vec3.atCenterOf(pair.projector().pos()));
            } else {
                nearest =
                        observedViews.values().stream()
                                .filter(
                                        view ->
                                                !view.terminal()
                                                        && view.active()
                                                        && view.network()
                                                                .equals(pair.projector().network()))
                                .map(view -> Vec3.atCenterOf(view.pos()))
                                .min(Comparator.comparingDouble(client.player::distanceToSqr))
                                .orElse(Vec3.atCenterOf(pair.projector().pos()));
            }
            GlassAmbienceSound sound = ambienceSounds.get(key);
            if (sound == null || sound.isStopped()) {
                sound = new GlassAmbienceSound(nearest);
                ambienceSounds.put(key, sound);
                client.getSoundManager().play(sound);
            } else {
                sound.moveTo(nearest);
            }
        }
    }

    private static String networkKey(View view) {
        return networkKey(view.network());
    }

    private static String networkKey(GlobalPos network) {
        return network.toString();
    }

    private void switchLevel(ClientLevel level) {
        if (observedLevel != level) {
            reset();
            observedLevel = level;
        }
    }

    private void markChunkLoaded(
            net.minecraft.client.renderer.LevelRenderer renderer,
            net.minecraft.world.level.ChunkPos pos) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            for (int y = level.getMinSection(); y < level.getMaxSection(); y++) {
                renderer.setSectionDirtyWithNeighbors(pos.x, y, pos.z);
            }
        }
    }

    @Override
    public void chunkLoaded(net.minecraft.world.level.ChunkPos pos) {
        channelContexts
                .values()
                .forEach(
                        channel -> {
                            if (channel.renderer != null) markChunkLoaded(channel.renderer, pos);
                        });
        channelSwitchAnimations
                .values()
                .forEach(
                        animation -> {
                            if (animation.oldContext != null
                                    && animation.oldContext.renderer != null)
                                markChunkLoaded(animation.oldContext.renderer, pos);
                        });
    }

    @Override
    public void reset() {
        observedLevel = null;
        observedViews.clear();
        lastProjectors.clear();
        lastTerminals.clear();
        establishedChannels.clear();
        channelContexts.values().forEach(GlassChannelCaptureContext::close);
        channelContexts.clear();
        ambienceSounds.values().forEach(GlassAmbienceSound::stopNow);
        ambienceSounds.clear();
        surfaceTransitions.clear();
        wirelessTransitions.clear();
        exitPlaneCullingOverrides.clear();
        channelSwitchAnimations.values().forEach(ChannelSwitchAnimation::close);
        channelSwitchAnimations.clear();
    }

    @Override
    public boolean isChannelExtraction() {
        return channelSnapshotIdentity.current();
    }

    @Override
    public void blockChanged(BlockPos pos, int updateFlags) {
        forEachChannelContext(
                channel -> {
                    if (channel.renderer != null)
                        channel.renderer.setBlocksDirty(
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                pos.getX(),
                                pos.getY(),
                                pos.getZ());
                });
    }

    @Override
    public void blockStateChanged(BlockPos pos, BlockState oldState, BlockState newState) {
        forEachChannelContext(
                channel -> {
                    if (channel.renderer != null)
                        channel.renderer.setBlockDirty(pos, oldState, newState);
                });
    }

    @Override
    public void setSectionDirty(int sectionX, int sectionY, int sectionZ) {
        forEachChannelContext(
                channel -> {
                    if (channel.renderer != null)
                        channel.renderer.setSectionDirty(sectionX, sectionY, sectionZ);
                });
    }

    @Override
    public void setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ) {
        forEachChannelContext(
                channel -> {
                    if (channel.renderer != null)
                        channel.renderer.setSectionDirtyWithNeighbors(sectionX, sectionY, sectionZ);
                });
    }

    @Override
    public void setSectionRangeDirty(
            int minSectionX,
            int minSectionY,
            int minSectionZ,
            int maxSectionX,
            int maxSectionY,
            int maxSectionZ) {
        forEachChannelContext(
                channel -> {
                    if (channel.renderer != null) {
                        for (int x = minSectionX; x <= maxSectionX; x++)
                            for (int y = minSectionY; y <= maxSectionY; y++)
                                for (int z = minSectionZ; z <= maxSectionZ; z++)
                                    channel.renderer.setSectionDirty(x, y, z);
                    }
                });
    }

    private void forEachChannelContext(
            java.util.function.Consumer<GlassChannelCaptureContext> action) {
        channelContexts.values().forEach(action);
        channelSwitchAnimations.values().stream()
                .map(animation -> animation.oldContext)
                .filter(java.util.Objects::nonNull)
                .forEach(action);
    }

    private static double currentGameTime() {
        Minecraft client = Minecraft.getInstance();
        return client.level == null ? 0.0 : client.level.getGameTime() + client.getFrameTime();
    }

    @Override
    public void close() {
        reset();
        compositor.close();
    }

    private record View(
            GlobalPos network,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            BlockPos pos,
            Direction facing,
            String channel,
            boolean terminal,
            boolean projector,
            boolean active,
            Map<Direction, Integer> propagationDistances,
            EnumSet<Direction> activeFaces,
            List<BlockPos> wirelessPositions) {
        GlobalPos globalPos() {
            return GlobalPos.of(dimension, pos);
        }

        ViewKey key() {
            return new ViewKey(network, globalPos());
        }

        int propagationDistance(Direction face) {
            return propagationDistances.getOrDefault(face, 0);
        }
    }

    private record ViewKey(GlobalPos network, GlobalPos pos) {}

    private record Pair(View terminal, View projector) {}

    private record PendingDraw(
            GlassProjectionCompositor.Pipeline pipeline,
            TextureTarget target,
            GlassStagedVertexBuffer.Draw draw) {}

    private record SurfaceGeometry(
            BlockPos pos, Direction face, GlassProjectionAnimation.Frame frame) {}

    private record SurfaceKey(GlobalPos network, BlockPos pos, Direction face) {}

    private record SurfaceTransition(
            GlobalPos network, String channel, GlassAnimationTimeline timeline) {
        GlassProjectionAnimation.Frame frame(double now) {
            return animationFrame(timeline, now);
        }

        boolean finished(double now) {
            return timeline.finished(now);
        }
    }

    private record WirelessTransition(
            GlobalPos network,
            BlockPos projectorPos,
            Direction facing,
            String channel,
            List<BlockPos> positions,
            List<GlassAnimationTimeline> timelines) {
        GlassProjectionAnimation.Frame frame(double now, int triangleIndex) {
            return triangleIndex < 0 || triangleIndex >= timelines.size()
                    ? new GlassProjectionAnimation.Frame(0.0, 0.0F, 0.0F)
                    : animationFrame(timelines.get(triangleIndex), now);
        }

        GlassProjectionAnimation.Frame outgoingChannelFrame(double now, int triangleIndex) {
            return triangleIndex < 0 || triangleIndex >= timelines.size()
                    ? new GlassProjectionAnimation.Frame(0.0, 0.0F, 0.0F)
                    : GlassProjectionAnimation.outgoingChannelFrame(
                            true, timelines.get(triangleIndex), now);
        }

        boolean finished(double now) {
            return timelines.stream().allMatch(timeline -> timeline.finished(now));
        }
    }

    private final class ChannelSwitchAnimation implements AutoCloseable {
        private final GlobalPos network;
        private GlassChannelCaptureContext oldContext;
        private final Pair oldPair;
        private final Map<SurfaceKey, GlassAnimationTimeline> oldSurfaces;
        private final WirelessTransition oldWireless;
        private final boolean oldExitPlaneCulling;
        private final boolean whiteMaskHandoff;
        private final double oldEndTime;
        private final double endTime;

        private ChannelSwitchAnimation(
                GlobalPos network,
                GlassChannelCaptureContext oldContext,
                Pair oldPair,
                Map<SurfaceKey, GlassAnimationTimeline> oldSurfaces,
                WirelessTransition oldWireless,
                boolean oldExitPlaneCulling,
                boolean whiteMaskHandoff,
                double oldEndTime,
                double endTime) {
            this.network = network;
            this.oldContext = oldContext;
            this.oldPair = oldPair;
            this.oldSurfaces = oldSurfaces;
            this.oldWireless = oldWireless;
            this.oldExitPlaneCulling = oldExitPlaneCulling;
            this.whiteMaskHandoff = whiteMaskHandoff;
            this.oldEndTime = oldEndTime;
            this.endTime = endTime;
        }

        private boolean finished(double now) {
            return now >= endTime;
        }

        private GlassProjectionAnimation.Frame outgoingFrame(
                GlassAnimationTimeline timeline, double now) {
            return GlassProjectionAnimation.outgoingChannelFrame(whiteMaskHandoff, timeline, now);
        }

        private void releaseOldIfFinished(double now) {
            if (now < oldEndTime || oldContext == null) return;
            oldContext.close();
            oldContext = null;
        }

        @Override
        public void close() {
            if (oldContext == null) return;
            oldContext.close();
            oldContext = null;
        }
    }
}
