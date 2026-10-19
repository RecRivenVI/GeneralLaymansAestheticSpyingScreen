package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.GlassClient;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
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
