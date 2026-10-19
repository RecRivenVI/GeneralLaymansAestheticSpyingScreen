package io.github.recrivenvi.glass.state;

import io.github.recrivenvi.glass.config.GlassConfig;
import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.content.block.GlassChannelBlock;
import io.github.recrivenvi.glass.content.block.GlassProjectorBlock;
import io.github.recrivenvi.glass.core.GlassTopologySolver;
import io.github.recrivenvi.glass.model.GlassTopologyUpdateQueue;
import io.github.recrivenvi.glass.network.GlassStatePayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** 世界级 GLASS 状态与拓扑重建的唯一服务端入口。 */
public final class GlassServerState {
    private static final Map<MinecraftServer, RuntimeState> RUNTIMES = new WeakHashMap<>();

    private GlassServerState() {}

    public static GlassSavedState saved(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(GlassSavedState.TYPE);
    }

    public static GlassStateSnapshot snapshot(MinecraftServer server) {
        return runtime(server).snapshot;
    }

    public static void initialize(MinecraftServer server) {
        rebuild(server);
    }

    public static void stop(MinecraftServer server) {
        RUNTIMES.remove(server);
    }

    public static void chunkLoaded(ServerLevel level, ChunkPos chunkPos) {
        chunkChanged(level, chunkPos);
    }

    public static void chunkUnloaded(ServerLevel level, ChunkPos chunkPos) {
        chunkChanged(level, chunkPos);
    }

    private static void chunkChanged(ServerLevel level, ChunkPos chunkPos) {
        MinecraftServer server = level.getServer();
        GlassSavedState saved = saved(server);
        if (shouldRebuildForChangedChunk(
                saved.terminals().values(),
                saved.projectors().values(),
                level.dimension(),
                chunkPos,
                GlassConfig.get().propagationLimit())) {
            topologyChanged(level);
        }
    }

    public static void tick(MinecraftServer server) {
        RuntimeState runtime = runtime(server);
        if (!runtime.updateQueue.consume()) return;
        rebuild(server);
    }

    static boolean shouldRebuildForChangedChunk(
            java.util.Collection<GlassStateSnapshot.Terminal> terminals,
            java.util.Collection<GlassStateSnapshot.Projector> projectors,
            net.minecraft.resources.ResourceKey<Level> dimension,
            ChunkPos chunkPos,
            int propagationLimit) {
        if (terminals.stream().anyMatch(terminal -> inChunk(terminal.pos(), dimension, chunkPos)))
            return true;
        return projectors.stream()
                .anyMatch(
                        projector ->
                                inChunk(projector.pos(), dimension, chunkPos)
                                        || projector.wirelessPositions().stream()
                                                .anyMatch(pos -> inChunk(pos, dimension, chunkPos))
                                        || withinPropagationRange(
                                                projector.pos(),
                                                dimension,
                                                chunkPos,
                                                propagationLimit));
    }

    private static boolean withinPropagationRange(
            GlobalPos origin,
            net.minecraft.resources.ResourceKey<Level> dimension,
            ChunkPos chunkPos,
            int propagationLimit) {
        if (!origin.dimension().equals(dimension)) return false;
        int minimumX = chunkPos.x() << 4;
        int minimumZ = chunkPos.z() << 4;
        int horizontalDistance =
                axisDistance(origin.pos().getX(), minimumX, minimumX + 15)
                        + axisDistance(origin.pos().getZ(), minimumZ, minimumZ + 15);
        return horizontalDistance <= propagationLimit;
    }

    private static int axisDistance(int coordinate, int minimum, int maximum) {
        if (coordinate < minimum) return minimum - coordinate;
        if (coordinate > maximum) return coordinate - maximum;
        return 0;
    }

    private static boolean inChunk(
            GlobalPos pos,
            net.minecraft.resources.ResourceKey<Level> dimension,
            ChunkPos chunkPos) {
        return pos.dimension().equals(dimension)
                && SectionPos.blockToSectionCoord(pos.pos().getX()) == chunkPos.x()
                && SectionPos.blockToSectionCoord(pos.pos().getZ()) == chunkPos.z();
    }

    public static void placed(ServerLevel level, BlockPos pos, BlockState state) {
        GlobalPos globalPos = GlobalPos.of(level.dimension(), pos.immutable());
        GlassSavedState saved = saved(level.getServer());
        if (state.is(GlassContent.TERMINAL)) {
            saved.ensureTerminal(globalPos);
        } else if (state.is(GlassContent.PROJECTOR)) {
            saved.ensureProjector(globalPos);
        }
        topologyChanged(level);
    }

    public static void removed(ServerLevel level, BlockPos pos, BlockState oldState) {
        if (!oldState.is(GlassContent.TERMINAL)
                && !oldState.is(GlassContent.PROJECTOR)
                && !GlassContent.isSurface(oldState)) {
            return;
        }
        saved(level.getServer()).remove(GlobalPos.of(level.dimension(), pos.immutable()));
        topologyChanged(level);
    }

    public static void topologyChanged(ServerLevel level) {
        runtime(level.getServer()).updateQueue.mark();
    }

    public static void setTerminal(
            ServerPlayer player, GlobalPos pos, String requestedName, boolean publicChannel) {
        if (!canInteract(player, pos)
                || !isBlock(player.level().getServer(), pos, GlassContent.TERMINAL)) return;
        GlassSavedState state = saved(player.level().getServer());
        GlassStateSnapshot.Terminal previous = state.ensureTerminal(pos);
        if (previous.channel().owner().isPresent()
                && previous.channel().owner().filter(player.getUUID()::equals).isEmpty()) return;
        String name = sanitizeName(requestedName, GlassConfig.get().channelNameLength());
        GlassStateSnapshot.Channel channel =
                new GlassStateSnapshot.Channel(
                        previous.channel().id(),
                        name,
                        Optional.of(player.getUUID()),
                        publicChannel);
        state.putTerminal(new GlassStateSnapshot.Terminal(pos, channel));
        rebuild(player.level().getServer());
    }

    public static void setProjectorChannel(
            ServerPlayer player, GlobalPos projectorPos, UUID channelId) {
        if (!canInteract(player, projectorPos)
                || !isBlock(player.level().getServer(), projectorPos, GlassContent.PROJECTOR))
            return;
        GlassSavedState state = saved(player.level().getServer());
        GlassStateSnapshot.Terminal terminal =
                state.terminals().values().stream()
                        .filter(value -> value.channel().id().equals(channelId))
                        .findFirst()
                        .orElse(null);
        if (terminal == null || !canAccess(terminal.channel(), player.getUUID())) return;
        GlassStateSnapshot.Projector projector = state.ensureProjector(projectorPos);
        state.putProjector(
                new GlassStateSnapshot.Projector(
                        projectorPos, Optional.of(channelId), projector.wirelessPositions()));
        rebuild(player.level().getServer());
    }

    public static void attachWireless(
            ServerPlayer player, GlobalPos wirelessPos, GlobalPos projectorPos) {
        if (!canInteract(player, wirelessPos)
                || !wirelessPos.dimension().equals(projectorPos.dimension())
                || !isBlock(
                        player.level().getServer(), wirelessPos, GlassContent.WIRELESS_EXTENSION)
                || !isBlock(player.level().getServer(), projectorPos, GlassContent.PROJECTOR))
            return;
        GlassSavedState state = saved(player.level().getServer());
        GlassStateSnapshot.Projector projector = state.ensureProjector(projectorPos);
        if (!canAccess(projector, state, player)) return;
        ArrayList<GlobalPos> positions = new ArrayList<>(projector.wirelessPositions());
        if (positions.contains(wirelessPos)
                || positions.size() >= GlassConfig.get().wirelessMaxNodes()) return;
        positions.add(wirelessPos);
        state.putProjector(
                new GlassStateSnapshot.Projector(projectorPos, projector.channelId(), positions));
        rebuild(player.level().getServer());
    }

    public static void setWirelessOrder(
            ServerPlayer player, GlobalPos projectorPos, List<GlobalPos> requested) {
        if (!canInteract(player, projectorPos)
                || !isBlock(player.level().getServer(), projectorPos, GlassContent.PROJECTOR))
            return;
        if (requested.size() > GlassConfig.get().wirelessMaxNodes()
                || new HashSet<>(requested).size() != requested.size()) return;
        GlassSavedState state = saved(player.level().getServer());
        GlassStateSnapshot.Projector projector = state.ensureProjector(projectorPos);
        if (!canAccess(projector, state, player)) return;
        for (GlobalPos pos : requested) {
            if (!pos.dimension().equals(projectorPos.dimension())
                    || !isBlock(player.level().getServer(), pos, GlassContent.WIRELESS_EXTENSION))
                return;
        }
        state.putProjector(
                new GlassStateSnapshot.Projector(projectorPos, projector.channelId(), requested));
        rebuild(player.level().getServer());
    }

    public static void sync(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        sendState(player, rebuildSnapshot(server));
    }

    public static void sendState(ServerPlayer player) {
        sendState(player, snapshot(player.level().getServer()));
    }

    private static void sendState(ServerPlayer player, GlassStateSnapshot snapshot) {
        ServerPlayNetworking.send(player, new GlassStatePayload(snapshot.toTag()));
    }

    public static void rebuild(MinecraftServer server) {
        GlassStateSnapshot snapshot = rebuildSnapshot(server);
        for (ServerPlayer player : PlayerLookup.all(server)) {
            ServerPlayNetworking.send(player, new GlassStatePayload(snapshot.toTag()));
        }
    }

    /** 玩家加入时必须从此刻已经加载的世界重新生成快照。服务器启动阶段附近区块尚未加载， 直接复用那时的运行时快照会把已保存的投影网络错误地同步为空。 */
    private static GlassStateSnapshot rebuildSnapshot(MinecraftServer server) {
        GlassSavedState saved = saved(server);
        pruneKnownBlocks(server, saved);
        ArrayList<GlassStateSnapshot.Network> networks = new ArrayList<>();
        long revision = server.getTickCount();
        for (GlassStateSnapshot.Projector projector : saved.projectors().values()) {
            ServerLevel level = server.getLevel(projector.pos().dimension());
            if (level == null || !level.hasChunkAt(projector.pos().pos())) continue;
            BlockState blockState = level.getBlockState(projector.pos().pos());
            if (!blockState.is(GlassContent.PROJECTOR) || projector.channelId().isEmpty()) continue;
            GlassStateSnapshot.Terminal terminal =
                    saved.terminals().values().stream()
                            .filter(
                                    value ->
                                            value.channel()
                                                    .id()
                                                    .equals(projector.channelId().get()))
                            .filter(value -> !value.channel().name().isEmpty())
                            .findFirst()
                            .orElse(null);
            if (terminal == null) continue;
            ServerLevel terminalLevel = server.getLevel(terminal.pos().dimension());
            if (terminalLevel == null || !terminalLevel.hasChunkAt(terminal.pos().pos())) continue;
            BlockState terminalState = terminalLevel.getBlockState(terminal.pos().pos());
            Direction terminalFacing =
                    terminalState.is(GlassContent.TERMINAL)
                            ? terminalState.getValue(GlassChannelBlock.FACING)
                            : Direction.NORTH;
            Direction facing = blockState.getValue(GlassProjectorBlock.FACING);
            boolean active = blockState.getValue(GlassProjectorBlock.POWERED);
            List<GlobalPos> wireless = validWireless(server, projector.wirelessPositions());
            boolean wirelessMode = wireless.size() >= GlassConfig.get().wirelessMinNodes();
            List<GlassStateSnapshot.Surface> surfaces =
                    wirelessMode
                            ? List.of()
                            : GlassTopologySolver.solve(
                                            projector.pos().pos(),
                                            facing,
                                            GlassConfig.get().propagationLimit(),
                                            candidate ->
                                                    GlassContent.isSurface(
                                                            level.getBlockState(candidate)),
                                            (candidate, face) ->
                                                    isPropagationFaceExposed(
                                                            level.getBlockState(candidate),
                                                            level.getBlockState(
                                                                    candidate.relative(face)),
                                                            face))
                                    .stream()
                                    .map(
                                            value ->
                                                    new GlassStateSnapshot.Surface(
                                                            GlobalPos.of(
                                                                    projector.pos().dimension(),
                                                                    value.pos()),
                                                            value.face(),
                                                            value.distance()))
                                    .toList();
            networks.add(
                    new GlassStateSnapshot.Network(
                            projector.pos(),
                            projector.channelId().get(),
                            terminal.pos(),
                            terminalFacing,
                            facing,
                            active,
                            revision,
                            surfaces,
                            wirelessMode ? wireless : List.of()));
        }
        networks.sort(Comparator.comparing(value -> value.projector().toString()));

        GlassConfig config = GlassConfig.get();
        GlassStateSnapshot snapshot =
                new GlassStateSnapshot(
                        List.copyOf(saved.terminals().values()),
                        List.copyOf(saved.projectors().values()),
                        networks,
                        new GlassStateSnapshot.Settings(
                                config.propagationDelayTicks(),
                                config.surfaceAnimationTicks(),
                                config.observationDistance(),
                                config.wirelessMinNodes(),
                                config.wirelessMaxNodes(),
                                config.channelNameLength(),
                                config.channelRenderDistance(),
                                config.legacyFadeAnimation()));
        RuntimeState runtime = runtime(server);
        runtime.snapshot = snapshot;
        runtime.updateQueue.clear();
        return snapshot;
    }

    static boolean isPropagationFaceExposed(
            BlockState surfaceState, BlockState neighbourState, Direction face) {
        return isPropagationFaceExposed(
                surfaceState, neighbourState, face, GlassContent::isSurface);
    }

    static boolean isPropagationFaceExposed(
            BlockState surfaceState,
            BlockState neighbourState,
            Direction face,
            Predicate<BlockState> isGlassSurface) {
        if (isGlassSurface.test(neighbourState)) return false;
        return Block.shouldRenderFace(surfaceState, neighbourState, face);
    }

    private static void pruneKnownBlocks(MinecraftServer server, GlassSavedState saved) {
        boolean changed =
                saved.terminals()
                        .entrySet()
                        .removeIf(
                                entry ->
                                        loadedAndNot(
                                                server, entry.getKey(), GlassContent.TERMINAL));
        changed |=
                saved.projectors()
                        .entrySet()
                        .removeIf(
                                entry ->
                                        loadedAndNot(
                                                server, entry.getKey(), GlassContent.PROJECTOR));
        if (changed) saved.setDirty();
    }

    private static boolean loadedAndNot(
            MinecraftServer server, GlobalPos pos, net.minecraft.world.level.block.Block expected) {
        ServerLevel level = server.getLevel(pos.dimension());
        return level != null
                && level.hasChunkAt(pos.pos())
                && !level.getBlockState(pos.pos()).is(expected);
    }

    private static List<GlobalPos> validWireless(MinecraftServer server, List<GlobalPos> values) {
        return values.stream()
                .filter(
                        pos -> {
                            ServerLevel level = server.getLevel(pos.dimension());
                            return level != null
                                    && level.hasChunkAt(pos.pos())
                                    && level.getBlockState(pos.pos())
                                            .is(GlassContent.WIRELESS_EXTENSION);
                        })
                .toList();
    }

    private static boolean canAccess(
            GlassStateSnapshot.Projector projector, GlassSavedState state, ServerPlayer player) {
        return canAccess(projector, state.terminals().values(), player.getUUID());
    }

    static boolean canAccess(
            GlassStateSnapshot.Projector projector,
            java.util.Collection<GlassStateSnapshot.Terminal> terminals,
            UUID playerId) {
        if (projector.channelId().isEmpty()) return true;
        return terminals.stream()
                .filter(value -> value.channel().id().equals(projector.channelId().get()))
                .findFirst()
                .map(value -> canAccess(value.channel(), playerId))
                .orElse(false);
    }

    private static boolean canAccess(GlassStateSnapshot.Channel channel, UUID playerId) {
        return channel.publicChannel() || channel.owner().filter(playerId::equals).isPresent();
    }

    /** 只保护直接交互，和投影可见距离及传播距离无关。 */
    public static boolean canInteract(ServerPlayer player, GlobalPos pos) {
        if (!player.level().dimension().equals(pos.dimension())) return false;
        BlockPos blockPos = pos.pos();
        return player.distanceToSqr(
                        blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5)
                <= 64.0;
    }

    private static boolean isBlock(
            MinecraftServer server, GlobalPos pos, net.minecraft.world.level.block.Block block) {
        ServerLevel level = server.getLevel(pos.dimension());
        return level != null
                && level.hasChunkAt(pos.pos())
                && level.getBlockState(pos.pos()).is(block);
    }

    private static String sanitizeName(String value, int maximumCodePoints) {
        if (value == null) return "";
        StringBuilder result = new StringBuilder();
        value.codePoints()
                .filter(codePoint -> codePoint >= 0x20 && codePoint != 0x7f)
                .limit(maximumCodePoints)
                .forEach(result::appendCodePoint);
        return result.toString().trim();
    }

    private static RuntimeState runtime(MinecraftServer server) {
        return RUNTIMES.computeIfAbsent(server, ignored -> new RuntimeState());
    }

    private static final class RuntimeState {
        private GlassStateSnapshot snapshot =
                new GlassStateSnapshot(
                        List.of(),
                        List.of(),
                        List.of(),
                        new GlassStateSnapshot.Settings(2, 8, 256, 2, 256, 64, 8, false));
        private final GlassTopologyUpdateQueue updateQueue = new GlassTopologyUpdateQueue();
    }
}
