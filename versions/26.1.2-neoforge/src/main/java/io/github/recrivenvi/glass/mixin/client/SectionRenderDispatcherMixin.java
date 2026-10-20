package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.GlassClient;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.class)
public abstract class SectionRenderDispatcherMixin {
    @Shadow @Final @Mutable private SectionBufferBuilderPack fixedBuffers;
    @Shadow @Final @Mutable private SectionBufferBuilderPool bufferPool;
    @Unique private boolean glass$ownsBuffers;
    @Unique private boolean glass$buffersClosed;
    @Unique private final ReentrantLock glass$compileLock = new ReentrantLock();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void glass$ownChannelCompileBuffers(CallbackInfo ci) {
        if (!GlassClient.VIEW_RENDERER.isChannelExtraction()) return;
        fixedBuffers = new SectionBufferBuilderPack();
        bufferPool = SectionBufferBuilderPool.allocate(1);
        glass$ownsBuffers = true;
    }

    @Inject(
            method = {"runTask", "dispose"},
            at = @At("HEAD"))
    private void glass$serializeChannelCompileAndClose(CallbackInfo ci) {
        // This version requeues a task on an empty pool without scheduling another attempt.
        // One private pack with serialized native tasks avoids starving the last channel task.
        if (glass$ownsBuffers) glass$compileLock.lock();
    }

    @Inject(method = "runTask", at = @At("RETURN"))
    private void glass$finishChannelCompile(CallbackInfo ci) {
        if (glass$ownsBuffers) glass$compileLock.unlock();
    }

    @Inject(method = "dispose", at = @At("RETURN"))
    private void glass$closeChannelCompileBuffers(CallbackInfo ci) {
        if (!glass$ownsBuffers) return;
        try {
            if (glass$buffersClosed) return;
            glass$buffersClosed = true;
            SectionBufferBuilderPack pack;
            while ((pack = bufferPool.acquire()) != null) pack.close();
            fixedBuffers.close();
        } finally {
            glass$compileLock.unlock();
        }
    }
}
