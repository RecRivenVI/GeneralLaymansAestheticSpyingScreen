package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.GlassClient;
import io.github.recrivenvi.glass.model.GlassChannelEntityVisibility;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelExtractorMixin {
    @Redirect(
            method = {"allChanged", "update", "extractLevel", "extractVisibleEntities"},
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
                                    "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;shouldRender(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/culling/Frustum;DDD)Z"))
    private boolean glass$keepLocalPlayerVisible(
            EntityRenderDispatcher dispatcher,
            Entity entity,
            Frustum frustum,
            double x,
            double y,
            double z) {
        return GlassChannelEntityVisibility.isVisibleForExtraction(
                dispatcher.shouldRender(entity, frustum, x, y, z),
                entity == Minecraft.getInstance().player,
                GlassClient.VIEW_RENDERER.isChannelExtraction());
    }

    @Redirect(
            method = "extractVisibleEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z"))
    private boolean glass$includeFirstPersonPlayer(Camera camera) {
        return GlassChannelEntityVisibility.isDetachedForExtraction(
                camera.isDetached(), GlassClient.VIEW_RENDERER.isChannelExtraction());
    }
}
