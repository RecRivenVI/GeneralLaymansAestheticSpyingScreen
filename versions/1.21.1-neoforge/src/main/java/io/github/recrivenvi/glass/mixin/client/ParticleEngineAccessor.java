package io.github.recrivenvi.glass.mixin.client;

import java.util.Map;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ParticleEngine.class)
public interface ParticleEngineAccessor {
    @Accessor("textureAtlas")
    TextureAtlas glass$atlas();

    @Accessor("providers")
    Map<ResourceLocation, ParticleProvider<?>> glass$providers();
}
