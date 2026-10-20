package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.GlassClient;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final
    private net.minecraft.client.renderer.state.level.LevelRenderState levelRenderState;

    @org.spongepowered.asm.mixin.Unique
    private boolean glass$isMainRenderer() {
        return (Object) this == net.minecraft.client.Minecraft.getInstance().levelRenderer;
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "render", at = @At("HEAD"))
    private void glass$prepareOverlay(
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (glass$isMainRenderer()) GlassClient.VIEW_RENDERER.prepareProjector(levelRenderState);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "executeSolid", at = @At("RETURN"))
    private void glass$drawOverlay(
            net.minecraft.client.renderer.chunk.ChunkSectionsToRender chunks,
            net.minecraft.client.renderer.feature.FeatureRenderDispatcher.PreparedFrame frame,
            com.mojang.renderpearl.api.commands.RenderPass pass,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (glass$isMainRenderer()) GlassClient.VIEW_RENDERER.drawProjector(pass);
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "render", at = @At("RETURN"))
    private void glass$endOverlay(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (glass$isMainRenderer()) GlassClient.VIEW_RENDERER.endProjectorFrame();
    }

    @org.spongepowered.asm.mixin.injection.Inject(method = "submitFeatures", at = @At("RETURN"))
    private void glass$submitInteriors(
            net.minecraft.client.renderer.state.level.LevelRenderState state,
            net.minecraft.client.renderer.SubmitNodeCollector collector,
            boolean outlines,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        io.github.recrivenvi.glass.client.render.GlassBlockInteriors.submit(state, collector);
    }

    @Redirect(
            method = "invalidateCompiledGeometry",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/Options;getEffectiveRenderDistance()I"))
    private int glass$useChannelRenderDistance(Options options) {
        return GlassClient.VIEW_RENDERER.isChannelExtraction()
                ? io.github.recrivenvi.glass.client.GlassClientState.snapshot()
                        .settings()
                        .channelRenderDistance()
                : options.getEffectiveRenderDistance();
    }
}
