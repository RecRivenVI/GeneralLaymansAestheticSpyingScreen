package io.github.recrivenvi.glass.probe.performance;

import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.content.block.GlassChannelBlock;
import io.github.recrivenvi.glass.content.block.GlassProjectorBlock;
import io.github.recrivenvi.glass.state.GlassServerState;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** The four physical panels stay in the same view even in the unpowered baseline. */
public final class PerformanceScene {
    private static final UUID CHANNEL =
            UUID.nameUUIDFromBytes(
                    "glass-performance-source".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final int[] X = {-9, -3, 3, 9};
    private static final List<net.minecraft.world.entity.Entity> ACTORS = new ArrayList<>();
    private static GlobalPos terminal;
    private static net.minecraft.server.level.ServerLevel actorLevel;

    private PerformanceScene() {}

    public static Object entityTicks() {
        return ACTORS.stream()
                .map(
                        reference -> {
                            var actor = actorLevel.getEntity(reference.getUUID());
                            return Map.of(
                                    "uuid",
                                    reference.getUUID().toString(),
                                    "loaded",
                                    actor != null,
                                    "ticks",
                                    actor == null ? -1 : actor.tickCount,
                                    "alive",
                                    actor != null && actor.isAlive());
                        })
                .toList();
    }

    public static void arrange(MinecraftServer server, String place) {
        var main = server.overworld();
        main.setDayTime(6000);
        main.getGameRules()
                .getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)
                .set(false, server);
        main.setWeatherParameters(6000, 0, false, false);
        var saved = GlassServerState.saved(server);
        for (var previous : List.copyOf(saved.terminals().values()))
            if (previous.channel().id().equals(CHANNEL)) saved.remove(previous.pos());
        ACTORS.forEach(net.minecraft.world.entity.Entity::discard);
        ACTORS.clear();
        var source = place.startsWith("nether") ? server.getLevel(Level.NETHER) : main;
        actorLevel = source;
        var center =
                new BlockPos(
                        (place.startsWith("far") ? 8192 : 48) + (place.endsWith("shift") ? 32 : 0),
                        80,
                        24);
        terminal = GlobalPos.of(source.dimension(), center);
        for (int u = -22; u <= 22; u++)
            for (int v = -2; v <= 8; v++)
                for (int z = -20; z <= 8; z++)
                    source.setBlockAndUpdate(
                            center.offset(u, v, z), Blocks.AIR.defaultBlockState());
        for (int u = -16; u <= 16; u++)
            for (int v = -16; v <= 16; v++) {
                source.setBlockAndUpdate(center.offset(u, -3, v), Blocks.STONE.defaultBlockState());
                if (u % 4 == 0 && v % 4 == 0)
                    for (int y = -2; y <= 1; y++)
                        source.setBlockAndUpdate(
                                center.offset(u, y, v), Blocks.GLASS.defaultBlockState());
            }
        for (int u = -20; u <= 20; u++)
            for (int v = -8; v <= 8; v++)
                source.setBlockAndUpdate(
                        center.offset(u, v, 8),
                        (place.endsWith("shift")
                                        ? (u < 0 ? Blocks.BLUE_CONCRETE : Blocks.YELLOW_CONCRETE)
                                        : (u < 0 ? Blocks.LIME_CONCRETE : Blocks.RED_CONCRETE))
                                .defaultBlockState());
        source.setBlockAndUpdate(
                center,
                GlassContent.TERMINAL
                        .get()
                        .defaultBlockState()
                        .setValue(GlassChannelBlock.FACING, Direction.NORTH));
        var owner = server.getPlayerList().getPlayers().getFirst().getUUID();
        saved.putTerminal(
                new GlassStateSnapshot.Terminal(
                        terminal,
                        new GlassStateSnapshot.Channel(
                                CHANNEL, "Performance", Optional.of(owner), true)));
        for (int i = 0; i < 32; i++) {
            var actor = EntityType.PIG.create(source);
            actor.setPos(
                    center.getX() - 15 + 4 * (i % 8) + .5,
                    78,
                    center.getZ() - 11 + 4 * (i / 8) + .5);
            actor.setNoAi(Boolean.getBoolean("glass.performance.entities.no_ai"));
            actor.setPersistenceRequired();
            source.addFreshEntity(actor);
            ACTORS.add(actor);
        }
        for (int x : X) {
            var pos = new BlockPos(x, 80, 0);
            for (int u = -1; u <= 1; u++)
                for (int v = -1; v <= 1; v++)
                    main.setBlockAndUpdate(
                            pos.offset(u, v, 0), GlassContent.EXTENSION.get().defaultBlockState());
            main.setBlockAndUpdate(
                    pos,
                    GlassContent.PROJECTOR
                            .get()
                            .defaultBlockState()
                            .setValue(GlassProjectorBlock.FACING, Direction.NORTH));
            saved.putProjector(
                    new GlassStateSnapshot.Projector(
                            GlobalPos.of(main.dimension(), pos), Optional.of(CHANNEL), List.of()));
        }
        for (int x = -20; x <= 20; x++)
            for (int z = -20; z <= 2; z++)
                main.setBlockAndUpdate(new BlockPos(x, 77, z), Blocks.STONE.defaultBlockState());
        power(server, 0);
    }

    public static void power(MinecraftServer server, int networks) {
        var level = server.overworld();
        for (int i = 0; i < X.length; i++)
            level.setBlockAndUpdate(
                    new BlockPos(X[i], 80, 1),
                    (i < networks ? Blocks.REDSTONE_BLOCK : Blocks.AIR).defaultBlockState());
        GlassServerState.rebuild(server);
    }
}
