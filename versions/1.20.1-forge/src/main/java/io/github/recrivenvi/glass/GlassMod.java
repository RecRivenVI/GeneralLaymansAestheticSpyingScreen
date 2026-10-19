package io.github.recrivenvi.glass;

import io.github.recrivenvi.glass.config.GlassConfig;
import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.network.GlassNetworking;
import io.github.recrivenvi.glass.state.GlassServerState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(GlassMod.MOD_ID)
public final class GlassMod {
    public static final String MOD_ID = "glass";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public GlassMod() {
        IEventBus bus =
                net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        GlassConfig.get().load();
        GlassContent.initialize(bus);
        GlassNetworking.initialize();
        MinecraftForge.EVENT_BUS.addListener(
                (ServerStartedEvent event) -> GlassServerState.initialize(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener(
                (ServerStoppingEvent event) -> GlassServerState.stop(event.getServer()));
        MinecraftForge.EVENT_BUS.addListener(
                (ChunkEvent.Load event) -> {
                    if (event.getLevel() instanceof ServerLevel level)
                        GlassServerState.chunkLoaded(level, event.getChunk().getPos());
                });
        MinecraftForge.EVENT_BUS.addListener(
                (ChunkEvent.Unload event) -> {
                    if (event.getLevel() instanceof ServerLevel level)
                        GlassServerState.chunkUnloaded(level, event.getChunk().getPos());
                });
        MinecraftForge.EVENT_BUS.addListener(
                (TickEvent.ServerTickEvent event) -> {
                    if (event.phase == TickEvent.Phase.END)
                        GlassServerState.tick(event.getServer());
                });
        MinecraftForge.EVENT_BUS.addListener(
                (PlayerEvent.PlayerLoggedInEvent event) -> {
                    if (event.getEntity() instanceof ServerPlayer player)
                        GlassServerState.sync(player);
                });
        LOGGER.info("Initializing GLASS Forge 1.20.1");
    }
}
