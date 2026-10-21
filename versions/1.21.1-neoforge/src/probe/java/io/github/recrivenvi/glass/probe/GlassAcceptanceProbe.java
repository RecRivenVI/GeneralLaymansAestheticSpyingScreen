package io.github.recrivenvi.glass.probe;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.KeyMapping;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Opt-in motion controls for the three project-owned acceptance showcase worlds. */
final class GlassAcceptanceProbe {
    private final Map<UUID, Pose3d> bases = new LinkedHashMap<>();
    private final KeyMapping toggle =
            new KeyMapping("key.glass_probe.motion", 297, "key.categories.glass_probe");
    private MinecraftServer active;
    private boolean moving = true;

    GlassAcceptanceProbe(net.neoforged.bus.api.IEventBus bus) {
        bus.addListener(
                (net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) ->
                        event.register(toggle));
        NeoForge.EVENT_BUS.addListener(
                (ServerStartedEvent event) -> {
                    if (!event.getServer()
                            .getWorldData()
                            .getLevelName()
                            .startsWith("GLASS B sodium ")) return;
                    active = event.getServer();
                    bases.clear();
                    moving = true;
                    SubLevelPhysicsSystem.require(active.overworld()).setPaused(true);
                });
        NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.ClientTickEvent.Post event) -> {
                    while (toggle.consumeClick())
                        if (active != null) active.execute(() -> moving = !moving);
                });
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(
                (ServerStoppingEvent event) -> {
                    if (event.getServer() != active) return;
                    for (var ship :
                            SubLevelContainer.getContainer(active.overworld()).getAllSubLevels())
                        if (bases.containsKey(ship.getUniqueId()))
                            GlassSableProbe.move(
                                    ship, bases.get(ship.getUniqueId()), 0, 0, 0, 0, 0, 0, false);
                    active = null;
                    bases.clear();
                });
    }

    private void tick(ServerTickEvent.Post event) {
        if (event.getServer() != active) return;
        double t = active.overworld().getGameTime() * .025;
        for (var ship : SubLevelContainer.getContainer(active.overworld()).getAllSubLevels()) {
            var base =
                    bases.computeIfAbsent(ship.getUniqueId(), id -> new Pose3d(ship.logicalPose()));
            if (moving)
                GlassSableProbe.move(
                        ship,
                        base,
                        Math.sin(t) * .5,
                        Math.sin(t * .6) * .2,
                        Math.cos(t) * .35,
                        Math.sin(t * .7) * .15,
                        Math.sin(t * .8) * .25,
                        Math.cos(t * .8) * .12,
                        true);
        }
        if (active.getTickCount() % 10 == 0 && System.getProperty("validation.result") != null) {
            try {
                var out = java.nio.file.Path.of(System.getProperty("validation.result"));
                java.nio.file.Files.createDirectories(out);
                java.nio.file.Files.writeString(
                        out.resolve("demo-state.json"),
                        new com.google.gson.Gson()
                                .toJson(
                                        Map.of(
                                                "moving",
                                                moving,
                                                "ships",
                                                SubLevelContainer.getContainer(active.overworld())
                                                        .getAllSubLevels()
                                                        .stream()
                                                        .map(
                                                                ship ->
                                                                        Map.of(
                                                                                "id",
                                                                                ship.getUniqueId()
                                                                                        .toString(),
                                                                                "pose",
                                                                                ship.logicalPose()
                                                                                        .toString()))
                                                        .toList())));
            } catch (java.io.IOException error) {
                throw new java.io.UncheckedIOException(error);
            }
        }
    }
}
