package io.github.recrivenvi.glass.probe.performance.mixin;

import io.github.recrivenvi.glass.probe.performance.PerformanceTimeline;
import java.util.Set;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "io.github.recrivenvi.glass.client.render.GlassRemoteViews$View", remap = false)
public abstract class PerformanceViewTimelineMixin {
    @Shadow @Final private Set<Long> received;
    @Shadow private int expectedChunks;
    @Unique private int glassperf$shape;
    @Unique private boolean glassperf$ready;

    @Inject(method = "synchronizeTime", at = @At("HEAD"))
    private void glassperf$shape(net.minecraft.nbt.CompoundTag header, CallbackInfo ci) {
        int shape = java.util.Arrays.hashCode(header.getLongArray("sourceChunks"));
        if (shape == glassperf$shape) return;
        glassperf$shape = shape;
        glassperf$ready = false;
        PerformanceTimeline.record(
                "source-shape",
                java.util.Map.of("hash", shape, "chunks", header.getLongArray("sourceChunks")));
    }

    @Inject(method = "ready", at = @At("RETURN"))
    private void glassperf$ready(CallbackInfoReturnable<Boolean> ci) {
        if (glassperf$ready || received.size() < expectedChunks) return;
        glassperf$ready = true;
        PerformanceTimeline.record(
                "snapshot-ready",
                java.util.Map.of(
                        "hash",
                        glassperf$shape,
                        "received",
                        received.size(),
                        "expected",
                        expectedChunks));
    }
}
