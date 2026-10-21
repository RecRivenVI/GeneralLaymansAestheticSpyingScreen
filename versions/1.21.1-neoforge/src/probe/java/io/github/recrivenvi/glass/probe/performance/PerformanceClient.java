package io.github.recrivenvi.glass.probe.performance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.opengl.GL33;

/** Enabled only in the independent probe jar; neither measured product jar is changed. */
public final class PerformanceClient {
    private static final java.lang.management.ThreadMXBean CPU =
            ManagementFactory.getThreadMXBean();
    private static final Path OUTPUT = Path.of(System.getProperty("validation.result"));
    private static final com.google.gson.Gson JSON =
            new com.google.gson.GsonBuilder().setPrettyPrinting().create();
    private static final Frame[] PENDING = new Frame[64];
    private static final int[][] QUERIES = new int[64][6];
    private static final List<Frame> ROWS = new ArrayList<>();
    private static Frame current;
    private static long end, beganMs, lastFrameStart, lastCommandTick = -1;
    private static String label;
    private static boolean gpu = true, initialized, arranged;
    private static int next, missed;

    public PerformanceClient() {
        if (Boolean.getBoolean("glass.performance.sable"))
            io.github.recrivenvi.glass.probe.PerformanceSableMotion.register();
        NeoForge.EVENT_BUS.addListener(this::tick);
    }

    public static void write(String name, Object data) {
        try {
            Files.createDirectories(OUTPUT);
            Files.writeString(OUTPUT.resolve(name), JSON.toJson(data));
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    }

    private void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        if (!arranged) {
            arranged = true;
            client.options.enableVsync().set(false);
            client.options.framerateLimit().set(260);
            client.getWindow().updateVsync(false);
            if (!Boolean.getBoolean("glass.performance.existing")
                    && client.getSingleplayerServer() != null)
                client.getSingleplayerServer()
                        .execute(
                                () -> {
                                    PerformanceScene.arrange(
                                            client.getSingleplayerServer(), "near");
                                    write("ready.json", Map.of("ready", true));
                                });
            else write("ready.json", Map.of("ready", true));
        }
        if (client.level.getGameTime() / 10 == lastCommandTick) return;
        lastCommandTick = client.level.getGameTime() / 10;
        Path command = OUTPUT.resolve("command.json");
        try {
            if (!Files.exists(command) || Files.size(command) == 0) return;
            JsonObject spec = JsonParser.parseString(Files.readString(command)).getAsJsonObject();
            Files.writeString(command, "");
            switch (spec.get("action").getAsString()) {
                case "status" -> {
                    write("status.json", resources());
                    write("ack.json", spec);
                }
                case "motion" ->
                        client.getSingleplayerServer()
                                .execute(
                                        () -> {
                                            io.github.recrivenvi.glass.probe.PerformanceSableMotion
                                                    .motion(spec.get("enabled").getAsBoolean());
                                            write("ack.json", spec);
                                        });
                case "set" ->
                        client.getSingleplayerServer()
                                .execute(
                                        () -> {
                                            if (spec.has("place"))
                                                PerformanceScene.arrange(
                                                        client.getSingleplayerServer(),
                                                        spec.get("place").getAsString());
                                            PerformanceScene.power(
                                                    client.getSingleplayerServer(),
                                                    spec.get("networks").getAsInt());
                                            write("ack.json", spec);
                                        });
                case "measure" -> {
                    if (PerformanceCounters.measuring)
                        throw new IllegalStateException("window still active");
                    label = spec.get("label").getAsString();
                    gpu = !spec.has("gpu") || spec.get("gpu").getAsBoolean();
                    ROWS.clear();
                    missed = 0;
                    PerformanceCounters.reset();
                    beganMs = System.currentTimeMillis();
                    end = System.nanoTime() + spec.get("seconds").getAsLong() * 1_000_000_000L;
                    PerformanceCounters.measuring = true;
                    write("ack.json", spec);
                }
                default -> throw new IllegalArgumentException(spec.toString());
            }
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    }

    public static void frameStart() {
        drain();
        if (!PerformanceCounters.measuring) return;
        if (!initialized && gpu) {
            for (int[] ids : QUERIES)
                for (int i = 0; i < ids.length; i++) ids[i] = GL33.glGenQueries();
            initialized = true;
        }
        current = new Frame();
        current.start = System.nanoTime();
        current.intervalNs = lastFrameStart == 0 ? 0 : current.start - lastFrameStart;
        lastFrameStart = current.start;
        current.cpu = CPU.getCurrentThreadCpuTime();
        if (gpu && PENDING[next] == null) {
            current.slot = next;
            next = (next + 1) % 64;
        } else {
            current.slot = -1;
            if (gpu) missed++;
        }
    }

    public static void stageStart(int stage) {
        if (current == null) return;
        current.starts[stage] = System.nanoTime();
        current.cpus[stage] = CPU.getCurrentThreadCpuTime();
        timestamp(stage == 0 ? 1 : 3);
    }

    public static void gpuFrameStart() {
        if (current != null) timestamp(0);
    }

    public static void gpuFrameEnd() {
        if (current != null) timestamp(5);
    }

    public static void stageEnd(int stage) {
        if (current == null) return;
        current.wall[stage] += System.nanoTime() - current.starts[stage];
        current.thread[stage] += CPU.getCurrentThreadCpuTime() - current.cpus[stage];
        timestamp(stage == 0 ? 2 : 4);
    }

    private static void timestamp(int index) {
        if (current.slot < 0) return;
        GL33.glQueryCounter(QUERIES[current.slot][index], GL33.GL_TIMESTAMP);
        current.written[index] = true;
    }

    public static void frameEnd() {
        if (current == null) return;
        Frame frame = current;
        frame.wallNs = System.nanoTime() - frame.start;
        frame.cpuNs = CPU.getCurrentThreadCpuTime() - frame.cpu;
        if (frame.slot >= 0) {
            for (int i = 1; i < 5; i++) if (!frame.written[i]) timestamp(i);
            if (!frame.written[5]) timestamp(5);
            PENDING[frame.slot] = frame;
        }
        ROWS.add(frame);
        current = null;
        if (System.nanoTime() >= end) {
            PerformanceCounters.measuring = false;
            finish();
        }
    }

    private static void drain() {
        if (!initialized) return;
        for (int slot = 0; slot < 64; slot++) {
            var frame = PENDING[slot];
            if (frame == null
                    || GL33.glGetQueryObjecti(QUERIES[slot][5], GL33.GL_QUERY_RESULT_AVAILABLE)
                            == 0) continue;
            long[] t = new long[6];
            for (int i = 0; i < 6; i++)
                t[i] = GL33.glGetQueryObjectui64(QUERIES[slot][i], GL33.GL_QUERY_RESULT);
            frame.gpuNs = t[5] - t[0];
            frame.captureGpu = t[2] - t[1];
            frame.drawGpu = t[4] - t[3];
            PENDING[slot] = null;
        }
    }

    private static void finish() {
        drain();
        StringBuilder csv =
                new StringBuilder(
                        "frame_interval_ns,frame_wall_ns,frame_thread_ns,capture_wall_ns,capture_thread_ns,draw_wall_ns,draw_thread_ns,gpu_span_ns,capture_gpu_span_ns,draw_gpu_span_ns\n");
        for (Frame f : ROWS)
            csv.append(f.intervalNs)
                    .append(',')
                    .append(f.wallNs)
                    .append(',')
                    .append(f.cpuNs)
                    .append(',')
                    .append(f.wall[0])
                    .append(',')
                    .append(f.thread[0])
                    .append(',')
                    .append(f.wall[1])
                    .append(',')
                    .append(f.thread[1])
                    .append(',')
                    .append(f.gpuNs)
                    .append(',')
                    .append(f.captureGpu)
                    .append(',')
                    .append(f.drawGpu)
                    .append('\n');
        try {
            Files.writeString(OUTPUT.resolve(label + ".csv"), csv);
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
        var memory = ManagementFactory.getMemoryMXBean();
        var summary = new java.util.LinkedHashMap<String, Object>();
        summary.put("label", label);
        summary.put("startedAtMillis", beganMs);
        summary.put("endedAtMillis", System.currentTimeMillis());
        summary.put("frames", ROWS.size());
        summary.put("gpuQueries", gpu);
        summary.put("gpuRingMisses", missed);
        summary.put("heapUsed", memory.getHeapMemoryUsage().getUsed());
        summary.put("heapCommitted", memory.getHeapMemoryUsage().getCommitted());
        summary.put("nonHeapUsed", memory.getNonHeapMemoryUsage().getUsed());
        summary.put(
                "buffers",
                ManagementFactory.getPlatformMXBeans(java.lang.management.BufferPoolMXBean.class)
                        .stream()
                        .map(b -> Map.of("name", b.getName(), "bytes", b.getMemoryUsed()))
                        .toList());
        summary.put("nativeCompileCount", PerformanceCounters.compileCount.sum());
        summary.put("nativeSourceCompileCount", PerformanceCounters.sourceCompiles.sum());
        summary.put("nativeMainCompileCount", PerformanceCounters.mainCompiles.sum());
        summary.put("sodiumCompileCount", PerformanceCounters.sodiumCompiles.sum());
        summary.put("sodiumSourceCompileCount", PerformanceCounters.sodiumSourceCompiles.sum());
        summary.put("sodiumCompileNanos", PerformanceCounters.sodiumCompileNanos.sum());
        summary.put("nativeCompileNanos", PerformanceCounters.compileNanos.sum());
        summary.put("serverTickNanos", List.copyOf(PerformanceCounters.tickSamples));
        summary.put("resources", resources());
        summary.put("timeline", List.copyOf(PerformanceTimeline.EVENTS));
        write(label + ".json", summary);
        write("done.json", Map.of("label", label));
    }

    private static Object resources() {
        try {
            Object renderer = io.github.recrivenvi.glass.GlassClient.VIEW_RENDERER;
            var field = renderer.getClass().getDeclaredField("channelContexts");
            field.setAccessible(true);
            var map = (Map<?, ?>) field.get(renderer);
            var worlds = new IdentityHashMap<Object, Boolean>();
            int targets = 0;
            for (Object context : map.values()) {
                var level = context.getClass().getDeclaredField("level");
                level.setAccessible(true);
                worlds.put(level.get(context), true);
                var target = context.getClass().getDeclaredField("target");
                target.setAccessible(true);
                if (target.get(context) != null) targets++;
            }
            var result = new java.util.LinkedHashMap<String, Object>();
            result.put("contexts", map.size());
            result.put("worlds", worlds.size());
            result.put("targets", targets);
            if (System.getProperty("glass.performance.route", "B").equals("B")) {
                var viewsField =
                        io.github.recrivenvi.glass.client.render.GlassRemoteViews.class
                                .getDeclaredField("VIEWS");
                viewsField.setAccessible(true);
                var views = (Map<?, ?>) viewsField.get(null);
                var terminals = new ArrayList<String>();
                for (Object source : views.values()) {
                    var terminal = source.getClass().getDeclaredField("terminal");
                    terminal.setAccessible(true);
                    terminals.add(terminal.get(source).toString());
                }
                result.put("sourceTerminals", terminals);
            }
            if (System.getProperty("glass.performance.route", "B").equals("B")
                    && Minecraft.getInstance().getSingleplayerServer() != null) {
                var server = Minecraft.getInstance().getSingleplayerServer();
                result.put(
                        "sources",
                        io.github.recrivenvi.glass.remote.GlassRemoteSubscriptions.activeSources(
                                server));
                result.put(
                        "sourceChunks",
                        io.github.recrivenvi.glass.remote.GlassRemoteSubscriptions
                                .streamedSourceChunks(server));
                result.put(
                        "fullChunks",
                        io.github.recrivenvi.glass.remote.GlassRemoteSubscriptions.heldSourceChunks(
                                server));
            }
            return result;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static final class Frame {
        long start, cpu, intervalNs, wallNs, cpuNs, gpuNs = -1, captureGpu = -1, drawGpu = -1;
        int slot = -1;
        final long[] starts = new long[2],
                cpus = new long[2],
                wall = new long[2],
                thread = new long[2];
        final boolean[] written = new boolean[6];
    }
}
