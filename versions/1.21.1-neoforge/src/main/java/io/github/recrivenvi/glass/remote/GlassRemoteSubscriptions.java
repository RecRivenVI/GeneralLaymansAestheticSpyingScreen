package io.github.recrivenvi.glass.remote;

import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.network.GlassRemoteFramePayload;
import io.github.recrivenvi.glass.network.GlassRemotePackets;
import io.github.recrivenvi.glass.state.GlassServerState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Owns authorization, shared-source references, bounded streaming, and nonpersistent native
 * tickets.
 */
public final class GlassRemoteSubscriptions {
    private static final TicketType<UUID> TICKET =
            TicketType.create("glass_remote_view", Comparator.<UUID>naturalOrder());
    private static final Map<MinecraftServer, Runtime> RUNTIMES = new WeakHashMap<>();

    private GlassRemoteSubscriptions() {}

    private static Runtime runtime(MinecraftServer server) {
        return RUNTIMES.computeIfAbsent(server, s -> new Runtime(s, GlassRemoteLimits.load()));
    }

    public static void watch(ServerPlayer player, Optional<GlobalPos> projector) {
        runtime(player.serverLevel().getServer()).watch(player, projector);
    }

    public static void disconnect(ServerPlayer player) {
        var state = RUNTIMES.get(player.serverLevel().getServer());
        if (state != null) state.remove(player.getUUID());
    }

    public static void tick(MinecraftServer server) {
        runtime(server).tick();
    }

    public static void stop(MinecraftServer server) {
        Runtime state = RUNTIMES.remove(server);
        if (state != null) state.close();
    }

    public static int activeSources(MinecraftServer server) {
        return runtime(server).sources.size();
    }

    public static int heldSourceChunks(MinecraftServer server) {
        return runtime(server).references.size();
    }

    public static int streamedSourceChunks(MinecraftServer server) {
        return runtime(server).sources.values().stream()
                .mapToInt(source -> source.chunks.size())
                .sum();
    }

    public static void blockChanged(ServerLevel level, net.minecraft.core.BlockPos pos) {
        Runtime state = RUNTIMES.get(level.getServer());
        if (state == null) return;
        long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        for (Watcher watcher : state.watchers.values())
            if (watcher.source.level == level && watcher.source.chunks.contains(chunk))
                watcher.dirty.add(chunk);
        visualEvent(
                level,
                pos,
                new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(level, pos));
        var entity = level.getBlockEntity(pos);
        if (entity != null && entity.getUpdatePacket() != null)
            visualEvent(level, pos, entity.getUpdatePacket());
    }

    public static void visualEvent(
            ServerLevel level,
            net.minecraft.core.BlockPos pos,
            net.minecraft.network.protocol.Packet<
                            ? super net.minecraft.network.protocol.game.ClientGamePacketListener>
                    packet) {
        Runtime state = RUNTIMES.get(level.getServer());
        if (state == null) return;
        long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        var recipients =
                state.watchers.values().stream()
                        .filter(
                                w ->
                                        w.begun
                                                && w.source.level == level
                                                && w.source.chunks.contains(chunk))
                        .toList();
        if (recipients.isEmpty()) return;
        byte[] bytes = GlassRemotePackets.encode(level.registryAccess(), packet);
        for (Watcher watcher : recipients) {
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(watcher.player);
            if (player != null) state.send(player, watcher, List.of(bytes));
        }
    }

    public static void entityEvent(
            ServerLevel level,
            net.minecraft.world.entity.Entity entity,
            net.minecraft.network.protocol.Packet<?> packet) {
        switch (packet) {
            case net.minecraft.network.protocol.game.ClientboundAnimatePacket p ->
                    visualEvent(level, entity.blockPosition(), p);
            case net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket p ->
                    visualEvent(level, entity.blockPosition(), p);
            case net.minecraft.network.protocol.game.ClientboundEntityEventPacket p ->
                    visualEvent(level, entity.blockPosition(), p);
            case net.minecraft.network.protocol.game.ClientboundDamageEventPacket p ->
                    visualEvent(level, entity.blockPosition(), p);
            case net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket p ->
                    visualEvent(level, entity.blockPosition(), p);
            case net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket p ->
                    visualEvent(level, entity.blockPosition(), p);
            default -> {}
        }
    }

    private static final class Runtime implements AutoCloseable {
        final MinecraftServer server;
        final GlassRemoteLimits limits;
        final Map<GlobalPos, Source> sources = new LinkedHashMap<>();
        final Map<UUID, Watcher> watchers = new LinkedHashMap<>();
        final Map<ChunkKey, Integer> references = new HashMap<>();

        Runtime(MinecraftServer server, GlassRemoteLimits limits) {
            this.server = server;
            this.limits = limits;
        }

        void watch(ServerPlayer player, Optional<GlobalPos> requested) {
            var authorized = requested.flatMap(p -> GlassServerState.remoteTerminal(player, p));
            Watcher previous = watchers.get(player.getUUID());
            if (authorized.isEmpty()) {
                remove(player.getUUID());
                return;
            }
            GlobalPos terminal = authorized.get().pos();
            if (previous != null && previous.source.terminal.equals(terminal)) {
                previous.projector = requested.get();
                previous.lastSeen = server.getTickCount();
                return;
            }
            remove(player.getUUID());
            ServerLevel level = server.getLevel(terminal.dimension());
            if (level == null) return;
            if (GlassSableServer.enabled() && !GlassSablePersistence.ensureLoaded(level, terminal))
                return;
            Source source = sources.get(terminal);
            if (source == null) {
                source = new Source(terminal, level, limits);
                int additional = 0;
                for (long chunk : source.forcedChunks)
                    if (!references.containsKey(new ChunkKey(level.dimension(), chunk)))
                        additional++;
                if (source.chunks.size() > limits.maxSourceChunks()
                        || !source.chunks.contains(source.center.toLong())
                        || references.size() + additional > limits.globalChunks()) {
                    CompoundTag header = new CompoundTag();
                    header.putString("operation", "denied");
                    PacketDistributor.sendToPlayer(
                            player,
                            new GlassRemoteFramePayload(UUID.randomUUID(), header, List.of()));
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.translatable(
                                    "message.glass.remote_budget"),
                            true);
                    return;
                }
                sources.put(terminal, source);
                for (long chunk : source.forcedChunks)
                    references.merge(new ChunkKey(level.dimension(), chunk), 1, Integer::sum);
                // A level-29 center keeps the radius-two source ENTITY_TICKING; its FULL halo is
                // budgeted separately.
                level.getChunkSource()
                        .addRegionTicket(
                                TICKET, source.center, source.ticketRadius(), source.owner);
                if (source.sable != null) source.sable.acquire(source.owner);
            }
            Watcher watcher =
                    new Watcher(player.getUUID(), requested.get(), source, server.getTickCount());
            watchers.put(player.getUUID(), watcher);
            source.viewers.add(player.getUUID());
        }

        void tick() {
            int now = server.getTickCount();
            for (Watcher watcher : List.copyOf(watchers.values())) {
                ServerPlayer player = server.getPlayerList().getPlayer(watcher.player);
                var authorized =
                        player == null
                                ? Optional
                                        .<io.github.recrivenvi.glass.state.GlassStateSnapshot
                                                        .Terminal>
                                                empty()
                                : GlassServerState.remoteTerminal(player, watcher.projector);
                if (player == null
                        || now - watcher.lastSeen > limits.idleTicks()
                        || authorized.isEmpty()
                        || !authorized.get().pos().equals(watcher.source.terminal)) {
                    remove(watcher.player);
                    continue;
                }
            }
            for (Source source : List.copyOf(sources.values())) updateShape(source, now);
            for (Watcher watcher : List.copyOf(watchers.values())) {
                ServerPlayer player = server.getPlayerList().getPlayer(watcher.player);
                Source source = watcher.source;
                var terminalChunk =
                        source.level
                                .getChunkSource()
                                .getChunk(
                                        source.terminal.pos().getX() >> 4,
                                        source.terminal.pos().getZ() >> 4,
                                        ChunkStatus.FULL,
                                        false);
                if (terminalChunk == null) continue;
                if (!source.level
                        .getBlockState(source.terminal.pos())
                        .is(GlassContent.TERMINAL.get())) {
                    remove(watcher.player);
                    continue;
                }
                if (!watcher.begun) {
                    CompoundTag begin = source.header();
                    begin.putString("operation", "begin");
                    PacketDistributor.sendToPlayer(
                            player,
                            new GlassRemoteFramePayload(watcher.generation, begin, List.of()));
                    watcher.begun = true;
                }
                if (now - watcher.windowTick >= 20) {
                    watcher.windowTick = now;
                    watcher.windowBytes = 0;
                }
                if (source.sable != null && source.sable.hasShips() && now % 2 == 0)
                    send(player, watcher, List.of());
                if (watcher.sent.size() == source.chunks.size() && now % 3 == 0)
                    updateEntities(player, watcher);
                if (now >= watcher.nextSnapshot) {
                    Long next =
                            source.chunks.stream()
                                    .filter(c -> !watcher.sent.contains(c))
                                    .findFirst()
                                    .orElseGet(
                                            () -> watcher.dirty.stream().findFirst().orElse(null));
                    if (next == null
                            && now - watcher.lastRefresh
                                    >= Math.max(1, limits.refreshTicks() / source.chunks.size())) {
                        next = source.chunks.get(watcher.refreshCursor++ % source.chunks.size());
                        watcher.lastRefresh = now;
                    }
                    if (next != null) {
                        ChunkPos pos = new ChunkPos(next);
                        var plot = source.sable == null ? null : source.sable.chunk(next);
                        var chunk =
                                plot == null
                                        ? source.level.getChunkSource().getChunkNow(pos.x, pos.z)
                                        : plot;
                        if (chunk != null && chunk.isLightCorrect()) {
                            var light =
                                    plot == null
                                            ? source.level.getLightEngine()
                                            : source.sable.lightEngine(next);
                            net.minecraft.network.protocol.Packet<
                                            ? super
                                                    net.minecraft.network.protocol.game
                                                            .ClientGamePacketListener>
                                    snapshot =
                                            watcher.sent.contains(next)
                                                    ? new net.minecraft.network.protocol.game
                                                            .ClientboundLightUpdatePacket(
                                                            pos, light, null, null)
                                                    : new ClientboundLevelChunkWithLightPacket(
                                                            chunk, light, null, null);
                            byte[] bytes =
                                    GlassRemotePackets.encode(
                                            source.level.registryAccess(), snapshot);
                            if (send(player, watcher, List.of(bytes))) {
                                watcher.sent.add(next);
                                watcher.dirty.remove(next);
                                watcher.nextSnapshot =
                                        now + Math.max(1, 20 / limits.initialChunksPerSecond());
                            }
                        }
                    }
                }
            }
        }

        void updateShape(Source source, int now) {
            var next = Source.shape(source.terminal, source.level, limits);
            boolean shipsChanged =
                    !(source.sable == null ? List.of() : source.sable.ids())
                            .equals(next.sable() == null ? List.of() : next.sable().ids());
            if (source.center.equals(next.center())
                    && source.chunks.equals(next.chunks())
                    && source.forcedChunks.equals(next.forcedChunks())
                    && !shipsChanged) {
                source.sable = next.sable();
                return;
            }
            int global = references.size();
            for (long old : source.forcedChunks)
                if (!next.forcedChunks().contains(old)
                        && references.get(new ChunkKey(source.level.dimension(), old)) == 1)
                    global--;
            for (long chunk : next.forcedChunks())
                if (!references.containsKey(new ChunkKey(source.level.dimension(), chunk)))
                    global++;
            if (next.chunks().size() > limits.maxSourceChunks()
                    || !next.chunks().contains(next.center().toLong())
                    || global > limits.globalChunks()) {
                for (UUID id : List.copyOf(source.viewers)) {
                    var player = server.getPlayerList().getPlayer(id);
                    if (player != null)
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable(
                                        "message.glass.remote_budget"),
                                true);
                    remove(id);
                }
                return;
            }
            if (source.sable != null) source.sable.release(source.owner);
            source.level
                    .getChunkSource()
                    .removeRegionTicket(TICKET, source.center, source.ticketRadius(), source.owner);
            for (long chunk : source.forcedChunks)
                references.compute(
                        new ChunkKey(source.level.dimension(), chunk),
                        (k, v) -> v == 1 ? null : v - 1);
            source.assign(next);
            for (long chunk : source.forcedChunks)
                references.merge(new ChunkKey(source.level.dimension(), chunk), 1, Integer::sum);
            source.level
                    .getChunkSource()
                    .addRegionTicket(TICKET, source.center, source.ticketRadius(), source.owner);
            if (source.sable != null) source.sable.acquire(source.owner);
            for (UUID id : List.copyOf(source.viewers)) {
                var watcher = watchers.get(id);
                if (shipsChanged) watcher.sent.clear();
                else watcher.sent.retainAll(source.chunks);
                watcher.dirty.retainAll(source.chunks);
                watcher.refreshCursor = 0;
                watcher.nextSnapshot = now;
                var player = server.getPlayerList().getPlayer(id);
                if (player != null && watcher.begun) send(player, watcher, List.of());
            }
        }

        boolean send(ServerPlayer player, Watcher watcher, List<byte[]> packets) {
            CompoundTag header = watcher.source.header();
            header.putString("operation", "data");
            var frame = new GlassRemoteFramePayload(watcher.generation, header, packets);
            var buffer =
                    new net.minecraft.network.RegistryFriendlyByteBuf(
                            io.netty.buffer.Unpooled.buffer(),
                            watcher.source.level.registryAccess());
            int size;
            try {
                GlassRemoteFramePayload.CODEC.encode(buffer, frame);
                size = buffer.readableBytes();
            } finally {
                buffer.release();
            }
            if (size > limits.bytesPerSecond() || size > 1024 * 1024 - 1024) {
                player.displayClientMessage(
                        net.minecraft.network.chat.Component.translatable(
                                "message.glass.remote_budget"),
                        true);
                remove(watcher.player);
                return false;
            }
            if (watcher.windowBytes + size > limits.bytesPerSecond()) return false;
            PacketDistributor.sendToPlayer(player, frame);
            watcher.windowBytes += size;
            return true;
        }

        void updateEntities(ServerPlayer player, Watcher watcher) {
            Source source = watcher.source;
            int extent = source.radius * 16;
            var area =
                    new net.minecraft.world.phys.AABB(
                            source.center.getMinBlockX() - extent,
                            source.level.getMinBuildHeight(),
                            source.center.getMinBlockZ() - extent,
                            source.center.getMaxBlockX() + extent + 1,
                            source.level.getMaxBuildHeight(),
                            source.center.getMaxBlockZ() + extent + 1);
            var entities =
                    source.level.getEntities(
                            (net.minecraft.world.entity.Entity) null,
                            area,
                            e ->
                                    !e.isRemoved()
                                            && source.chunks.contains(e.chunkPosition().toLong()));
            entities.sort(
                    Comparator.comparingDouble(
                            e ->
                                    e.distanceToSqr(
                                            GlassSableServer.center(
                                                    source.level, source.terminal.pos()))));
            var selected = entities.stream().limit(limits.maxEntities()).toList();
            Set<Integer> ids =
                    selected.stream()
                            .map(net.minecraft.world.entity.Entity::getId)
                            .collect(java.util.stream.Collectors.toSet());
            List<byte[]> packets = new ArrayList<>();
            var registry = source.level.registryAccess();
            java.util.function.Consumer<
                            net.minecraft.network.protocol.Packet<
                                    ? super
                                            net.minecraft.network.protocol.game
                                                    .ClientGamePacketListener>>
                    append = packet -> packets.add(GlassRemotePackets.encode(registry, packet));
            int[] removed =
                    watcher.entities.stream()
                            .filter(id -> !ids.contains(id))
                            .mapToInt(Integer::intValue)
                            .toArray();
            if (removed.length > 0)
                append.accept(
                        new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(
                                removed));
            for (var entity : selected) {
                if (!watcher.entities.contains(entity.getId())) {
                    var tracker =
                            new net.minecraft.server.level.ServerEntity(
                                    source.level, entity, 3, true, packet -> {});
                    tracker.sendPairingData(
                            player,
                            new net.neoforged.neoforge.network.bundle.PacketAndPayloadAcceptor<>(
                                    append));
                } else {
                    append.accept(
                            new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(
                                    entity));
                    append.accept(
                            new net.minecraft.network.protocol.game
                                    .ClientboundSetEntityMotionPacket(entity));
                    append.accept(
                            new net.minecraft.network.protocol.game.ClientboundRotateHeadPacket(
                                    entity,
                                    (byte)
                                            net.minecraft.util.Mth.floor(
                                                    entity.getYHeadRot() * 256F / 360F)));
                    var data =
                            new ArrayList<
                                    net.minecraft.network.syncher.SynchedEntityData.DataValue<?>>();
                    for (var item :
                            ((io.github.recrivenvi.glass.mixin.client.EntityDataAccessor)
                                            entity.getEntityData())
                                    .glass$items()) data.add(item.value());
                    if (!data.isEmpty())
                        append.accept(
                                new net.minecraft.network.protocol.game
                                        .ClientboundSetEntityDataPacket(entity.getId(), data));
                    if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                        append.accept(
                                new net.minecraft.network.protocol.game
                                        .ClientboundUpdateAttributesPacket(
                                        entity.getId(),
                                        living.getAttributes().getSyncableAttributes()));
                        var equipment =
                                new ArrayList<
                                        com.mojang.datafixers.util.Pair<
                                                net.minecraft.world.entity.EquipmentSlot,
                                                net.minecraft.world.item.ItemStack>>();
                        for (var slot : net.minecraft.world.entity.EquipmentSlot.values())
                            equipment.add(
                                    com.mojang.datafixers.util.Pair.of(
                                            slot, living.getItemBySlot(slot).copy()));
                        append.accept(
                                new net.minecraft.network.protocol.game
                                        .ClientboundSetEquipmentPacket(entity.getId(), equipment));
                    }
                    append.accept(
                            new net.minecraft.network.protocol.game.ClientboundSetPassengersPacket(
                                    entity));
                }
            }
            if (!packets.isEmpty() && send(player, watcher, packets)) {
                watcher.entities.clear();
                watcher.entities.addAll(ids);
            }
        }

        void remove(UUID playerId) {
            Watcher watcher = watchers.remove(playerId);
            if (watcher == null) return;
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null && watcher.begun) {
                CompoundTag end = new CompoundTag();
                end.putString("operation", "end");
                PacketDistributor.sendToPlayer(
                        player, new GlassRemoteFramePayload(watcher.generation, end, List.of()));
            }
            Source source = watcher.source;
            source.viewers.remove(playerId);
            if (!source.viewers.isEmpty()) return;
            sources.remove(source.terminal);
            if (source.sable != null) source.sable.release(source.owner);
            source.level
                    .getChunkSource()
                    .removeRegionTicket(TICKET, source.center, source.ticketRadius(), source.owner);
            for (long chunk : source.forcedChunks) {
                ChunkKey key = new ChunkKey(source.level.dimension(), chunk);
                int remaining = references.get(key) - 1;
                if (remaining == 0) references.remove(key);
                else references.put(key, remaining);
            }
        }

        @Override
        public void close() {
            for (UUID player : List.copyOf(watchers.keySet())) remove(player);
        }
    }

    private record ChunkKey(
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
            long chunk) {}

    private static final class Source {
        final GlobalPos terminal;
        final ServerLevel level;
        final UUID owner = UUID.randomUUID();
        ChunkPos center;
        int radius;
        GlassSableServer.Scene sable;
        List<Long> chunks, forcedChunks;
        final Set<UUID> viewers = new HashSet<>();

        Source(GlobalPos terminal, ServerLevel level, GlassRemoteLimits limits) {
            this.terminal = terminal;
            this.level = level;
            assign(shape(terminal, level, limits));
        }

        int ticketRadius() {
            return radius + 2;
        }

        void assign(Shape shape) {
            center = shape.center();
            radius = shape.radius();
            sable = shape.sable();
            chunks = shape.chunks();
            forcedChunks = shape.forcedChunks();
        }

        static Shape shape(GlobalPos terminal, ServerLevel level, GlassRemoteLimits limits) {
            var world = GlassSableServer.center(level, terminal.pos());
            var center = new ChunkPos(net.minecraft.core.BlockPos.containing(world));
            int radius = limits.radius();
            var sable = GlassSableServer.scene(level, center, radius);
            List<ChunkPos> positions = new ArrayList<>();
            for (int x = -radius; x <= radius; x++)
                for (int z = -radius; z <= radius; z++)
                    positions.add(new ChunkPos(center.x + x, center.z + z));
            positions.sort(
                    Comparator.comparingInt(
                            p -> Math.abs(p.x - center.x) + Math.abs(p.z - center.z)));
            var plots = sable == null ? List.<Long>of() : sable.chunks();
            var forced = new ArrayList<Long>();
            int fullRadius = radius + 2;
            for (int x = -fullRadius; x <= fullRadius; x++)
                for (int z = -fullRadius; z <= fullRadius; z++)
                    forced.add(ChunkPos.asLong(center.x + x, center.z + z));
            forced.addAll(plots);
            var chunks =
                    new ArrayList<>(
                            positions.stream()
                                    .limit(Math.max(0, limits.maxSourceChunks() - plots.size()))
                                    .map(ChunkPos::toLong)
                                    .toList());
            chunks.addAll(plots);
            return new Shape(center, radius, sable, List.copyOf(chunks), List.copyOf(forced));
        }

        private record Shape(
                ChunkPos center,
                int radius,
                GlassSableServer.Scene sable,
                List<Long> chunks,
                List<Long> forcedChunks) {}

        CompoundTag header() {
            CompoundTag tag = new CompoundTag();
            tag.putString("dimension", level.dimension().location().toString());
            tag.putString(
                    "dimensionType",
                    level.dimensionTypeRegistration()
                            .unwrapKey()
                            .orElseThrow()
                            .location()
                            .toString());
            tag.putLong("terminal", terminal.pos().asLong());
            tag.putInt("centerX", center.x);
            tag.putInt("centerZ", center.z);
            tag.putInt("chunks", chunks.size());
            tag.putInt("radius", radius);
            tag.putLongArray("sourceChunks", chunks);
            tag.putInt("serverTick", level.getServer().getTickCount());
            if (sable != null) tag.put("sableShips", sable.header());
            tag.putInt("difficulty", level.getDifficulty().getId());
            tag.putBoolean("flat", level.isFlat());
            tag.putLong("biomeSeed", BiomeManager.obfuscateSeed(level.getSeed()));
            tag.putLong("gameTime", level.getGameTime());
            tag.putLong("dayTime", level.getDayTime());
            tag.putFloat("rain", level.getRainLevel(1));
            tag.putFloat("thunder", level.getThunderLevel(1));
            return tag;
        }
    }

    private static final class Watcher {
        final UUID player;
        final UUID generation = UUID.randomUUID();
        GlobalPos projector;
        final Source source;
        final Set<Long> sent = new HashSet<>();
        final Set<Integer> entities = new HashSet<>();
        final Set<Long> dirty = new java.util.LinkedHashSet<>();
        int lastSeen, windowTick, windowBytes, nextSnapshot, lastRefresh, refreshCursor;
        boolean begun;

        Watcher(UUID player, GlobalPos projector, Source source, int tick) {
            this.player = player;
            this.projector = projector;
            this.source = source;
            lastSeen = tick;
            windowTick = tick;
        }
    }
}
