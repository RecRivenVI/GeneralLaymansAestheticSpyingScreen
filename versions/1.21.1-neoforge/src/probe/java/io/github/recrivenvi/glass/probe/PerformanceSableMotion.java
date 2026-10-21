package io.github.recrivenvi.glass.probe;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;

/** Slow continuous translation crosses a source chunk during the measured motion window. */
public final class PerformanceSableMotion {
    private static final Map<UUID, Pose3d> BASES = new LinkedHashMap<>();
    private static MinecraftServer active;
    private static int start;
    private static boolean moving;

    private PerformanceSableMotion() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.server.ServerStartedEvent event) ->
                        setup(event.getServer()));
    }

    public static void setup(MinecraftServer server) {
        active = server;
        BASES.clear();
        moving = false;
        SubLevelPhysicsSystem.require(server.overworld()).setPaused(true);
        for (int x = 8; x <= 112; x++)
            for (int y = 195; y <= 205; y++)
                server.overworld()
                        .setBlockAndUpdate(
                                new net.minecraft.core.BlockPos(x, y, 22),
                                (x < 24
                                                ? net.minecraft.world.level.block.Blocks
                                                        .RED_CONCRETE
                                                : net.minecraft.world.level.block.Blocks
                                                        .LIME_CONCRETE)
                                        .defaultBlockState());
        for (var ship : SubLevelContainer.getContainer(server.overworld()).getAllSubLevels())
            BASES.put(ship.getUniqueId(), new Pose3d(ship.logicalPose()));
        NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) -> {
                    if (event.getServer() != active || !moving) return;
                    double ticks = active.getTickCount() - start;
                    for (var ship :
                            SubLevelContainer.getContainer(active.overworld()).getAllSubLevels())
                        GlassSableProbe.move(
                                ship,
                                BASES.computeIfAbsent(
                                        ship.getUniqueId(), id -> new Pose3d(ship.logicalPose())),
                                ticks * .04,
                                0,
                                0,
                                Math.sin(ticks * .01) * .2,
                                Math.sin(ticks * .013) * .25,
                                Math.cos(ticks * .01) * .12,
                                true);
                });
    }

    public static void motion(boolean enabled) {
        moving = enabled;
        start = active.getTickCount();
    }
}
