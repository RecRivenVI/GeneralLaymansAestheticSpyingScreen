package io.github.recrivenvi.glass.probe;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.recrivenvi.glass.GlassClient;
import io.github.recrivenvi.glass.client.GlassClientState;
import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.content.block.GlassChannelBlock;
import io.github.recrivenvi.glass.content.block.GlassProjectorBlock;
import io.github.recrivenvi.glass.network.SetProjectorChannelPayload;
import io.github.recrivenvi.glass.network.SetTerminalChannelPayload;
import io.github.recrivenvi.glass.state.GlassServerState;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

/** Runs only in this validation's own fresh integrated world. */
@Mod(value = "glass_probe", dist = Dist.CLIENT)
public final class GlassMigrationProbe {
    private static final BlockPos PROJECTOR = new BlockPos(0, 65, 0);
    private static final BlockPos TERMINAL = new BlockPos(4, 65, 0);
    private final Path result = Path.of(System.getProperty("validation.result"));
    private final Map<String, Object> evidence = new LinkedHashMap<>();
    private final long started = System.nanoTime();
    private int phase;
    private int ticks;
    private volatile boolean prepared;
    private volatile boolean captured;
    private boolean finished;
    private UUID channel;

    public GlassMigrationProbe() {
        evidence.put("target", System.getProperty("validation.target"));
        evidence.put("world", "glass_migration_" + System.currentTimeMillis());
        NeoForge.EVENT_BUS.addListener(this::tick);
        io.github.recrivenvi.glass.GlassMod.LOGGER.info("GLASS migration probe initialized");
        Thread watchdog =
                new Thread(
                        () -> {
                            try {
                                Thread.sleep(240_000);
                            } catch (InterruptedException ignored) {
                                return;
                            }
                            Minecraft.getInstance()
                                    .execute(
                                            () -> {
                                                if (!finished)
                                                    finish(
                                                            Minecraft.getInstance(),
                                                            new IllegalStateException(
                                                                    "Probe watchdog: "
                                                                            + Minecraft
                                                                                    .getInstance()
                                                                                    .gui
                                                                                    .screen()));
                                            });
                        },
                        "GLASS probe watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    private void tick(ClientTickEvent.Post event) {
        if (finished) return;
        Minecraft client = Minecraft.getInstance();
        try {
            if ((System.nanoTime() - started) / 1_000_000_000L > 240) {
                throw new IllegalStateException("Runtime validation timed out in phase " + phase);
            }
            if (phase == 0
                    && client.isGameLoadFinished()
                    && client.gui.screen() instanceof TitleScreen
                    && client.gui.overlay() == null) {
                client.options.getSoundSourceOptionInstance(SoundSource.MASTER).set(0.0);
                client.options.renderDistance().set(6);
                client.options.simulationDistance().set(5);
                client.options.pauseOnLostFocus = false;
                client.options.save();
                phase = 1;
                client.createWorldOpenFlows()
                        .createFreshLevel(
                                (String) evidence.get("world"),
                                new LevelSettings(
                                        "GLASS template migration",
                                        GameType.CREATIVE,
                                        new LevelSettings.DifficultySettings(
                                                Difficulty.PEACEFUL, false, false),
                                        true,
                                        WorldDataConfiguration.DEFAULT),
                                new WorldOptions(20261019L, false, false),
                                WorldPresets::createTestWorldDimensions,
                                client.gui.screen());
            }
            if (phase == 1
                    && client.player != null
                    && client.level != null
                    && client.getSingleplayerServer() != null) {
                phase = 2;
                client.getSingleplayerServer().execute(() -> prepare(client));
            }
            if (phase == 2 && prepared && ++ticks > 40) {
                ClientPacketDistributor.sendToServer(
                        new SetTerminalChannelPayload(
                                GlobalPos.of(Level.OVERWORLD, TERMINAL), "migration-smoke", true));
                phase = 3;
                ticks = 0;
            }
            if (phase == 3
                    && GlassClientState.snapshot().terminals().stream()
                            .anyMatch(t -> t.channel().name().equals("migration-smoke"))) {
                channel =
                        GlassClientState.snapshot().terminals().stream()
                                .filter(t -> t.pos().pos().equals(TERMINAL))
                                .findFirst()
                                .orElseThrow()
                                .channel()
                                .id();
                ClientPacketDistributor.sendToServer(
                        new SetProjectorChannelPayload(
                                GlobalPos.of(Level.OVERWORLD, PROJECTOR), channel));
                phase = 4;
                ticks = 0;
                client.gui.setScreen(null);
            }
            if (phase == 4 && ++ticks > 160 && !GlassClientState.snapshot().networks().isEmpty()) {
                evidence.put("packetRoundTrip", true);
                evidence.put("networks", GlassClientState.snapshot().networks().size());
                evidence.put(
                        "surfaces",
                        GlassClientState.snapshot().networks().get(0).surfaces().size());
                evidence.put("active", GlassClientState.snapshot().networks().get(0).active());
                evidence.put("backend", RenderSystem.getDevice().getDeviceInfo().backendName());
                evidence.put("width", client.getWindow().getWidth());
                evidence.put("height", client.getWindow().getHeight());
                evidence.put(
                        "masterVolume",
                        client.options.getFinalSoundSourceVolume(SoundSource.MASTER));
                capture(client, "wired");
                phase = 5;
                ticks = 0;
            }
            if (phase == 5 && captured && ++ticks > 30) {
                captured = false;
                client.getSingleplayerServer()
                        .execute(
                                () -> {
                                    var server = client.getSingleplayerServer();
                                    var level = server.overworld();
                                    List<GlobalPos> nodes =
                                            List.of(
                                                            new BlockPos(-2, 65, 0),
                                                            new BlockPos(-2, 68, 0),
                                                            new BlockPos(2, 68, 0))
                                                    .stream()
                                                    .map(
                                                            pos -> {
                                                                level.setBlockAndUpdate(
                                                                        pos,
                                                                        GlassContent
                                                                                .WIRELESS_EXTENSION
                                                                                .get()
                                                                                .defaultBlockState());
                                                                return GlobalPos.of(
                                                                        Level.OVERWORLD, pos);
                                                            })
                                                    .toList();
                                    var saved = GlassServerState.saved(server);
                                    saved.putProjector(
                                            new GlassStateSnapshot.Projector(
                                                    GlobalPos.of(Level.OVERWORLD, PROJECTOR),
                                                    Optional.of(channel),
                                                    nodes));
                                    GlassServerState.rebuild(server);
                                });
                phase = 6;
                ticks = 0;
            }
            if (phase == 6 && ++ticks > 160) {
                evidence.put(
                        "wirelessNodes",
                        GlassClientState.snapshot().networks().get(0).wirelessPositions().size());
                capture(client, "wireless");
                phase = 7;
                ticks = 0;
            }
            if (phase == 7 && captured && ++ticks > 30) finish(client, null);
        } catch (Throwable exception) {
            finish(client, exception);
        }
    }

    private void prepare(Minecraft client) {
        var server = client.getSingleplayerServer();
        var level = server.overworld();
        for (int x = -5; x <= 8; x++)
            for (int z = -3; z <= 12; z++) {
                level.setBlockAndUpdate(
                        new BlockPos(x, 64, z), Blocks.SMOOTH_STONE.defaultBlockState());
                for (int y = 65; y <= 70; y++)
                    level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
            }
        for (int x = 2; x <= 6; x++)
            for (int y = 65; y <= 68; y++)
                level.setBlockAndUpdate(
                        new BlockPos(x, y, -8),
                        ((x + y) % 2 == 0
                                        ? Blocks.CONCRETE.pick(
                                                net.minecraft.world.item.DyeColor.RED)
                                        : Blocks.CONCRETE.pick(
                                                net.minecraft.world.item.DyeColor.LIME))
                                .defaultBlockState());
        level.setBlockAndUpdate(
                TERMINAL,
                GlassContent.TERMINAL
                        .get()
                        .defaultBlockState()
                        .setValue(GlassChannelBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(
                PROJECTOR,
                GlassContent.PROJECTOR
                        .get()
                        .defaultBlockState()
                        .setValue(GlassProjectorBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(PROJECTOR.below(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        for (int x = -2; x <= 2; x++)
            if (x != 0)
                level.setBlockAndUpdate(
                        PROJECTOR.offset(x, 0, 0),
                        GlassContent.EXTENSION.get().defaultBlockState());
        GlassServerState.rebuild(server);
        var player = server.getPlayerList().getPlayers().get(0);
        player.teleportTo(level, .5, 65, 5.5, Set.of(), 180, 15, false);
        prepared = true;
    }

    private void capture(Minecraft client, String name) throws Exception {
        Files.createDirectories(result);
        var field = GlassClient.VIEW_RENDERER.getClass().getDeclaredField("channelContexts");
        field.setAccessible(true);
        Map<?, ?> contexts = (Map<?, ?>) field.get(GlassClient.VIEW_RENDERER);
        evidence.put(name + "Contexts", contexts.size());
        if (contexts.isEmpty()) throw new IllegalStateException("No live channel context");
        Object context = contexts.values().iterator().next();
        var targetField = context.getClass().getDeclaredField("target");
        targetField.setAccessible(true);
        RenderTarget target = (RenderTarget) targetField.get(context);
        if (target == null) throw new IllegalStateException("Channel framebuffer was not created");
        Screenshot.takeScreenshot(
                target,
                image -> {
                    try (image) {
                        image.writeToFile(result.resolve(name + "_channel.png"));
                        recordPixels(name + "Channel", image);
                    } catch (Exception e) {
                        evidence.put("imageError", e.toString());
                    }
                });
        Screenshot.takeScreenshot(
                client.gameRenderer.mainRenderTarget(),
                image -> {
                    try (image) {
                        image.writeToFile(result.resolve(name + "_main.png"));
                        recordPixels(name + "Main", image);
                        captured = true;
                    } catch (Exception e) {
                        evidence.put("imageError", e.toString());
                    }
                });
    }

    private void recordPixels(String name, com.mojang.blaze3d.platform.NativeImage image) {
        int red = 0;
        int green = 0;
        for (int pixel : image.getPixels()) {
            int r = (pixel >>> 16) & 255;
            int g = (pixel >>> 8) & 255;
            int b = pixel & 255;
            if (r > 60 && r > g * 1.8 && r > b * 1.5) red++;
            if (g > 60 && g > r * 1.4 && g > b * 1.4) green++;
        }
        evidence.put(name + "RedPixels", red);
        evidence.put(name + "GreenPixels", green);
    }

    private void finish(Minecraft client, Throwable error) {
        finished = true;
        evidence.put("phase", phase);
        evidence.put("status", error == null ? "PENDING_IMAGE_REVIEW" : "FAIL");
        if (error != null) {
            evidence.put("error", error.toString());
            error.printStackTrace();
        }
        try {
            Files.createDirectories(result);
            Files.writeString(
                    result.resolve("runtime.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(evidence) + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        client.stop();
    }
}
