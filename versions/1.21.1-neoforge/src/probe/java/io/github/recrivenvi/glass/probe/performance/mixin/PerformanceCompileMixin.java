package io.github.recrivenvi.glass.probe.performance.mixin;

import io.github.recrivenvi.glass.probe.performance.PerformanceCounters;
import io.github.recrivenvi.glass.probe.performance.PerformanceRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SectionCompiler.class)
public abstract class PerformanceCompileMixin {
    @Unique private static final ThreadLocal<Long> GLASSPERF_START = new ThreadLocal<>();
    @Unique private static final ThreadLocal<Boolean> GLASSPERF_SOURCE = new ThreadLocal<>();

    @Inject(
            method =
                    "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
            at = @At("HEAD"))
    private void glassperf$start(
            net.minecraft.core.SectionPos pos,
            net.minecraft.client.renderer.chunk.RenderChunkRegion region,
            com.mojang.blaze3d.vertex.VertexSorting sorting,
            net.minecraft.client.renderer.SectionBufferBuilderPack pack,
            java.util.List<?> extras,
            CallbackInfoReturnable<?> ci) {
        GLASSPERF_START.set(System.nanoTime());
        GLASSPERF_SOURCE.set(
                ((PerformanceRegion) region).glassperf$level()
                        != net.minecraft.client.Minecraft.getInstance().level);
    }

    @Inject(
            method =
                    "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderChunkRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;Ljava/util/List;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;",
            at = @At("RETURN"))
    private void glassperf$end(CallbackInfoReturnable<?> ci) {
        if (PerformanceCounters.measuring) {
            PerformanceCounters.compileNanos.add(System.nanoTime() - GLASSPERF_START.get());
            PerformanceCounters.compileCount.increment();
            if (GLASSPERF_SOURCE.get()) PerformanceCounters.sourceCompiles.increment();
            else PerformanceCounters.mainCompiles.increment();
        }
        GLASSPERF_START.remove();
        GLASSPERF_SOURCE.remove();
    }
}
