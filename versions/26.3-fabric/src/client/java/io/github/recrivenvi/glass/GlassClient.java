package io.github.recrivenvi.glass;

import io.github.recrivenvi.glass.client.GlassClientInteraction;
import io.github.recrivenvi.glass.client.GlassClientState;
import io.github.recrivenvi.glass.client.GlassRenderSnapshots;
import io.github.recrivenvi.glass.client.render.GlassViewRenderer;
import io.github.recrivenvi.glass.client.render.VulkanBlaze3DViewRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public final class GlassClient implements ClientModInitializer {
    public static final GlassViewRenderer VIEW_RENDERER = new VulkanBlaze3DViewRenderer();

    @Override
    public void onInitializeClient() {
        GlassClientInteraction.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(
                client -> {
                    io.github.recrivenvi.glass.client.render.GlassBlockInteriors.tick();
                    VIEW_RENDERER.replaceViews(GlassRenderSnapshots.current(client));
                });
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> {
                    GlassClientState.clear();
                    VIEW_RENDERER.reset();
                });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> VIEW_RENDERER.close());
        GlassMod.LOGGER.info("Initializing GLASS Fabric 26.3 client");
    }
}
