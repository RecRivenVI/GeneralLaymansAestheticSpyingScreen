package io.github.recrivenvi.glass.probe.performance.mixin;

import io.github.recrivenvi.glass.probe.performance.PerformanceClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class PerformanceFrameMixin {
    @Inject(method = "runTick", at = @At("HEAD"))
    private void glassperf$start(CallbackInfo ci) {
        PerformanceClient.frameStart();
    }

    @Inject(method = "runTick", at = @At("RETURN"))
    private void glassperf$end(CallbackInfo ci) {
        PerformanceClient.frameEnd();
    }
}
