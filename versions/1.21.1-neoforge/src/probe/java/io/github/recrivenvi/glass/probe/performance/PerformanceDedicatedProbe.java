package io.github.recrivenvi.glass.probe.performance;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "glass_probe", dist = Dist.DEDICATED_SERVER)
public final class PerformanceDedicatedProbe {
    private Path output;
    private final com.google.gson.Gson json =
            new com.google.gson.GsonBuilder().setPrettyPrinting().create();
    private ServerPlayer fake;
    private GlobalPos projector;
    private long deadline, started;
    private String label;
    private boolean arranged, second;
    private Object entityStart;

    public PerformanceDedicatedProbe() {
        if (!Boolean.getBoolean("glass.performance")) return;
        output =
                Path.of(System.getProperty("validation.result"))
                        .resolve(
                                System.getenv()
                                        .getOrDefault(
                                                "GLASS_PERFORMANCE_SERVER_CASE", "network-server"));
        NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.server.ServerStartedEvent event) -> {
                    try {
                        var field =
                                net.minecraft.server.network.ServerConnectionListener.class
                                        .getDeclaredField("channels");
                        field.setAccessible(true);
                        var channels = (List<?>) field.get(event.getServer().getConnection());
                        var address =
                                (java.net.InetSocketAddress)
                                        ((io.netty.channel.ChannelFuture) channels.getFirst())
                                                .channel()
                                                .localAddress();
                        write(
                                "server-ready.json",
                                Map.of(
                                        "pid",
                                        ProcessHandle.current().pid(),
                                        "port",
                                        address.getPort()));
                    } catch (ReflectiveOperationException error) {
                        throw new IllegalStateException(error);
                    }
                });
        NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent
                                event) -> {
                    var player = (ServerPlayer) event.getEntity();
                    PerformanceWire.install(player.connection.getConnection(), player.getUUID());
                    player.serverLevel().getServer().getPlayerList().op(player.getGameProfile());
                    if (!arranged) {
                        arranged = true;
                        PerformanceScene.arrange(player.serverLevel().getServer(), "far");
                        write("fixture-ready.json", Map.of("ready", true));
                    }
                });
        NeoForge.EVENT_BUS.addListener(this::tick);
    }

    private void write(String file, Object value) {
        try {
            Files.createDirectories(output);
            Files.writeString(output.resolve(file), json.toJson(value));
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    }

    private void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        var server = event.getServer();
        if (second && server.getTickCount() % 20 == 0)
            io.github.recrivenvi.glass.remote.GlassRemoteSubscriptions.watch(
                    fake, Optional.of(projector));
        if (PerformanceCounters.measuring && System.nanoTime() >= deadline) {
            PerformanceCounters.measuring = false;
            var report = new java.util.LinkedHashMap<String, Object>();
            report.put("label", label);
            report.put("startedAtMillis", started);
            report.put("endedAtMillis", System.currentTimeMillis());
            report.put("ticks", List.copyOf(PerformanceCounters.tickSamples));
            report.put("tickCpuNanos", List.copyOf(PerformanceCounters.tickCpuSamples));
            report.put("wireSent", counts(PerformanceWire.SENT));
            report.put("wireReceived", counts(PerformanceWire.RECEIVED));
            report.put("encodedGlassPayload", counts(PerformanceWire.PAYLOAD));
            report.put("secondIsFakePlayer", second);
            report.put("entitiesBefore", entityStart);
            report.put("entitiesAfter", PerformanceScene.entityTicks());
            report.put(
                    "sources",
                    io.github.recrivenvi.glass.remote.GlassRemoteSubscriptions.activeSources(
                            server));
            report.put(
                    "fullChunks",
                    io.github.recrivenvi.glass.remote.GlassRemoteSubscriptions.heldSourceChunks(
                            server));
            write(label + ".json", report);
            write("done.json", Map.of("label", label));
        }
        if (server.getTickCount() % 5 != 0) return;
        var task = output.resolve("command.json");
        try {
            if (!Files.exists(task) || Files.size(task) == 0) return;
            JsonObject spec = JsonParser.parseString(Files.readString(task)).getAsJsonObject();
            Files.writeString(task, "");
            switch (spec.get("action").getAsString()) {
                case "set" -> {
                    PerformanceScene.power(server, spec.get("networks").getAsInt());
                    boolean requested = spec.has("second") && spec.get("second").getAsBoolean();
                    if (requested != second) second(server, requested);
                    write("ack.json", spec);
                }
                case "measure" -> {
                    PerformanceCounters.reset();
                    PerformanceWire.reset();
                    entityStart = PerformanceScene.entityTicks();
                    label = spec.get("label").getAsString();
                    started = System.currentTimeMillis();
                    deadline = System.nanoTime() + spec.get("seconds").getAsLong() * 1_000_000_000L;
                    PerformanceCounters.measuring = true;
                    write("ack.json", spec);
                }
                case "stop" -> server.halt(false);
                default -> throw new IllegalArgumentException(spec.toString());
            }
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    }

    private Object counts(java.util.Map<UUID, java.util.concurrent.atomic.LongAdder> source) {
        var result = new java.util.LinkedHashMap<String, Long>();
        source.forEach((key, value) -> result.put(key.toString(), value.sum()));
        return result;
    }

    @SuppressWarnings("unchecked")
    private void second(MinecraftServer server, boolean enabled) {
        try {
            var field =
                    net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
            field.setAccessible(true);
            var players = (Map<UUID, ServerPlayer>) field.get(server.getPlayerList());
            if (enabled) {
                fake =
                        net.neoforged.neoforge.common.util.FakePlayerFactory.get(
                                server.overworld(),
                                new com.mojang.authlib.GameProfile(
                                        UUID.nameUUIDFromBytes(
                                                "glass-perf-second"
                                                        .getBytes(
                                                                java.nio.charset.StandardCharsets
                                                                        .UTF_8)),
                                        "GlassPerfPeer"));
                fake.setPos(.5, 78.88, -15.5);
                players.put(fake.getUUID(), fake);
                projector = GlobalPos.of(server.overworld().dimension(), new BlockPos(-9, 80, 0));
                io.github.recrivenvi.glass.remote.GlassRemoteSubscriptions.watch(
                        fake, Optional.of(projector));
            } else {
                io.github.recrivenvi.glass.remote.GlassRemoteSubscriptions.watch(
                        fake, Optional.empty());
                players.remove(fake.getUUID());
            }
            second = enabled;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }
}
