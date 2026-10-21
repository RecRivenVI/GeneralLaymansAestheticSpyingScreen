package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.GlassClient;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.util.thread.ProcessorMailbox;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SectionRenderDispatcher.class)
public abstract class SectionRenderDispatcherMixin {
    @Shadow @Final @Mutable private SectionBufferBuilderPack fixedBuffers;
    @Shadow @Final @Mutable private SectionBufferBuilderPool bufferPool;
    @Shadow @Final private ProcessorMailbox<Runnable> mailbox;
    @Shadow private volatile boolean closed;
    @Unique private boolean glass$ownsBuffers;
    @Unique private boolean glass$cleanupScheduled;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void glass$ownChannelCompileBuffers(CallbackInfo ci) {
        if (!GlassClient.VIEW_RENDERER.isChannelExtraction()
                && io.github.recrivenvi.glass.client.render.GlassCaptureWorld.current() == null)
            return;
        // Vanilla's mailbox assumes one dispatcher consumes this pool. Sharing the main pool
        // starves channel queues and races acquire() against another renderer in Fabulous.
        fixedBuffers = new SectionBufferBuilderPack();
        bufferPool = SectionBufferBuilderPool.allocate(1);
        glass$ownsBuffers = true;
    }

    @Redirect(
            method = "lambda$runTask$2",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/SectionBufferBuilderPool;release(Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)V"))
    private void glass$releaseChannelCompileBuffer(
            SectionBufferBuilderPool pool, SectionBufferBuilderPack pack) {
        if (glass$ownsBuffers && closed) pack.close();
        else pool.release(pack);
    }

    @Inject(method = "dispose", at = @At("RETURN"))
    private void glass$retireChannelCompileBuffers(CallbackInfo ci) {
        if (!glass$ownsBuffers || glass$cleanupScheduled) return;
        glass$cleanupScheduled = true;
        // Serialize free-buffer cleanup with runTask; borrowed packs close when returned.
        mailbox.tell(
                () -> {
                    SectionBufferBuilderPack pack;
                    while ((pack = bufferPool.acquire()) != null) pack.close();
                    fixedBuffers.close();
                });
    }
}
