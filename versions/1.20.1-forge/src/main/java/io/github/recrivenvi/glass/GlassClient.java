package io.github.recrivenvi.glass;

import io.github.recrivenvi.glass.client.GlassClientInteraction;
import io.github.recrivenvi.glass.client.GlassClientState;
import io.github.recrivenvi.glass.client.GlassRenderSnapshots;
import io.github.recrivenvi.glass.client.gui.GlassConfigScreen;
import io.github.recrivenvi.glass.client.gui.GlassCursor;
import io.github.recrivenvi.glass.client.render.Blaze3DViewRenderer;
import io.github.recrivenvi.glass.client.render.GlassViewRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(
        modid = GlassMod.MOD_ID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GlassClient {
    public static final GlassViewRenderer VIEW_RENDERER = new Blaze3DViewRenderer();

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        GlassClientInteraction.initialize();
        net.minecraftforge.fml.ModLoadingContext.get()
                .registerExtensionPoint(
                        net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class,
                        () ->
                                new net.minecraftforge.client.ConfigScreenHandler
                                        .ConfigScreenFactory(
                                        (client, parent) -> new GlassConfigScreen(parent)));
        MinecraftForge.EVENT_BUS.addListener(
                (TickEvent.ClientTickEvent tick) -> {
                    if (tick.phase == TickEvent.Phase.END)
                        VIEW_RENDERER.replaceViews(
                                GlassRenderSnapshots.current(Minecraft.getInstance()));
                });
        MinecraftForge.EVENT_BUS.addListener(
                (ClientPlayerNetworkEvent.LoggingOut logout) -> {
                    GlassClientState.clear();
                    VIEW_RENDERER.reset();
                    GlassCursor.reset();
                });
        MinecraftForge.EVENT_BUS.addListener(
                (GameShuttingDownEvent shutdown) -> {
                    VIEW_RENDERER.close();
                    GlassCursor.close();
                });
        GlassMod.LOGGER.info("Initializing GLASS Forge 1.20.1 client");
    }

    @SubscribeEvent
    public static void shaders(RegisterShadersEvent event) {
        Blaze3DViewRenderer.registerShaders(event);
    }

    @SubscribeEvent
    public static void reload(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(
                (ResourceManagerReloadListener) manager -> VIEW_RENDERER.reset());
    }
}
