package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.client.render.GlassCaptureWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineAtlasMixin {
    @Redirect(
            method = "<init>",
            at = @At(value = "NEW", target = "net/minecraft/client/renderer/texture/TextureAtlas"))
    private TextureAtlas glass$shareLoadedParticleSprites(ResourceLocation id) {
        return GlassCaptureWorld.creatingParticles()
                ? ((ParticleEngineAccessor) Minecraft.getInstance().particleEngine).glass$atlas()
                : new TextureAtlas(id);
    }

    @Redirect(
            method = "<init>",
            at =
                    @At(
                            value = "INVOKE",
                            target =
                                    "Lnet/minecraft/client/renderer/texture/TextureManager;register(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/client/renderer/texture/AbstractTexture;)V"))
    private void glass$keepMainParticleAtlasOwnership(
            TextureManager manager, ResourceLocation id, AbstractTexture texture) {
        // Remote engines own queues only; re-registering an atlas would reload the main asset.
        if (!GlassCaptureWorld.creatingParticles()) manager.register(id, texture);
    }
}
