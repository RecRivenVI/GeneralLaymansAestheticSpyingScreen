package io.github.recrivenvi.glass;

import io.github.recrivenvi.glass.config.GlassConfig;
import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.network.GlassNetworking;
import io.github.recrivenvi.glass.state.GlassServerState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(GlassMod.MOD_ID)
public final class GlassMod {
    public static final String MOD_ID = "glass";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public GlassMod(IEventBus bus) {
        GlassConfig.get().load();
        GlassContent.initialize(bus);
        bus.addListener(GlassNetworking::initialize);
        NeoForge.EVENT_BUS.addListener(
                (ServerStartedEvent event) -> GlassServerState.initialize(event.getServer()));
        NeoForge.EVENT_BUS.addListener(
                (ServerStoppingEvent event) -> GlassServerState.stop(event.getServer()));
        NeoForge.EVENT_BUS.addListener(
                (ChunkEvent.Load event) -> {
                    if (event.getLevel() instanceof ServerLevel level)
                        GlassServerState.chunkLoaded(level, event.getChunk().getPos());
                });
        NeoForge.EVENT_BUS.addListener(
                (ChunkEvent.Unload event) -> {
                    if (event.getLevel() instanceof ServerLevel level)
                        GlassServerState.chunkUnloaded(level, event.getChunk().getPos());
                });
        NeoForge.EVENT_BUS.addListener(
                (ServerTickEvent.Post event) -> GlassServerState.tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener(
                (PlayerEvent.PlayerLoggedInEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player)
                        GlassServerState.sync(player);
                });
        LOGGER.info("Initializing GLASS NeoForge 26.1.2");
    }
}
