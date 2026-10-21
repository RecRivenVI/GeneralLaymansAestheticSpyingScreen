package io.github.recrivenvi.glass.probe.performance.mixin;

import io.github.recrivenvi.glass.probe.performance.PerformanceClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "io.github.recrivenvi.glass.client.render.Blaze3DViewRenderer", remap = false)
public abstract class PerformanceStagesMixin {
    @Inject(method = "capture", at = @At("HEAD"))
    private void glassperf$captureStart(CallbackInfo ci) {
        PerformanceClient.stageStart(0);
    }

    @Inject(method = "capture", at = @At("RETURN"))
    private void glassperf$captureEnd(CallbackInfo ci) {
        PerformanceClient.stageEnd(0);
    }

    @Inject(method = "drawProjector", at = @At("HEAD"))
    private void glassperf$drawStart(CallbackInfo ci) {
        PerformanceClient.stageStart(1);
    }

    @Inject(method = "drawProjector", at = @At("RETURN"))
    private void glassperf$drawEnd(CallbackInfo ci) {
        PerformanceClient.stageEnd(1);
    }
}
