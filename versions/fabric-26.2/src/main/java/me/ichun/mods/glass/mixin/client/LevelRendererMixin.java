package me.ichun.mods.glass.mixin.client;

import me.ichun.mods.glass.GlassClient;
import me.ichun.mods.glass.client.render.GlassChannelFramePolicy;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.PostChain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
                ? me.ichun.mods.glass.client.GlassClientState.snapshot()
                        .settings()
                        .channelRenderDistance()
                : options.getEffectiveRenderDistance();
    }

    @Inject(method = "getTransparencyChain", at = @At("HEAD"), cancellable = true)
    private void glass$avoidSharedChannelPostChain(CallbackInfoReturnable<PostChain> callback) {
        if (!GlassChannelFramePolicy.mayUseSharedPostChains(
                GlassClient.VIEW_RENDERER.isChannelExtraction())) {
            // 原版 ShaderManager 只提供一个透明后处理链；频道必须改用各自的链和环形缓冲。
            callback.setReturnValue(GlassChannelFramePolicy.currentChannelPostChain());
        }
    }
}
