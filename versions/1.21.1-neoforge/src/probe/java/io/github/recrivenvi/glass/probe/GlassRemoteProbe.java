package io.github.recrivenvi.glass.probe;

import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.content.block.GlassChannelBlock;
import io.github.recrivenvi.glass.remote.GlassRemoteSubscriptions;
import io.github.recrivenvi.glass.state.GlassServerState;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Native fixtures and observations for the remote-view trial; commands are absent from product
 * jars.
 */
final class GlassRemoteProbe {
    private GlobalPos terminal;
    private net.minecraft.world.entity.animal.Pig actor;

    GlassRemoteProbe() {
        NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ClientTickEvent.Post event) -> {
                    var client = net.minecraft.client.Minecraft.getInstance();
                    if (client.level == null || client.level.getGameTime() % 10 != 0) return;
                    try {
                        var field =
                                io.github.recrivenvi.glass.client.render.GlassRemoteViews.class
                                        .getDeclaredField("VIEWS");
                        field.setAccessible(true);
                        var counts = new java.util.ArrayList<String>();
                        for (Object view : ((java.util.Map<?, ?>) field.get(null)).values()) {
                            var particles = view.getClass().getDeclaredField("particles");
                            particles.setAccessible(true);
                            counts.add(
                                    ((net.minecraft.client.particle.ParticleEngine)
                                                    particles.get(view))
                                            .countParticles());
                        }
                        Files.writeString(
                                GlassOverlayProbe.output().resolve("source-particles.json"),
                                new com.google.gson.Gson().toJson(counts));
                    } catch (ReflectiveOperationException | java.io.IOException failure) {
                        throw new RuntimeException(failure);
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (RegisterCommandsEvent event) -> {
                    var root = Commands.literal("glass_remote").requires(s -> s.hasPermission(2));
                    for (String kind :
                            new String[] {
                                "near", "far", "nether", "change", "private", "public", "refs",
                                "entity", "scale", "death"
                            })
                        root.then(
                                Commands.literal(kind)
                                        .executes(
                                                context -> {
                                                    var player =
                                                            context.getSource()
                                                                    .getPlayerOrException();
                                                    var server = context.getSource().getServer();
                                                    var saved = GlassServerState.saved(server);
                                                    var projector =
                                                            saved.projectors()
                                                                    .get(
                                                                            GlobalPos.of(
                                                                                    Level.OVERWORLD,
                                                                                    new BlockPos(
                                                                                            0, 200,
                                                                                            0)));
                                                    if (kind.equals("refs")) {
                                                        references(player, projector.pos());
                                                        return 1;
                                                    }
                                                    if (kind.equals("entity")) {
                                                        var level =
                                                                server.getLevel(
                                                                        terminal.dimension());
                                                        actor =
                                                                net.minecraft.world.entity
                                                                        .EntityType.PIG
                                                                        .create(level);
                                                        actor.setPos(
                                                                terminal.pos().getX() - .5,
                                                                199.5,
                                                                terminal.pos().getZ() + 4.5);
                                                        actor.setNoGravity(true);
                                                        actor.setPersistenceRequired();
                                                        level.addFreshEntity(actor);
                                                    } else if (kind.equals("scale"))
                                                        actor.getAttribute(
                                                                        net.minecraft.world.entity
                                                                                .ai.attributes
                                                                                .Attributes.SCALE)
                                                                .setBaseValue(1.5);
                                                    else if (kind.equals("death"))
                                                        actor.hurt(
                                                                actor.damageSources().generic(),
                                                                Float.MAX_VALUE);
                                                    else if (kind.equals("change")) {
                                                        ServerLevel level =
                                                                server.getLevel(
                                                                        terminal.dimension());
                                                        for (int u = -5; u <= 5; u++)
                                                            for (int v = -5; v <= 5; v++)
                                                                level.setBlockAndUpdate(
                                                                        terminal.pos()
                                                                                .offset(u, v, 6),
                                                                        Blocks.BLUE_CONCRETE
                                                                                .defaultBlockState());
                                                        level.sendParticles(
                                                                net.minecraft.core.particles
                                                                        .ParticleTypes.FLAME,
                                                                terminal.pos().getX() + .5,
                                                                201,
                                                                terminal.pos().getZ() + 3.5,
                                                                30,
                                                                .7,
                                                                .7,
                                                                .7,
                                                                .02);
                                                        level.levelEvent(
                                                                2001,
                                                                terminal.pos().offset(0, 1, 3),
                                                                net.minecraft.world.level.block
                                                                        .Block.getId(
                                                                        Blocks.GOLD_BLOCK
                                                                                .defaultBlockState()));
                                                        level.destroyBlockProgress(
                                                                23456,
                                                                terminal.pos().offset(0, 0, 6),
                                                                5);
                                                    } else if (kind.equals("private")
                                                            || kind.equals("public")) {
                                                        var old = saved.terminals().get(terminal);
                                                        saved.putTerminal(
                                                                new GlassStateSnapshot.Terminal(
                                                                        terminal,
                                                                        new GlassStateSnapshot
                                                                                .Channel(
                                                                                old.channel().id(),
                                                                                old.channel()
                                                                                        .name(),
                                                                                kind.equals(
                                                                                                "private")
                                                                                        ? Optional
                                                                                                .of(
                                                                                                        java
                                                                                                                .util
                                                                                                                .UUID
                                                                                                                .randomUUID())
                                                                                        : Optional
                                                                                                .of(
                                                                                                        player
                                                                                                                .getUUID()),
                                                                                kind.equals(
                                                                                        "public"))));
                                                    } else {
                                                        for (var old :
                                                                java.util.List.copyOf(
                                                                        saved.terminals().values()))
                                                            if (old.channel()
                                                                    .id()
                                                                    .equals(
                                                                            projector
                                                                                    .channelId()
                                                                                    .orElseThrow()))
                                                                saved.remove(old.pos());
                                                        ServerLevel level =
                                                                kind.equals("nether")
                                                                        ? server.getLevel(
                                                                                Level.NETHER)
                                                                        : server.overworld();
                                                        BlockPos pos =
                                                                new BlockPos(
                                                                        kind.equals("far")
                                                                                ? 8192
                                                                                : 16,
                                                                        200,
                                                                        16);
                                                        terminal =
                                                                GlobalPos.of(
                                                                        level.dimension(), pos);
                                                        for (int x = -2; x <= 2; x++)
                                                            for (int z = -2; z <= 2; z++)
                                                                level.getChunk(
                                                                        (pos.getX() >> 4) + x,
                                                                        (pos.getZ() >> 4) + z);
                                                        for (int u = -6; u <= 6; u++)
                                                            for (int v = -6; v <= 6; v++)
                                                                for (int w = -2; w <= 8; w++)
                                                                    level.setBlockAndUpdate(
                                                                            pos.offset(u, v, w),
                                                                            Blocks.AIR
                                                                                    .defaultBlockState());
                                                        level.setBlockAndUpdate(
                                                                pos,
                                                                GlassContent.TERMINAL
                                                                        .get()
                                                                        .defaultBlockState()
                                                                        .setValue(
                                                                                GlassChannelBlock
                                                                                        .FACING,
                                                                                Direction.NORTH));
                                                        for (int u = -5; u <= 5; u++)
                                                            for (int v = -5; v <= 5; v++)
                                                                level.setBlockAndUpdate(
                                                                        pos.offset(u, v, 6),
                                                                        (u < 0
                                                                                        ? Blocks
                                                                                                .LIME_CONCRETE
                                                                                        : Blocks
                                                                                                .RED_CONCRETE)
                                                                                .defaultBlockState());
                                                        saved.putTerminal(
                                                                new GlassStateSnapshot.Terminal(
                                                                        terminal,
                                                                        new GlassStateSnapshot
                                                                                .Channel(
                                                                                projector
                                                                                        .channelId()
                                                                                        .orElseThrow(),
                                                                                "Remote " + kind,
                                                                                Optional.of(
                                                                                        player
                                                                                                .getUUID()),
                                                                                true)));
                                                    }
                                                    GlassServerState.rebuild(server);
                                                    return 1;
                                                }));
                    event.getDispatcher().register(root);
                });
        NeoForge.EVENT_BUS.addListener(
                (ServerTickEvent.Post event) -> {
                    if (actor != null && !actor.isRemoved() && actor.isAlive())
                        actor.setDeltaMovement(.012, 0, 0);
                    if (event.getServer().getTickCount() % 10 != 0) return;
                    try {
                        var task = GlassOverlayProbe.output().resolve("remote-request.txt");
                        if (Files.exists(task)) {
                            String action = Files.readString(task).strip();
                            if (!action.isEmpty()) {
                                Files.writeString(task, "");
                                var player =
                                        event.getServer().getPlayerList().getPlayers().getFirst();
                                event.getServer()
                                        .getCommands()
                                        .performPrefixedCommand(
                                                player.createCommandSourceStack(),
                                                "glass_remote " + action);
                                Files.writeString(
                                        task.resolveSibling("remote-arranged.txt"), action);
                            }
                        }
                        var client = net.minecraft.client.Minecraft.getInstance();
                        Map<String, Object> state = new java.util.LinkedHashMap<>();
                        var mods = new java.util.LinkedHashMap<String, String>();
                        for (String id :
                                new String[] {
                                    "glass", "sable", "sodium", "minecraft_protocol_probe"
                                })
                            net.neoforged.fml.ModList.get()
                                    .getModContainerById(id)
                                    .ifPresent(
                                            mod ->
                                                    mods.put(
                                                            id,
                                                            mod.getModInfo()
                                                                    .getVersion()
                                                                    .toString()));
                        state.put("runtimeMods", mods);
                        state.put("mainParticleCount", client.particleEngine.countParticles());
                        if (client.level != null && client.player != null)
                            state.put(
                                    "mainPlayerBiome",
                                    client.level
                                            .getBiome(client.player.blockPosition())
                                            .unwrapKey()
                                            .orElseThrow()
                                            .location()
                                            .toString());
                        event.getServer().getPlayerList().getPlayers().stream()
                                .findFirst()
                                .ifPresent(
                                        player ->
                                                state.put(
                                                        "viewerUUID", player.getUUID().toString()));
                        state.put(
                                "sources",
                                GlassRemoteSubscriptions.activeSources(event.getServer()));
                        if (actor != null)
                            state.put(
                                    "sourceActor",
                                    Map.of(
                                            "x",
                                            actor.getX(),
                                            "ticks",
                                            actor.tickCount,
                                            "deathTime",
                                            actor.deathTime,
                                            "removed",
                                            actor.isRemoved()));
                        state.put(
                                "sourceChunks",
                                GlassRemoteSubscriptions.streamedSourceChunks(event.getServer()));
                        state.put(
                                "forcedFullChunks",
                                GlassRemoteSubscriptions.heldSourceChunks(event.getServer()));
                        state.put("nativeGlassTickets", nativeTickets(event.getServer()));
                        if (net.neoforged.fml.ModList.get().isLoaded("sable")) {
                            state.put("sableGlassTickets", sableTickets(event.getServer()));
                            state.put(
                                    "serverSableShips",
                                    java.util.stream.StreamSupport.stream(
                                                    event.getServer().getAllLevels().spliterator(),
                                                    false)
                                            .mapToInt(
                                                    l ->
                                                            dev.ryanhcode.sable.api.sublevel
                                                                    .SubLevelContainer.getContainer(
                                                                            l)
                                                                    .getAllSubLevels()
                                                                    .size())
                                            .sum());
                        }
                        state.put(
                                "views",
                                io.github.recrivenvi.glass.client.render.GlassRemoteViews
                                        .activeViews());
                        state.put(
                                "mainConnectionWorldUnchanged",
                                client.getConnection() != null
                                        && client.getConnection().getLevel() == client.level);
                        state.put(
                                "terminalMainLoaded",
                                terminal != null
                                        && client.level != null
                                        && terminal.dimension().equals(client.level.dimension())
                                        && client.level
                                                        .getChunkSource()
                                                        .getChunk(
                                                                terminal.pos().getX() >> 4,
                                                                terminal.pos().getZ() >> 4,
                                                                net.minecraft.world.level.chunk
                                                                        .status.ChunkStatus.FULL,
                                                                false)
                                                != null);
                        state.put(
                                "forcedChunks",
                                event.getServer().overworld().getForcedChunks().size());
                        Files.writeString(
                                GlassOverlayProbe.output().resolve("remote-state.json"),
                                new com.google.gson.Gson().toJson(state));
                    } catch (java.io.IOException error) {
                        throw new java.io.UncheckedIOException(error);
                    }
                });
    }

    private static int nativeTickets(net.minecraft.server.MinecraftServer server) {
        try {
            var distance =
                    net.minecraft.server.level.ServerChunkCache.class.getDeclaredField(
                            "distanceManager");
            distance.setAccessible(true);
            var tickets =
                    net.minecraft.server.level.DistanceManager.class.getDeclaredField("tickets");
            tickets.setAccessible(true);
            int total = 0;
            for (var level : server.getAllLevels()) {
                var map =
                        (it.unimi.dsi.fastutil.longs.Long2ObjectMap<?>)
                                tickets.get(distance.get(level.getChunkSource()));
                for (var value : map.values())
                    for (var ticket : (Iterable<?>) value)
                        if (((net.minecraft.server.level.Ticket<?>) ticket)
                                .getType()
                                .toString()
                                .equals("glass_remote_view")) total++;
            }
            return total;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static long sableTickets(net.minecraft.server.MinecraftServer server) {
        long count = 0;
        for (var level : server.getAllLevels())
            for (var set :
                    dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level)
                            .collectForceLoadTickets()
                            .values())
                count +=
                        set.stream()
                                .filter(t -> t.type().name().toString().equals("glass:remote_view"))
                                .count();
        return count;
    }

    private static void references(
            net.minecraft.server.level.ServerPlayer player, GlobalPos projector) {
        var server = player.serverLevel().getServer();
        var out = GlassOverlayProbe.output();
        var actor =
                net.neoforged.neoforge.common.util.FakePlayerFactory.get(
                        player.serverLevel(),
                        new com.mojang.authlib.GameProfile(
                                java.util.UUID.nameUUIDFromBytes(
                                        "glass-owner-reference-probe"
                                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                                "GlassRefProbe"));
        var position =
                io.github.recrivenvi.glass.remote.GlassSableServer.center(
                        player.serverLevel(), projector.pos());
        actor.setPos(position.x, position.y, position.z);
        java.util.List<Map<String, Object>> stages = new java.util.ArrayList<>();
        GlassRemoteSubscriptions.watch(actor, Optional.of(projector));
        stages.add(
                Map.of(
                        "stage",
                        "two_native_server_actors",
                        "sources",
                        GlassRemoteSubscriptions.activeSources(server),
                        "tickets",
                        nativeTickets(server)));
        GlassRemoteSubscriptions.watch(player, Optional.empty());
        stages.add(
                Map.of(
                        "stage",
                        "one_reference_remains",
                        "sources",
                        GlassRemoteSubscriptions.activeSources(server),
                        "tickets",
                        nativeTickets(server)));
        GlassRemoteSubscriptions.watch(actor, Optional.empty());
        stages.add(
                Map.of(
                        "stage",
                        "last_reference_released",
                        "sources",
                        GlassRemoteSubscriptions.activeSources(server),
                        "tickets",
                        nativeTickets(server)));
        GlassRemoteSubscriptions.watch(player, Optional.of(projector));
        if (!stages.get(0).get("sources").equals(1)
                || !stages.get(1).get("tickets").equals(1)
                || !stages.get(2).get("tickets").equals(0)) throw new AssertionError(stages);
        try {
            Files.writeString(
                    out.resolve("owner-reference-stages.json"),
                    new com.google.gson.Gson()
                            .toJson(
                                    Map.of(
                                            "actors",
                                            "one real client and one project-owned native FakePlayer; subscriptions added and removed before the next server tick",
                                            "stages",
                                            stages)));
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    }
}
