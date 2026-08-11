package me.ichun.mods.glass;

import me.ichun.mods.glass.config.GlassConfig;
import me.ichun.mods.glass.content.GlassContent;
import me.ichun.mods.glass.network.GlassNetworking;
import me.ichun.mods.glass.state.GlassServerState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlassMod implements ModInitializer {
    public static final String MOD_ID = "generallaymansaestheticspyingscreen";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        GlassConfig.get().load();
        GlassContent.initialize();
        GlassNetworking.initialize();
        ServerLifecycleEvents.SERVER_STARTED.register(GlassServerState::initialize);
        ServerLifecycleEvents.SERVER_STOPPING.register(GlassServerState::stop);
        ServerChunkEvents.CHUNK_LOAD.register(
                (level, chunk, generated) -> GlassServerState.chunkLoaded(level, chunk.getPos()));
        ServerChunkEvents.CHUNK_UNLOAD.register(
                (level, chunk) -> GlassServerState.chunkUnloaded(level, chunk.getPos()));
        ServerTickEvents.END_SERVER_TICK.register(GlassServerState::tick);
        LOGGER.info("Initializing GLASS Fabric 26.2");
    }
}
