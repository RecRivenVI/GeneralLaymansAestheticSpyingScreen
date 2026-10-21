package io.github.recrivenvi.glass.probe;

import java.nio.file.Files;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Native tick hold records the same two-tick animation phase after the source becomes ready. */
final class GlassAnimationProbe {
    private volatile String pending;
    private volatile boolean scheduled;
    private long freezeAt, resumeAt;
    private int sequence;

    GlassAnimationProbe() {
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
    }

    private void clientTick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        var server = client.getSingleplayerServer();
        if (client.level == null || server == null) return;
        try {
            var request = GlassOverlayProbe.output().resolve("animation-request.txt");
            if (pending == null && Files.exists(request)) {
                String value = Files.readString(request).strip();
                if (!value.isEmpty()) {
                    Files.writeString(request, "");
                    pending = value;
                    scheduled = false;
                    server.execute(
                            () ->
                                    server.overworld()
                                            .setBlockAndUpdate(
                                                    new BlockPos(0, 200, 1),
                                                    (value.equals("open")
                                                                    ? Blocks.REDSTONE_BLOCK
                                                                    : Blocks.STONE)
                                                            .defaultBlockState()));
                }
            }
            if (pending != null && !scheduled) {
                boolean target = pending.equals("open");
                boolean active =
                        io.github.recrivenvi.glass.client.GlassRenderSnapshots.current(client)
                                .stream()
                                .anyMatch(
                                        state ->
                                                state.projector
                                                        && state.network
                                                                .pos()
                                                                .equals(new BlockPos(0, 200, 0))
                                                        && state.active);
                if (active == target) {
                    scheduled = true;
                    server.execute(
                            () -> {
                                freezeAt = server.overworld().getGameTime() + 2;
                                resumeAt = 0;
                            });
                }
            }
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    }

    private void serverTick(ServerTickEvent.Post event) {
        if (pending == null || !scheduled || freezeAt == 0) return;
        var server = event.getServer();
        long tick = server.overworld().getGameTime();
        try {
            if (resumeAt == 0 && tick >= freezeAt) {
                server.tickRateManager().setFrozen(true);
                resumeAt = System.nanoTime() + 8_000_000_000L;
                sequence++;
                Files.writeString(
                        GlassOverlayProbe.output().resolve("animation-phase-" + sequence + ".json"),
                        new com.google.gson.Gson()
                                .toJson(
                                        Map.of(
                                                "action",
                                                pending,
                                                "worldTick",
                                                tick,
                                                "holdUntilMillis",
                                                System.currentTimeMillis() + 8000,
                                                "kind",
                                                "PROBE_NATIVE_TICK_HOLD_AFTER_VIEW_READY")));
            } else if (resumeAt != 0 && System.nanoTime() >= resumeAt) {
                server.tickRateManager().setFrozen(false);
                freezeAt = 0;
                pending = null;
                scheduled = false;
                Files.writeString(
                        GlassOverlayProbe.output()
                                .resolve("animation-resume-" + sequence + ".json"),
                        "{\"frozen\":false}");
            }
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    }
}
