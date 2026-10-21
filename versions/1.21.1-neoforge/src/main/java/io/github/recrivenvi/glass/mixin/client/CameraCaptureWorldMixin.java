package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.client.render.GlassCaptureWorld;
import net.minecraft.client.Camera;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Camera.class)
public abstract class CameraCaptureWorldMixin {
    @Shadow private BlockGetter level;

    @Redirect(
            method = {"getFluidInCamera", "getBlockAtCamera"},
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/client/Camera;level:Lnet/minecraft/world/level/BlockGetter;",
                            opcode = 180))
    private BlockGetter glass$sourceFluidAndBlock(Camera camera) {
        var scope = GlassCaptureWorld.current();
        return scope == null ? level : scope.level();
    }
}
