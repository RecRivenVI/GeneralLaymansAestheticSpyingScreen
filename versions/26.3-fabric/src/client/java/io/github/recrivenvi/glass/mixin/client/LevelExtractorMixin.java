package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.GlassClient;
import io.github.recrivenvi.glass.model.GlassChannelEntityVisibility;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
    @Redirect(
            method = {"extract", "extractVisibleEntities", "allChanged"},
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

    @Redirect(
            method = "extractVisibleEntities",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/extract/LevelExtractor;isEntityVisible(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDDFJ)Z"))
    private boolean glass$keepLocalPlayerVisible(
            LevelExtractor extractor,
            Entity entity,
            Frustum frustum,
            double cameraX,
            double cameraY,
            double cameraZ,
            float entityPartialTicks,
            long chunkFadeDuration) {
        return GlassChannelEntityVisibility.isVisibleForExtraction(
                extractor.isEntityVisible(
                        entity,
                        frustum,
                        cameraX,
                        cameraY,
                        cameraZ,
                        entityPartialTicks,
                        chunkFadeDuration),
                entity == Minecraft.getInstance().player,
                GlassClient.VIEW_RENDERER.isChannelExtraction());
    }

    @Redirect(
            method = "extract",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/multiplayer/ClientChunkCache;flipUpdateTrackingSets()V"))
    private void glass$preserveMainChunkUpdates(ClientChunkCache chunkCache) {
        if (!GlassClient.VIEW_RENDERER.isChannelExtraction()) {
            chunkCache.flipUpdateTrackingSets();
        }
    }

    @Redirect(
            method = "extractVisibleEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z"))
    private boolean glass$includeFirstPersonPlayer(Camera camera) {
        return GlassChannelEntityVisibility.isDetachedForExtraction(
                camera.isDetached(), GlassClient.VIEW_RENDERER.isChannelExtraction());
    }
}
