package me.ichun.mods.glass.mixin.client;

import net.minecraft.client.renderer.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CloudRenderer.class)
public interface CloudRendererAccessor {
    @Accessor("texture")
    CloudRenderer.TextureData glass$getTexture();

    @Accessor("texture")
    void glass$setTexture(CloudRenderer.TextureData texture);
}
