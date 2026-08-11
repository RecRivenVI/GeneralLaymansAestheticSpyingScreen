package me.ichun.mods.glass.mixin.client;

import me.ichun.mods.glass.GlassClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.extract.LevelExtractor;
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
                                    "Lnet/minecraft/client/renderer/extract/LevelExtractor;setSectionDirty(III)V"))
    private void glass$forwardBiomeSectionDirty(
            LevelExtractor extractor, int sectionX, int sectionY, int sectionZ) {
        extractor.setSectionDirty(sectionX, sectionY, sectionZ);
        GlassClient.VIEW_RENDERER.setSectionDirty(sectionX, sectionY, sectionZ);
    }
}
