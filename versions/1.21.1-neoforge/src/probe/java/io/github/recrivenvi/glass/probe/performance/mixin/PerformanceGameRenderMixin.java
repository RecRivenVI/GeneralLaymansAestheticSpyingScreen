package io.github.recrivenvi.glass.probe.performance.mixin;

import io.github.recrivenvi.glass.probe.performance.PerformanceClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class PerformanceGameRenderMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void glassperf$start(CallbackInfo ci) {
        PerformanceClient.gpuFrameStart();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void glassperf$end(CallbackInfo ci) {
        PerformanceClient.gpuFrameEnd();
    }
}
