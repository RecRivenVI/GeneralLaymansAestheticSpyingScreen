package io.github.recrivenvi.glass.mixin.client;

import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {
    @Accessor("resourcePool")
    CrossFrameResourcePool glass$getResourcePool();

    @Accessor("fogRenderer")
    FogRenderer glass$getFogRenderer();

    @Accessor("levelProjectionMatrixBuffer")
    ProjectionMatrixBuffer glass$getLevelProjectionMatrixBuffer();
}
