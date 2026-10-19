package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.client.render.ShaderManagerConfigAccess;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin implements ShaderManagerConfigAccess {
    @Unique private ShaderManager.Configs glass$configs = ShaderManager.Configs.EMPTY;

    @Inject(
            method =
                    "apply(Lnet/minecraft/client/renderer/ShaderManager$Configs;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"))
    private void glass$rememberConfigs(
            ShaderManager.Configs configs,
            ResourceManager resourceManager,
            ProfilerFiller profiler,
            CallbackInfo callback) {
        glass$configs = configs;
    }

    @Override
    public ShaderManager.Configs glass$getConfigs() {
        return glass$configs;
    }
}
