package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.GlassClient;
import io.github.recrivenvi.glass.model.GameRendererProjectionAccess;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements GameRendererProjectionAccess {
    @Unique private final Matrix4f glass$levelProjection = new Matrix4f();

    @ModifyArg(
            method = "renderLevel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;",
                            ordinal = 0),
            index = 0)
    private Matrix4f glass$captureLevelProjection(Matrix4f projection) {
        glass$levelProjection.set(projection);
        return projection;
    }

    @Override
    public Matrix4fc glass$getLevelProjection() {
        return glass$levelProjection;
    }

    @Inject(
            method = "renderLevel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lcom/mojang/blaze3d/systems/RenderSystem;setProjectionMatrix(Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lcom/mojang/blaze3d/ProjectionType;)V",
                            ordinal = 0,
                            shift = At.Shift.AFTER))
    private void glass$captureChannel(CallbackInfo callback) {
        GlassClient.VIEW_RENDERER.capture(
                net.minecraft.client.Minecraft.getInstance().getDeltaTracker());
    }

    @Inject(
            method = "renderLevel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;Lorg/joml/Vector4f;ZZ)V",
                            shift = At.Shift.AFTER))
    private void glass$drawProjector(CallbackInfo callback) {
        GlassClient.VIEW_RENDERER.drawProjector(
                ((GameRenderer) (Object) this).gameRenderState().levelRenderState);
    }
}
