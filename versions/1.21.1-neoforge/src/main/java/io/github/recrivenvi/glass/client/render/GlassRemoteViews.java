package io.github.recrivenvi.glass.client.render;

import io.github.recrivenvi.glass.GlassClient;
import io.github.recrivenvi.glass.client.GlassClientState;
import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.content.block.GlassProjectorBlock;
import io.github.recrivenvi.glass.network.GlassRemoteFramePayload;
import io.github.recrivenvi.glass.network.GlassRemotePackets;
import io.github.recrivenvi.glass.network.GlassRemoteWatchPayload;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.network.PacketDistributor;

/** One authorized data world per viewer; drawing targets stay owned by projector contexts. */
public final class GlassRemoteViews {
    private static final Map<UUID, View> VIEWS = new LinkedHashMap<>();
    private static Optional<GlobalPos> requested = Optional.empty();
    private static int heartbeat;

    private GlassRemoteViews() {}

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            clear();
            return;
        }
        if (client.isPaused()) return;
        Optional<GlobalPos> next = observedProjector(client);
        if (!next.equals(requested) || ++heartbeat >= 20) {
            requested = next;
            heartbeat = 0;
            PacketDistributor.sendToServer(new GlassRemoteWatchPayload(next));
        }
        VIEWS.values().forEach(View::tick);
    }

    private static Optional<GlobalPos> observedProjector(Minecraft client) {
        var snapshot = GlassClientState.snapshot();
        double distance = snapshot.settings().observationDistance();
        var frustum = client.levelRenderer.getFrustum();
        return snapshot.projectors().stream()
                .filter(p -> p.pos().dimension().equals(client.level.dimension()))
                .filter(p -> p.channelId().isPresent())
                .filter(
                        p ->
                                snapshot.terminals().stream()
                                        .anyMatch(
                                                t ->
                                                        t.channel().id().equals(p.channelId().get())
                                                                && (t.channel().publicChannel()
                                                                        || t.channel()
                                                                                .owner()
                                                                                .filter(
                                                                                        client
                                                                                                        .player
                                                                                                        .getUUID()
                                                                                                ::equals)
                                                                                .isPresent())))
                .filter(
                        p ->
                                snapshot.observationPositions(p)
                                        .filter(
                                                pos ->
                                                        pos.dimension()
                                                                .equals(client.level.dimension()))
                                        .anyMatch(
                                                pos ->
                                                        client.player.distanceToSqr(
                                                                                GlassSableClient
                                                                                        .center(
                                                                                                client.level,
                                                                                                pos
                                                                                                        .pos()))
                                                                        <= distance * distance
                                                                && (frustum == null
                                                                        || frustum.isVisible(
                                                                                GlassSableClient
                                                                                        .bounds(
                                                                                                client.level,
                                                                                                pos
                                                                                                        .pos())))))
                .map(GlassStateSnapshot.Projector::pos)
                .filter(
                        p ->
                                client.level
                                                .getChunkSource()
                                                .getChunk(
                                                        p.pos().getX() >> 4,
                                                        p.pos().getZ() >> 4,
                                                        ChunkStatus.FULL,
                                                        false)
                                        != null)
                .filter(
                        p -> {
                            var state = client.level.getBlockState(p.pos());
                            return state.is(GlassContent.PROJECTOR.get())
                                    && state.getValue(GlassProjectorBlock.POWERED);
                        })
                .min(
                        Comparator.comparingDouble(
                                p ->
                                        client.player.distanceToSqr(
                                                GlassSableClient.center(client.level, p.pos()))));
    }

    public static void receive(GlassRemoteFramePayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        String operation = payload.header().getString("operation");
        if (operation.equals("begin")) {
            clearViews();
            GlassClient.VIEW_RENDERER.withChannelScope(
                    () ->
                            VIEWS.put(
                                    payload.generation(),
                                    new View(payload.generation(), payload.header())));
        } else if (operation.equals("end")) {
            View old = VIEWS.remove(payload.generation());
            if (old != null) old.close();
            return;
        } else if (operation.equals("denied")) {
            clearViews();
            return;
        }
        View view = VIEWS.get(payload.generation());
        if (view == null) return;
        try (var ignored = GlassCaptureWorld.open(view.context())) {
            view.synchronizeTime(payload.header());
            for (byte[] bytes : payload.packets()) {
                var packet =
                        GlassRemotePackets.apply(
                                view.level.registryAccess(), bytes, client.getConnection());
                if (packet instanceof ClientboundLevelChunkWithLightPacket chunk)
                    view.received.add(ChunkPos.asLong(chunk.getX(), chunk.getZ()));
            }
        }
    }

    static View find(ResourceKey<Level> dimension, BlockPos terminal) {
        return VIEWS.values().stream()
                .filter(
                        v ->
                                v.terminal.dimension().equals(dimension)
                                        && v.terminal.pos().equals(terminal))
                .findFirst()
                .orElse(null);
    }

    public static int activeViews() {
        return VIEWS.size();
    }

    public static boolean ready(ResourceKey<Level> dimension, BlockPos terminal) {
        View view = find(dimension, terminal);
        return view != null && view.ready();
    }

    public static void clear() {
        clearViews();
        requested = Optional.empty();
        heartbeat = 0;
    }

    public static void releaseAndClear() {
        var client = Minecraft.getInstance();
        if (client.level != null && client.getConnection() != null && requested.isPresent())
            PacketDistributor.sendToServer(new GlassRemoteWatchPayload(Optional.empty()));
        clear();
    }

    private static void clearViews() {
        VIEWS.values().forEach(View::close);
        VIEWS.clear();
    }

    static final class View implements AutoCloseable {
        final UUID generation;
        final GlobalPos terminal;
        final ClientLevel level;
        final LevelRenderer dataRenderer;
        final ParticleEngine particles;
        final LightTexture lightTexture;
        final Set<Long> received = new HashSet<>();
        int expectedChunks;
        private Set<Long> sourceChunks = Set.of(), boundaryChunks = Set.of();
        private boolean initialized;
        private boolean closed;
        private int sableTick = Integer.MIN_VALUE;

        View(UUID generation, CompoundTag header) {
            this.generation = generation;
            Minecraft client = Minecraft.getInstance();
            ResourceKey<Level> dimension =
                    ResourceKey.create(
                            Registries.DIMENSION,
                            ResourceLocation.parse(header.getString("dimension")));
            terminal = GlobalPos.of(dimension, BlockPos.of(header.getLong("terminal")));
            expectedChunks = header.getInt("chunks");
            var type =
                    client.level
                            .registryAccess()
                            .registryOrThrow(Registries.DIMENSION_TYPE)
                            .getHolderOrThrow(
                                    ResourceKey.create(
                                            Registries.DIMENSION_TYPE,
                                            ResourceLocation.parse(
                                                    header.getString("dimensionType"))));
            dataRenderer =
                    new LevelRenderer(
                            client,
                            client.getEntityRenderDispatcher(),
                            client.getBlockEntityRenderDispatcher(),
                            client.renderBuffers());
            level =
                    new ClientLevel(
                            client.getConnection(),
                            new ClientLevel.ClientLevelData(
                                    Difficulty.byId(header.getInt("difficulty")),
                                    false,
                                    header.getBoolean("flat")),
                            dimension,
                            type,
                            header.getInt("radius"),
                            header.getInt("radius"),
                            client::getProfiler,
                            dataRenderer,
                            false,
                            header.getLong("biomeSeed"));
            particles = GlassCaptureWorld.createParticles(level);
            lightTexture = new LightTexture(client.gameRenderer, client);
            try (var ignored = GlassCaptureWorld.open(context())) {
                dataRenderer.setLevel(level);
                synchronizeTime(header);
            }
        }

        GlassCaptureWorld.Context context() {
            return context(dataRenderer);
        }

        GlassCaptureWorld.Context context(LevelRenderer renderer) {
            return new GlassCaptureWorld.Context(level, renderer, particles, lightTexture);
        }

        boolean ready() {
            initialized |= received.size() >= expectedChunks;
            return !closed && initialized;
        }

        boolean currentSnapshotReceived() {
            return received.size() >= expectedChunks;
        }

        void synchronizeTime(CompoundTag header) {
            if (!header.contains("gameTime")) return;
            var next =
                    java.util.Arrays.stream(header.getLongArray("sourceChunks"))
                            .boxed()
                            .collect(java.util.stream.Collectors.toSet());
            if (!next.equals(sourceChunks)) {
                for (long key : received)
                    if (!next.contains(key)) level.getChunkSource().drop(new ChunkPos(key));
                received.retainAll(next);
                sourceChunks = Set.copyOf(next);
                expectedChunks = header.getInt("chunks");
                boundaryChunks = GlassSodiumBoundary.update(level, header, boundaryChunks);
            }
            if (header.getInt("serverTick") != sableTick) {
                sableTick = header.getInt("serverTick");
                GlassSableClient.receive(level, header);
            }
            level.setGameTime(header.getLong("gameTime"));
            level.setDayTime(header.getLong("dayTime"));
            level.setRainLevel(header.getFloat("rain"));
            level.setThunderLevel(header.getFloat("thunder"));
        }

        void tick() {
            try (var ignored = GlassCaptureWorld.open(context())) {
                level.pollLightUpdates();
                level.tick(() -> true);
                level.tickEntities();
                GlassSableClient.tick(level);
                particles.tick();
                lightTexture.tick();
                GlassBlockInteriors.tick();
            }
        }

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            try (var ignored = GlassCaptureWorld.open(context())) {
                GlassSableClient.close(level);
                dataRenderer.setLevel(null);
                dataRenderer.close();
                particles.setLevel(null);
                lightTexture.close();
            }
        }
    }
}
