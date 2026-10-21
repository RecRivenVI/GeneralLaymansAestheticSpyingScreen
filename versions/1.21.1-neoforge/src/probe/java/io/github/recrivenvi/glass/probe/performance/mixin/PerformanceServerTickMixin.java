package io.github.recrivenvi.glass.probe.performance.mixin;

import io.github.recrivenvi.glass.probe.performance.PerformanceCounters;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class PerformanceServerTickMixin {
    @Unique private long glassperf$start;
    @Unique private long glassperf$cpu;

    @Unique
    private static final java.lang.management.ThreadMXBean GLASSPERF_CPU =
            java.lang.management.ManagementFactory.getThreadMXBean();

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void glassperf$start(CallbackInfo ci) {
        glassperf$start = System.nanoTime();
        glassperf$cpu = GLASSPERF_CPU.getCurrentThreadCpuTime();
    }

    @Inject(method = "tickServer", at = @At("RETURN"))
    private void glassperf$end(CallbackInfo ci) {
        PerformanceCounters.tick(
                System.nanoTime() - glassperf$start,
                GLASSPERF_CPU.getCurrentThreadCpuTime() - glassperf$cpu);
    }
}
