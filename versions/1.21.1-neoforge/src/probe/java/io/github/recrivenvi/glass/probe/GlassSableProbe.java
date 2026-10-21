package io.github.recrivenvi.glass.probe;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import io.github.recrivenvi.glass.state.GlassServerState;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Native Sable assembly and controlled pose fixture, restricted to this validation's own world. */
final class GlassSableProbe {
    private final Map<ServerSubLevel, Pose3d> ships = new LinkedHashMap<>();
    private boolean built, moving;
    private int motionStart;

    GlassSableProbe() {
        NeoForge.EVENT_BUS.addListener(this::tick);
    }

    private void tick(ServerTickEvent.Post event) {
        var output = GlassOverlayProbe.output();
        var server = event.getServer();
        if (!Files.exists(output.resolve("fixture.json")) || server.getTickCount() < 60) return;
        try {
            var level = server.overworld();
            if (!built) {
                built = true;
                SubLevelPhysicsSystem.require(level).setPaused(true);
                String kind = GlassOverlayProbe.sableCase();
                if (kind.equals("terminal") || kind.equals("both")) {
                    var pos = new BlockPos(16, 200, 16);
                    var ship =
                            SubLevelAssemblyHelper.assembleBlocks(
                                    level, pos, List.of(pos), new BoundingBox3i(pos, pos));
                    ships.put(ship, new Pose3d(ship.logicalPose()));
                }
                if (kind.equals("projector") || kind.equals("both")) {
                    List<BlockPos> blocks = new ArrayList<>();
                    for (int x = -2; x <= 2; x++)
                        for (int y = 198; y <= 202; y++)
                            for (int z = 0; z <= 1; z++) blocks.add(new BlockPos(x, y, z));
                    var ship =
                            SubLevelAssemblyHelper.assembleBlocks(
                                    level,
                                    new BlockPos(0, 200, 0),
                                    blocks,
                                    new BoundingBox3i(-2, 198, 0, 2, 202, 1));
                    ships.put(ship, new Pose3d(ship.logicalPose()));
                }
                if (kind.equals("resume"))
                    for (var ship :
                            dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level)
                                    .getAllSubLevels())
                        ships.put(ship, new Pose3d(ship.logicalPose()));
                GlassServerState.rebuild(server);
                Files.writeString(
                        output.resolve("sable-fixture.json"),
                        new com.google.gson.Gson()
                                .toJson(
                                        Map.of(
                                                "case",
                                                kind,
                                                "ships",
                                                ships.keySet().stream()
                                                        .map(
                                                                s ->
                                                                        Map.of(
                                                                                "id",
                                                                                s.getUniqueId()
                                                                                        .toString(),
                                                                                "plot",
                                                                                s.getPlot()
                                                                                        .getCenterBlock()
                                                                                        .toShortString()))
                                                        .toList())));
            }
            var request = output.resolve("sable-request.txt");
            if (Files.exists(request)) {
                String action = Files.readString(request).strip();
                if (!action.isEmpty()) {
                    Files.writeString(request, "");
                    moving = action.equals("motion");
                    motionStart = server.getTickCount();
                    if (action.equals("tilt")) apply(1.25, .4, -.75, .2, .4, -.15, true);
                    if (action.equals("clip")) {
                        for (int x = 14; x <= 18; x++)
                            for (int y = 198; y <= 202; y++)
                                for (int z = 15; z <= 19; z++)
                                    if (x == 14 || x == 18 || y == 198 || y == 202)
                                        level.setBlockAndUpdate(
                                                new BlockPos(x, y, z),
                                                ((x + y + z) % 2 == 0
                                                                ? net.minecraft.world.level.block
                                                                        .Blocks.CYAN_CONCRETE
                                                                : net.minecraft.world.level.block
                                                                        .Blocks.ORANGE_CONCRETE)
                                                        .defaultBlockState());
                    }
                    if (action.equals("far-shift")) {
                        for (int x = 8203; x <= 8213; x++)
                            for (int y = 195; y <= 205; y++)
                                level.setBlockAndUpdate(
                                        new BlockPos(x, y, 22),
                                        (x < 8208
                                                        ? net.minecraft.world.level.block.Blocks
                                                                .YELLOW_CONCRETE
                                                        : net.minecraft.world.level.block.Blocks
                                                                .BLUE_CONCRETE)
                                                .defaultBlockState());
                        apply(8192, 0, 0, 0, 0, 0, false);
                    }
                    if (action.equals("shift")) {
                        for (int x = 59; x <= 69; x++)
                            for (int y = 195; y <= 205; y++)
                                level.setBlockAndUpdate(
                                        new BlockPos(x, y, 22),
                                        (x < 64
                                                        ? net.minecraft.world.level.block.Blocks
                                                                .YELLOW_CONCRETE
                                                        : net.minecraft.world.level.block.Blocks
                                                                .BLUE_CONCRETE)
                                                .defaultBlockState());
                        apply(48, 0, 0, 0, 0, 0, false);
                    }
                    if (action.equals("unload")) {
                        var container =
                                dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(
                                        level);
                        for (var ship : List.copyOf(container.getAllSubLevels()))
                            container
                                    .getHoldingChunkMap()
                                    .moveToUnloaded(
                                            ship,
                                            new net.minecraft.world.level.ChunkPos(
                                                    net.minecraft.core.BlockPos.containing(
                                                            ship.logicalPose()
                                                                    .transformPosition(
                                                                            net.minecraft.world.phys
                                                                                    .Vec3
                                                                                    .atCenterOf(
                                                                                            ship.getPlot()
                                                                                                    .getCenterBlock())))));
                        container.processSubLevelRemovals();
                        container.getHoldingChunkMap().saveAll();
                        Files.writeString(
                                output.resolve("unloaded-state.json"),
                                new com.google.gson.Gson()
                                        .toJson(
                                                Map.of(
                                                        "loadedShips",
                                                        container.getAllSubLevels().size(),
                                                        "links",
                                                        GlassServerState.saved(server)
                                                                .sableLinks()
                                                                .values()
                                                                .stream()
                                                                .map(Object::toString)
                                                                .toList())));
                    }
                    if (action.equals("reset")) apply(0, 0, 0, 0, 0, 0, false);
                    Files.writeString(output.resolve("sable-arranged.txt"), action);
                }
            }
            if (moving) {
                double t = (server.getTickCount() - motionStart) * .04;
                apply(
                        Math.sin(t) * 1.5,
                        Math.sin(t * .6) * .6,
                        Math.cos(t) * 1.25,
                        Math.sin(t * .7) * .3,
                        t * .2,
                        Math.cos(t * .8) * .23,
                        true);
            }
            if (server.getTickCount() % 10 == 0)
                Files.writeString(
                        output.resolve("sable-state.json"),
                        new com.google.gson.Gson()
                                .toJson(
                                        Map.of(
                                                "moving",
                                                moving,
                                                "ships",
                                                ships.keySet().stream()
                                                        .map(
                                                                s ->
                                                                        Map.of(
                                                                                "pose",
                                                                                Pose3d.CODEC
                                                                                        .encodeStart(
                                                                                                net
                                                                                                        .minecraft
                                                                                                        .nbt
                                                                                                        .NbtOps
                                                                                                        .INSTANCE,
                                                                                                s
                                                                                                        .logicalPose())
                                                                                        .getOrThrow()
                                                                                        .toString(),
                                                                                "renderedWorldCenter",
                                                                                s.logicalPose()
                                                                                        .transformPosition(
                                                                                                net
                                                                                                        .minecraft
                                                                                                        .world
                                                                                                        .phys
                                                                                                        .Vec3
                                                                                                        .atCenterOf(
                                                                                                                s.getPlot()
                                                                                                                        .getCenterBlock()))
                                                                                        .toString()))
                                                        .toList())));
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    }

    private void apply(
            double x, double y, double z, double pitch, double yaw, double roll, boolean scaled) {
        ships.forEach((ship, base) -> move(ship, base, x, y, z, pitch, yaw, roll, scaled));
    }

    static void move(
            ServerSubLevel ship,
            Pose3d base,
            double x,
            double y,
            double z,
            double pitch,
            double yaw,
            double roll,
            boolean scaled) {
        var pose = ship.logicalPose().set(base);
        pose.position().add(x, y, z);
        pose.orientation().rotateXYZ(pitch, yaw, roll);
        if (scaled) {
            pose.rotationPoint().add(.125, .25, -.125);
            pose.scale().mul(.95, 1.1, 1.05);
        }
        var physics = SubLevelPhysicsSystem.require(ship.getLevel());
        physics.getPhysicsHandle(ship).teleport(pose.position(), pose.orientation());
        ship.forceUpdateGlobalBounds();
        ship.updateBoundingBox();
    }
}
