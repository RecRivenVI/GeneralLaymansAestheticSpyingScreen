package io.github.recrivenvi.glass.probe;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** 默认动画短于截图往返的最坏耗时；只在验证中用原版 tick 冻结固定中间帧。 */
final class GlassRenderReviewClock {
    private final MinecraftServer server;
    private final Map<String, BlockPos> powers;
    private final Map<String, Boolean> previous = new HashMap<>();
    private final Path output;
    private String pending;
    private boolean powered;
    private long freezeAt;
    private long resumeAt;
    private int sequence;

    GlassRenderReviewClock(MinecraftServer server, Map<String, BlockPos> powers, Path output) {
        this.server = server;
        this.powers = Map.copyOf(powers);
        this.output = output;
        NeoForge.EVENT_BUS.addListener(this::tick);
    }

    private void tick(ServerTickEvent.Post event) {
        if (event.getServer() != server) return;
        long tick = server.overworld().getGameTime();
        if (pending == null) {
            for (var entry : powers.entrySet()) {
                if (!server.overworld().hasChunkAt(entry.getValue())) {
                    previous.remove(entry.getKey());
                    continue;
                }
                boolean active =
                        server.overworld()
                                .getBlockState(entry.getValue())
                                .is(Blocks.REDSTONE_BLOCK);
                Boolean old = previous.put(entry.getKey(), active);
                if (old != null && active != old) {
                    pending = entry.getKey();
                    powered = active;
                    freezeAt = tick + 2;
                    resumeAt = 0;
                    break;
                }
            }
        } else if (resumeAt == 0 && tick >= freezeAt) {
            server.tickRateManager().setFrozen(true);
            resumeAt = System.nanoTime() + 10_000_000_000L;
            sequence++;
            write(
                    "phase-" + sequence,
                    Map.of(
                            "sequence",
                            sequence,
                            "scene",
                            pending,
                            "powered",
                            powered,
                            "worldTick",
                            tick,
                            "holdUntilMillis",
                            System.currentTimeMillis() + 10_000,
                            "kind",
                            "PROBE_NATIVE_TICK_HOLD"));
        } else if (resumeAt != 0 && System.nanoTime() >= resumeAt) {
            server.tickRateManager().setFrozen(false);
            write(
                    "resume-" + sequence,
                    Map.of("sequence", sequence, "worldTick", tick, "frozen", false));
            pending = null;
        }
    }

    private void write(String name, Object data) {
        try {
            Path temporary = output.resolve(name + ".tmp");
            Files.writeString(
                    temporary, new GsonBuilder().setPrettyPrinting().create().toJson(data) + "\n");
            Files.move(temporary, output.resolve(name + ".json"), StandardCopyOption.ATOMIC_MOVE);
        } catch (java.io.IOException exception) {
            throw new RuntimeException("Could not record held fixture phase", exception);
        }
    }
}
