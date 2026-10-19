package io.github.recrivenvi.glass;

import io.github.recrivenvi.glass.config.GlassConfig;
import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.network.GlassNetworking;
import io.github.recrivenvi.glass.state.GlassServerState;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlassMod implements ModInitializer {
    public static final String MOD_ID = "glass";
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
        LOGGER.info("Initializing GLASS Fabric 26.3");
    }
}
