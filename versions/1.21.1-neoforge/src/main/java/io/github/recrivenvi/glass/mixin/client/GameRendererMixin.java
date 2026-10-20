package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.GlassClient;
import io.github.recrivenvi.glass.model.GameRendererProjectionAccess;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements GameRendererProjectionAccess {
    @Unique private final Matrix4f glass$levelProjection = new Matrix4f();

    @Override
    public Matrix4fc glass$getLevelProjection() {
        return glass$levelProjection;
    }

    @Redirect(
            method = "renderLevel",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"))
    private void glass$captureAndComposite(
            LevelRenderer level,
            DeltaTracker delta,
            boolean outline,
            Camera camera,
            GameRenderer game,
            LightTexture light,
            Matrix4f view,
            Matrix4f projection) {
        glass$levelProjection.set(projection);
        GlassClient.VIEW_RENDERER.capture(delta);
        level.renderLevel(delta, outline, camera, game, light, view, projection);
    }
}
