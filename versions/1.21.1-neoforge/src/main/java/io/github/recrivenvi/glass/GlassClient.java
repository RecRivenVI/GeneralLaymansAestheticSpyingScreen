package io.github.recrivenvi.glass;

import io.github.recrivenvi.glass.client.GlassClientInteraction;
import io.github.recrivenvi.glass.client.GlassClientState;
import io.github.recrivenvi.glass.client.GlassRenderSnapshots;
import io.github.recrivenvi.glass.client.render.Blaze3DViewRenderer;
import io.github.recrivenvi.glass.client.render.GlassViewRenderer;
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
    public static final GlassViewRenderer VIEW_RENDERER = new Blaze3DViewRenderer();

    public GlassClient(IEventBus bus, net.neoforged.fml.ModContainer container) {
        container.registerExtensionPoint(
                net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (net.neoforged.neoforge.client.gui.IConfigScreenFactory)
                        (mod, parent) ->
                                new io.github.recrivenvi.glass.client.gui.GlassConfigScreen(
                                        parent));
        GlassClientInteraction.initialize();
        bus.addListener(Blaze3DViewRenderer::registerShaders);
        NeoForge.EVENT_BUS.addListener(
                (ClientTickEvent.Post event) -> {
                    io.github.recrivenvi.glass.client.render.GlassRemoteViews.tick();
                    io.github.recrivenvi.glass.client.render.GlassBlockInteriors.tick();
                    VIEW_RENDERER.replaceViews(
                            GlassRenderSnapshots.current(Minecraft.getInstance()));
                });
        NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.client.event.RenderLevelStageEvent event) -> {
                    if (event.getStage()
                            != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage
                                    .AFTER_CUTOUT_BLOCKS) return;
                    io.github.recrivenvi.glass.client.render.GlassBlockInteriors.draw(event);
                    if (!VIEW_RENDERER.isChannelExtraction())
                        VIEW_RENDERER.drawProjector(
                                event.getModelViewMatrix(),
                                event.getCamera().getPosition(),
                                event.getFrustum());
                });
        NeoForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut event) -> {
                    io.github.recrivenvi.glass.client.render.GlassRemoteViews.clear();
                    GlassClientState.clear();
                    VIEW_RENDERER.reset();
                });
        NeoForge.EVENT_BUS.addListener(
                (GameShuttingDownEvent event) -> {
                    VIEW_RENDERER.close();
                    io.github.recrivenvi.glass.client.render.GlassRemoteViews.clear();
                    io.github.recrivenvi.glass.client.gui.GlassCursor.close();
                });
        bus.addListener(
                (net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) ->
                        event.registerReloadListener(
                                (net.minecraft.server.packs.resources.ResourceManagerReloadListener)
                                        manager -> {
                                            VIEW_RENDERER.reset();
                                            io.github.recrivenvi.glass.client.render
                                                    .GlassRemoteViews.releaseAndClear();
                                        }));
        GlassMod.LOGGER.info("Initializing GLASS NeoForge 1.21.1 client");
    }
}
