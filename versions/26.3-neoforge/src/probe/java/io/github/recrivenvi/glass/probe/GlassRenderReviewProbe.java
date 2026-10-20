package io.github.recrivenvi.glass.probe;

import com.google.gson.GsonBuilder;
import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.content.block.GlassChannelBlock;
import io.github.recrivenvi.glass.content.block.GlassProjectorBlock;
import io.github.recrivenvi.glass.state.GlassSavedState;
import io.github.recrivenvi.glass.state.GlassServerState;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** 显式验证运行的 Arrange；不进入产品 JAR，也不替代 MCP 的画面观察。 */
final class GlassRenderReviewProbe {
    private boolean scheduled;
    private long lastObservation;

    GlassRenderReviewProbe() {
        NeoForge.EVENT_BUS.addListener(this::tick);
    }

    private record Spec(String name, Direction face, BlockPos pos) {
        boolean fades() {
            return name.equals("full_north")
                    || name.equals("glass_water")
                    || name.equals("clouds")
                    || name.equals("wireless");
        }
    }

    private static List<Spec> specifications() {
        List<Spec> specs = new ArrayList<>();
        int index = 0;
        for (Direction face : Direction.values())
            specs.add(
                    new Spec(
                            "full_" + face.getSerializedName(),
                            face,
                            new BlockPos(
                                    1280 + index++ * 48,
                                    80,
                                    face == Direction.EAST
                                            ? 64
                                            : face == Direction.WEST ? -64 : 0)));
        specs.add(new Spec("glass_water", Direction.NORTH, new BlockPos(1568, 80, 0)));
        specs.add(new Spec("clouds", Direction.NORTH, new BlockPos(1616, 196, 0)));
        specs.add(new Spec("clip_touch", Direction.NORTH, new BlockPos(1664, 80, 0)));
        specs.add(new Spec("clip_texel", Direction.NORTH, new BlockPos(1712, 80, 0)));
        specs.add(new Spec("wireless", Direction.NORTH, new BlockPos(1760, 80, 0)));
        return specs;
    }

    private void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (scheduled) {
            if (client.player != null && System.nanoTime() - lastObservation > 500_000_000L) {
                lastObservation = System.nanoTime();
                observe(client);
            }
            return;
        }
        if (client.player == null || client.getSingleplayerServer() == null) return;
        scheduled = true;
        var server = client.getSingleplayerServer();
        server.execute(
                () -> {
                    try {
                        ServerLevel level = server.overworld();
                        GlassSavedState saved = GlassServerState.saved(server);
                        for (int x = 1250; x <= 1800; x++)
                            for (int z = -8; z <= -3; z++)
                                level.setBlockAndUpdate(
                                        new BlockPos(x, 78, z), Blocks.STONE.defaultBlockState());
                        int fullShapes = 0;
                        List<Map<String, Object>> scenes = new ArrayList<>();
                        Map<String, BlockPos> fadePowers = new java.util.LinkedHashMap<>();
                        for (Spec spec : specifications()) {
                            Direction face = spec.face();
                            BlockPos projector = spec.pos();
                            BlockPos terminal = projector.offset(16, 0, 16);
                            Direction horizontal =
                                    face.getAxis().isVertical()
                                            ? Direction.EAST
                                            : face.getClockWise();
                            Direction vertical =
                                    face.getAxis().isVertical() ? Direction.SOUTH : Direction.UP;
                            for (int u = -2; u <= 2; u++)
                                for (int v = -2; v <= 2; v++) {
                                    BlockPos at =
                                            projector.relative(horizontal, u).relative(vertical, v);
                                    level.setBlockAndUpdate(
                                            at.relative(face.getOpposite()),
                                            Blocks.CONCRETE
                                                    .pick(net.minecraft.world.item.DyeColor.MAGENTA)
                                                    .defaultBlockState());
                                    level.setBlockAndUpdate(
                                            at,
                                            Math.abs(u) == 2 || Math.abs(v) == 2
                                                    ? Blocks.STONE.defaultBlockState()
                                                    : GlassContent.EXTENSION
                                                            .get()
                                                            .defaultBlockState());
                                }
                            level.setBlockAndUpdate(
                                    projector,
                                    GlassContent.PROJECTOR
                                            .get()
                                            .defaultBlockState()
                                            .setValue(GlassProjectorBlock.FACING, face));
                            level.setBlockAndUpdate(
                                    projector.relative(face.getOpposite()),
                                    Blocks.REDSTONE_BLOCK.defaultBlockState());
                            level.setBlockAndUpdate(
                                    terminal,
                                    GlassContent.TERMINAL
                                            .get()
                                            .defaultBlockState()
                                            .setValue(GlassChannelBlock.FACING, face));
                            Direction keep = face.getOpposite();
                            int sourceWidth = spec.name().equals("full_north") ? 48 : 5;
                            for (int u = -sourceWidth; u <= sourceWidth; u++)
                                for (int v = -5; v <= 5; v++) {
                                    level.setBlockAndUpdate(
                                            terminal.relative(keep, 6)
                                                    .relative(horizontal, u)
                                                    .relative(vertical, v),
                                            (spec.name().startsWith("clip_")
                                                            ? Blocks.CONCRETE.pick(
                                                                    net.minecraft.world.item
                                                                            .DyeColor.MAGENTA)
                                                            : u < 0
                                                                    ? Blocks.CONCRETE.pick(
                                                                            net.minecraft.world.item
                                                                                    .DyeColor.RED)
                                                                    : Blocks.CONCRETE.pick(
                                                                            net.minecraft.world.item
                                                                                    .DyeColor.LIME))
                                                    .defaultBlockState());
                                }
                            if (spec.name().equals("glass_water")) {
                                for (int y = 78; y <= 82; y++)
                                    for (int x = 0; x <= 2; x++)
                                        for (int z = -3; z <= -1; z++) {
                                            boolean shell =
                                                    y == 78 || y == 82 || x == 0 || x == 2
                                                            || z == -3 || z == -1;
                                            level.setBlockAndUpdate(
                                                    new BlockPos(projector.getX() + x, y, z),
                                                    (shell ? Blocks.GLASS : Blocks.WATER)
                                                            .defaultBlockState());
                                        }
                                for (int y = 79; y <= 81; y++)
                                    level.setBlockAndUpdate(
                                            new BlockPos(projector.getX() - 1, y, -2),
                                            Blocks.GLASS.defaultBlockState());
                                for (int y = 79; y <= 81; y++)
                                    level.setBlockAndUpdate(
                                            new BlockPos(projector.getX() - 1, y, 3),
                                            Blocks.GLASS.defaultBlockState());
                            }
                            if (spec.name().equals("clip_touch")
                                    || spec.name().equals("clip_texel")) {
                                for (int u = -2; u <= 2; u++)
                                    for (int v = -2; v <= 2; v++) {
                                        BlockPos at =
                                                terminal.relative(keep)
                                                        .relative(horizontal, u)
                                                        .relative(vertical, v);
                                        if (spec.name().equals("clip_touch"))
                                            level.setBlockAndUpdate(
                                                    at,
                                                    (u < 0
                                                                    ? Blocks.CONCRETE.pick(
                                                                            net.minecraft.world.item
                                                                                    .DyeColor.RED)
                                                                    : Blocks.CONCRETE.pick(
                                                                            net.minecraft.world.item
                                                                                    .DyeColor.LIME))
                                                            .defaultBlockState());
                                        else
                                            level.setBlockAndUpdate(
                                                    at,
                                                    (u < 0
                                                                    ? GlassRenderReviewBlocks
                                                                            .INSET_RED
                                                                    : GlassRenderReviewBlocks
                                                                            .INSET_GREEN)
                                                            .get()
                                                            .defaultBlockState());
                                    }
                            }
                            GlobalPos projectorPos = GlobalPos.of(level.dimension(), projector);
                            GlobalPos terminalPos = GlobalPos.of(level.dimension(), terminal);
                            UUID channel =
                                    UUID.nameUUIDFromBytes(
                                            ("render-review-" + spec.name())
                                                    .getBytes(StandardCharsets.UTF_8));
                            saved.putTerminal(
                                    new GlassStateSnapshot.Terminal(
                                            terminalPos,
                                            new GlassStateSnapshot.Channel(
                                                    channel,
                                                    "Render " + spec.name(),
                                                    Optional.of(client.player.getUUID()),
                                                    true)));
                            List<GlobalPos> wireless = List.of();
                            if (spec.name().equals("wireless")) {
                                for (int u = -2; u <= 2; u++)
                                    for (int v = -2; v <= 2; v++) {
                                        BlockPos at =
                                                projector
                                                        .relative(horizontal, u)
                                                        .relative(vertical, v);
                                        level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                                        level.setBlockAndUpdate(
                                                at.relative(face.getOpposite()),
                                                Blocks.AIR.defaultBlockState());
                                    }
                                level.setBlockAndUpdate(
                                        projector,
                                        GlassContent.PROJECTOR
                                                .get()
                                                .defaultBlockState()
                                                .setValue(GlassProjectorBlock.FACING, face));
                                level.setBlockAndUpdate(
                                        projector.relative(face.getOpposite()),
                                        Blocks.REDSTONE_BLOCK.defaultBlockState());
                                List<BlockPos> nodes =
                                        List.of(
                                                projector.offset(-2, 2, 0),
                                                projector.offset(2, 2, 0),
                                                projector.offset(2, -2, 0));
                                for (BlockPos node : nodes)
                                    level.setBlockAndUpdate(
                                            node,
                                            GlassContent.WIRELESS_EXTENSION
                                                    .get()
                                                    .defaultBlockState());
                                wireless =
                                        nodes.stream()
                                                .map(pos -> GlobalPos.of(level.dimension(), pos))
                                                .toList();
                                for (int u = -2; u <= 2; u++)
                                    for (int v = -2; v <= 2; v++)
                                        level.setBlockAndUpdate(
                                                projector.offset(u, v, 3),
                                                Blocks.GLASS.defaultBlockState());
                            }
                            saved.putProjector(
                                    new GlassStateSnapshot.Projector(
                                            projectorPos, Optional.of(channel), wireless));
                            Vec3 eye =
                                    Vec3.atCenterOf(projector)
                                            .add(
                                                    Vec3.atLowerCornerOf(face.getUnitVec3i())
                                                            .scale(6));
                            float yaw =
                                    switch (face) {
                                        case NORTH -> 0;
                                        case SOUTH -> 180;
                                        case EAST -> 90;
                                        case WEST -> -90;
                                        case UP, DOWN -> 0;
                                    };
                            float pitch =
                                    face == Direction.UP ? 90 : face == Direction.DOWN ? -90 : 0;
                            scenes.add(
                                    Map.of(
                                            "name",
                                            spec.name(),
                                            "fade",
                                            spec.fades(),
                                            "projector",
                                            List.of(
                                                    projector.getX(),
                                                    projector.getY(),
                                                    projector.getZ()),
                                            "terminal",
                                            List.of(
                                                    terminal.getX(),
                                                    terminal.getY(),
                                                    terminal.getZ()),
                                            "camera",
                                            List.of(eye.x, eye.y - 1.62, eye.z, yaw, pitch),
                                            "power",
                                            List.of(
                                                    projector.relative(face.getOpposite()).getX(),
                                                    projector.relative(face.getOpposite()).getY(),
                                                    projector
                                                            .relative(face.getOpposite())
                                                            .getZ())));
                            for (BlockPos checked :
                                    List.of(projector, terminal, projector.relative(horizontal))) {
                                var state = level.getBlockState(checked);
                                if (GlassContent.isSurface(state)
                                        || state.is(GlassContent.TERMINAL.get())) {
                                    if (net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(
                                                    state.getShape(level, checked),
                                                    net.minecraft.world.phys.shapes.Shapes.block(),
                                                    net.minecraft.world.phys.shapes.BooleanOp
                                                            .NOT_SAME)
                                            || net.minecraft.world.phys.shapes.Shapes
                                                    .joinIsNotEmpty(
                                                            state.getCollisionShape(level, checked),
                                                            net.minecraft.world.phys.shapes.Shapes
                                                                    .block(),
                                                            net.minecraft.world.phys.shapes
                                                                    .BooleanOp.NOT_SAME))
                                        throw new IllegalStateException(
                                                "Incomplete GLASS physical shape at " + checked);
                                    fullShapes++;
                                }
                            }
                            if (spec.fades())
                                fadePowers.put(spec.name(), projector.relative(face.getOpposite()));
                        }
                        GlassServerState.rebuild(server);
                        Path result = Path.of(System.getProperty("validation.result"));
                        Files.createDirectories(result);
                        new GlassRenderReviewClock(server, fadePowers, result);
                        Files.writeString(
                                result.resolve("fixture.json"),
                                new GsonBuilder()
                                                .setPrettyPrinting()
                                                .create()
                                                .toJson(
                                                        Map.of(
                                                                "status",
                                                                "PREPARED",
                                                                "source",
                                                                "project_probe_arrange",
                                                                "physicalFullCubeChecks",
                                                                fullShapes,
                                                                "scenes",
                                                                scenes))
                                        + "\n");
                    } catch (Exception exception) {
                        throw new RuntimeException("Render fixture failed", exception);
                    }
                });
    }

    private static void observe(Minecraft client) {
        var snapshot = io.github.recrivenvi.glass.client.GlassClientState.snapshot();
        List<Map<String, Object>> views = new ArrayList<>();
        for (Spec spec : specifications()) {
            BlockPos pos = spec.pos();
            BlockPos terminal = pos.offset(16, 0, 16);
            var network =
                    snapshot.networks().stream()
                            .filter(value -> value.projector().pos().equals(pos))
                            .findFirst();
            var block = client.level.getBlockState(pos);
            views.add(
                    Map.of(
                            "scene",
                            spec.name(),
                            "projectorLoaded",
                            client.level
                                            .getChunkSource()
                                            .getChunk(
                                                    pos.getX() >> 4,
                                                    pos.getZ() >> 4,
                                                    net.minecraft.world.level.chunk.status
                                                            .ChunkStatus.FULL,
                                                    false)
                                    != null,
                            "terminalLoaded",
                            client.level
                                            .getChunkSource()
                                            .getChunk(
                                                    terminal.getX() >> 4,
                                                    terminal.getZ() >> 4,
                                                    net.minecraft.world.level.chunk.status
                                                            .ChunkStatus.FULL,
                                                    false)
                                    != null,
                            "powered",
                            block.is(GlassContent.PROJECTOR.get())
                                    && block.getValue(GlassProjectorBlock.POWERED),
                            "networkPresent",
                            network.isPresent(),
                            "projectorRegistered",
                            snapshot.projectors().stream()
                                    .anyMatch(value -> value.pos().pos().equals(pos)),
                            "terminalRegistered",
                            snapshot.terminals().stream()
                                    .anyMatch(value -> value.pos().pos().equals(terminal)),
                            "active",
                            network.map(GlassStateSnapshot.Network::active).orElse(false),
                            "surfaces",
                            network.map(value -> value.surfaces().size()).orElse(0)));
        }
        try {
            Path output = Path.of(System.getProperty("validation.result"));
            Path temporary = output.resolve("view-state.tmp");
            Files.writeString(
                    temporary,
                    new GsonBuilder()
                                    .create()
                                    .toJson(
                                            Map.of(
                                                    "worldTick",
                                                    client.level.getGameTime(),
                                                    "player",
                                                    List.of(
                                                            client.player.getX(),
                                                            client.player.getY(),
                                                            client.player.getZ()),
                                                    "views",
                                                    views))
                            + "\n");
            Files.move(
                    temporary,
                    output.resolve("view-state.json"),
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.io.IOException exception) {
            throw new RuntimeException("Could not record GLASS view readiness", exception);
        }
    }
}
