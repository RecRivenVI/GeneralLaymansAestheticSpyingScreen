package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.client.render.GlassCaptureWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Sodium replaces setupRender; its own section manager already follows the supplied camera. */
@Mixin(LevelRenderer.class)
public abstract class VanillaCaptureSectionsMixin {
    @Redirect(
            method = "setupRender",
            at =
                    @At(
                            value = "INVOKE",
                            target = "Lnet/minecraft/client/Options;getEffectiveRenderDistance()I"))
    private int glass$distance(Options options) {
        return GlassCaptureWorld.current() == null
                ? options.getEffectiveRenderDistance()
                : io.github.recrivenvi.glass.client.GlassClientState.snapshot()
                        .settings()
                        .channelRenderDistance();
    }

    @Redirect(
            method = {"allChanged", "setupRender"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getX()D"))
    private double glass$x(LocalPlayer player) {
        return GlassCaptureWorld.current() == null
                ? player.getX()
                : Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().x;
    }

    @Redirect(
            method = "setupRender",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getY()D"))
    private double glass$y(LocalPlayer player) {
        return GlassCaptureWorld.current() == null
                ? player.getY()
                : Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().y;
    }

    @Redirect(
            method = {"allChanged", "setupRender"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getZ()D"))
    private double glass$z(LocalPlayer player) {
        return GlassCaptureWorld.current() == null
                ? player.getZ()
                : Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().z;
    }
}
