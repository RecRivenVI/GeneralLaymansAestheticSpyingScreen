package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.GlassClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Redirect(
            method = "handleChunksBiomes",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/LevelRenderer;setSectionDirty(III)V"))
    private void glass$forwardBiomeSectionDirty(
            LevelRenderer extractor, int sectionX, int sectionY, int sectionZ) {
        extractor.setSectionDirty(sectionX, sectionY, sectionZ);
        GlassClient.VIEW_RENDERER.setSectionDirty(sectionX, sectionY, sectionZ);
    }
}
