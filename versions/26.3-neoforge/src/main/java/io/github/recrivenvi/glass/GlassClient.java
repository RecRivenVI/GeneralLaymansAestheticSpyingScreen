package io.github.recrivenvi.glass;

import io.github.recrivenvi.glass.client.GlassClientInteraction;
import io.github.recrivenvi.glass.client.GlassClientState;
import io.github.recrivenvi.glass.client.GlassRenderSnapshots;
import io.github.recrivenvi.glass.client.render.GlassViewRenderer;
import io.github.recrivenvi.glass.client.render.VulkanBlaze3DViewRenderer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

@Mod(value = GlassMod.MOD_ID, dist = Dist.CLIENT)
public final class GlassClient {
    public static final GlassViewRenderer VIEW_RENDERER = new VulkanBlaze3DViewRenderer();

    public GlassClient(IEventBus bus, net.neoforged.fml.ModContainer container) {
        container.registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (net.neoforged.neoforge.client.gui.IConfigScreenFactory)
                        (mod, parent) ->
                                new io.github.recrivenvi.glass.client.gui.GlassConfigScreen(
                                        parent));
        bus.addListener(GlassClientInteraction::initialize);
        bus.addListener(VulkanBlaze3DViewRenderer::registerPipelines);
        NeoForge.EVENT_BUS.addListener(
                (ClientTickEvent.Post event) -> {
                    io.github.recrivenvi.glass.client.render.GlassBlockInteriors.tick();
                    VIEW_RENDERER.replaceViews(
                            GlassRenderSnapshots.current(Minecraft.getInstance()));
                });
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> {
                    GlassClientState.clear();
                    VIEW_RENDERER.reset();
                });
        NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent event) -> VIEW_RENDERER.close());
        GlassMod.LOGGER.info("Initializing GLASS NeoForge 26.3 client");
    }
}
