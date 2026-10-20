package io.github.recrivenvi.glass.probe;

import com.google.gson.GsonBuilder;
import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.content.block.GlassChannelBlock;
import io.github.recrivenvi.glass.content.block.GlassProjectorBlock;
import io.github.recrivenvi.glass.state.GlassSavedState;
import io.github.recrivenvi.glass.state.GlassServerState;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Creates a fixed scene only in this validation's copied world; MCP performs the observation. */
@Mod(value = "glass_probe", dist = Dist.CLIENT)
public final class GlassOverlayProbe {
    private boolean scheduled;
    private long lastObservation;
    private boolean creating;

    public GlassOverlayProbe() {
        NeoForge.EVENT_BUS.addListener(this::tick);
    }

    private void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (Boolean.getBoolean("glass.overlay.fresh_world")
                && !creating
                && client.isGameLoadFinished()
                && client.screen instanceof net.minecraft.client.gui.screens.TitleScreen
                && client.getOverlay() == null) {
            creating = true;
            client.createWorldOpenFlows()
                    .createFreshLevel(
                            "GLASS Overlay 21 Fresh",
                            new net.minecraft.world.level.LevelSettings(
                                    "GLASS Overlay 21 Fresh",
                                    net.minecraft.world.level.GameType.CREATIVE,
                                    false,
                                    net.minecraft.world.Difficulty.PEACEFUL,
                                    true,
                                    new net.minecraft.world.level.GameRules(),
                                    net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                            new net.minecraft.world.level.levelgen.WorldOptions(
                                    20261020L, false, false),
                            access ->
                                    access.registryOrThrow(
                                                    net.minecraft.core.registries.Registries
                                                            .WORLD_PRESET)
                                            .getOrThrow(
                                                    net.minecraft.world.level.levelgen.presets
                                                            .WorldPresets.FLAT)
                                            .createWorldDimensions(),
                            client.screen);
        }
        if (client.player == null || client.getSingleplayerServer() == null) return;
        if (scheduled) {
            if (System.nanoTime() - lastObservation > 500_000_000L) {
                lastObservation = System.nanoTime();
                try {
                    var snapshot = io.github.recrivenvi.glass.client.GlassClientState.snapshot();
                    java.util.List<Map<String, Object>> views = new java.util.ArrayList<>();
                    for (int x : new int[] {0, 40}) {
                        BlockPos pos = new BlockPos(x, 200, 0);
                        var state = client.level.getBlockState(pos);
                        var network =
                                snapshot.networks().stream()
                                        .filter(n -> n.projector().pos().equals(pos))
                                        .findFirst();
                        views.add(
                                Map.of(
                                        "x",
                                        x,
                                        "powered",
                                        state.is(GlassContent.PROJECTOR.get())
                                                && state.getValue(GlassProjectorBlock.POWERED),
                                        "active",
                                        network.map(GlassStateSnapshot.Network::active)
                                                .orElse(false),
                                        "surfaces",
                                        network.map(n -> n.surfaces().size()).orElse(0)));
                    }
                    Files.writeString(
                            Path.of(System.getProperty("validation.result"))
                                    .resolve("view-state.json"),
                            new GsonBuilder()
                                    .create()
                                    .toJson(
                                            Map.of(
                                                    "graphics",
                                                    client.options.graphicsMode().get().name(),
                                                    "views",
                                                    views)));
                } catch (java.io.IOException error) {
                    throw new java.io.UncheckedIOException(error);
                }
            }
            return;
        }
        scheduled = true;
        var server = client.getSingleplayerServer();
        server.execute(
                () -> {
                    try {
                        ServerLevel level = server.overworld();
                        GlassSavedState saved = GlassServerState.saved(server);
                        for (int x = -5; x <= 65; x++)
                            for (int z = -8; z <= -3; z++)
                                level.setBlockAndUpdate(
                                        new BlockPos(x, 198, z), Blocks.STONE.defaultBlockState());
                        for (int x : new int[] {0, 40}) {
                            BlockPos projector = new BlockPos(x, 200, 0);
                            BlockPos terminal = projector.offset(16, 0, 16);
                            for (int u = -2; u <= 2; u++)
                                for (int v = -2; v <= 2; v++) {
                                    BlockPos p = projector.offset(u, v, 0);
                                    boolean frame = Math.abs(u) == 2 || Math.abs(v) == 2;
                                    level.setBlockAndUpdate(
                                            p,
                                            (frame
                                                    ? Blocks.STONE.defaultBlockState()
                                                    : GlassContent.EXTENSION
                                                            .get()
                                                            .defaultBlockState()));
                                    level.setBlockAndUpdate(
                                            p.south(), Blocks.MAGENTA_CONCRETE.defaultBlockState());
                                }
                            level.setBlockAndUpdate(
                                    projector.south(), Blocks.AIR.defaultBlockState());
                            level.setBlockAndUpdate(
                                    projector,
                                    GlassContent.PROJECTOR
                                            .get()
                                            .defaultBlockState()
                                            .setValue(GlassProjectorBlock.FACING, Direction.NORTH));
                            level.setBlockAndUpdate(
                                    projector.south(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                            level.setBlockAndUpdate(
                                    terminal,
                                    GlassContent.TERMINAL
                                            .get()
                                            .defaultBlockState()
                                            .setValue(GlassChannelBlock.FACING, Direction.NORTH));
                            for (int u = -5; u <= 5; u++)
                                for (int v = -5; v <= 5; v++)
                                    level.setBlockAndUpdate(
                                            terminal.offset(u, v, 6),
                                            (u < 0 ? Blocks.LIME_CONCRETE : Blocks.RED_CONCRETE)
                                                    .defaultBlockState());
                            var p = GlobalPos.of(level.dimension(), projector);
                            var t = GlobalPos.of(level.dimension(), terminal);
                            UUID channel =
                                    UUID.nameUUIDFromBytes(
                                            ("overlay-200-" + x)
                                                    .getBytes(
                                                            java.nio.charset.StandardCharsets
                                                                    .UTF_8));
                            saved.putTerminal(
                                    new GlassStateSnapshot.Terminal(
                                            t,
                                            new GlassStateSnapshot.Channel(
                                                    channel,
                                                    "Overlay " + x,
                                                    Optional.of(client.player.getUUID()),
                                                    true)));
                            List<GlobalPos> wireless = List.of();
                            if (x == 40) {
                                for (int u = -2; u <= 2; u++)
                                    for (int v = -2; v <= 2; v++) {
                                        if (u == 0 && v == 0) continue;
                                        level.setBlockAndUpdate(
                                                projector.offset(u, v, 0),
                                                Blocks.AIR.defaultBlockState());
                                        level.setBlockAndUpdate(
                                                projector.offset(u, v, 1),
                                                Blocks.AIR.defaultBlockState());
                                    }
                                wireless =
                                        List.of(
                                                GlobalPos.of(
                                                        level.dimension(),
                                                        projector.offset(-2, 2, 0)),
                                                GlobalPos.of(
                                                        level.dimension(),
                                                        projector.offset(2, 2, 0)),
                                                GlobalPos.of(
                                                        level.dimension(),
                                                        projector.offset(2, -2, 0)));
                                for (var node : wireless)
                                    level.setBlockAndUpdate(
                                            node.pos(),
                                            GlassContent.WIRELESS_EXTENSION
                                                    .get()
                                                    .defaultBlockState());
                            }
                            saved.putProjector(
                                    new GlassStateSnapshot.Projector(
                                            p, Optional.of(channel), wireless));
                        }
                        GlassServerState.rebuild(server);
                        Path output = Path.of(System.getProperty("validation.result"));
                        Files.createDirectories(output);
                        Files.writeString(
                                output.resolve("fixture.json"),
                                new GsonBuilder()
                                        .setPrettyPrinting()
                                        .create()
                                        .toJson(
                                                Map.of(
                                                        "wired",
                                                        List.of(.5, 198.88, -5.5, 0, 0),
                                                        "wireless",
                                                        List.of(40.5, 198.88, -5.5, 0, 0))));
                    } catch (Exception error) {
                        throw new RuntimeException(error);
                    }
                });
    }
}
