package io.github.recrivenvi.glass.probe.performance.mixin;

import io.github.recrivenvi.glass.probe.performance.PerformanceCounters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets =
                "net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask",
        remap = false)
public abstract class PerformanceSodiumCompileMixin {
    @Unique private boolean glassperf$source;
    @Unique private long glassperf$start;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void glassperf$identity(CallbackInfo ci) {
        glassperf$source =
                io.github.recrivenvi.glass.GlassClient.VIEW_RENDERER.isChannelExtraction();
    }

    @Inject(
            method =
                    "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At("HEAD"))
    private void glassperf$start(CallbackInfoReturnable<?> ci) {
        glassperf$start = System.nanoTime();
    }

    @Inject(
            method =
                    "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
            at = @At("RETURN"))
    private void glassperf$end(CallbackInfoReturnable<?> ci) {
        if (!PerformanceCounters.measuring) return;
        PerformanceCounters.sodiumCompiles.increment();
        PerformanceCounters.sodiumCompileNanos.add(System.nanoTime() - glassperf$start);
        if (glassperf$source) PerformanceCounters.sodiumSourceCompiles.increment();
    }
}
