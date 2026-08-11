package me.ichun.mods.glass;

import me.ichun.mods.glass.client.GlassClientInteraction;
import me.ichun.mods.glass.client.GlassClientState;
import me.ichun.mods.glass.client.GlassRenderSnapshots;
import me.ichun.mods.glass.client.render.GlassViewRenderer;
import me.ichun.mods.glass.client.render.VulkanBlaze3DViewRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

public final class GlassClient implements ClientModInitializer {
    public static final GlassViewRenderer VIEW_RENDERER = new VulkanBlaze3DViewRenderer();

    @Override
    public void onInitializeClient() {
        GlassClientInteraction.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(
                client -> VIEW_RENDERER.replaceViews(GlassRenderSnapshots.current(client)));
        LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(VIEW_RENDERER::drawProjector);
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    GlassClientState.clear();
                    VIEW_RENDERER.reset();
                });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> VIEW_RENDERER.close());
        GlassMod.LOGGER.info("Initializing GLASS Fabric 26.2 client");
    }
}
