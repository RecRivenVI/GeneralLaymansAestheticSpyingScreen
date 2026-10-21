package io.github.recrivenvi.glass.mixin.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.recrivenvi.glass.client.render.GlassCaptureWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "net.caffeinemc.mods.sodium.client.render.vertex.VertexConsumerUtils",
        remap = false)
public abstract class SodiumParticleSubmissionMixin {
    @Inject(
            method =
                    "convertOrLog(Lcom/mojang/blaze3d/vertex/VertexConsumer;Lcom/mojang/blaze3d/vertex/VertexFormat;)Lnet/caffeinemc/mods/sodium/api/vertex/buffer/VertexBufferWriter;",
            at = @At("HEAD"),
            cancellable = true)
    private static void glass$nativeSourceParticleSubmission(
            VertexConsumer consumer, VertexFormat format, CallbackInfoReturnable<Object> callback) {
        // The fast writer drops particles in the independent view. Its native null-writer
        // branch retains the original emitter, which also preserves Sable's vertex handling.
        if (GlassCaptureWorld.current() != null && format == DefaultVertexFormat.PARTICLE)
            callback.setReturnValue(null);
    }
}
