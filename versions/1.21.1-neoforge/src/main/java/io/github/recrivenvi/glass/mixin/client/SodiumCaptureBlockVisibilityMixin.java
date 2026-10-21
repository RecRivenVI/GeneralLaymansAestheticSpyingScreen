package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.client.render.GlassChannelBlockVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice", remap = false)
public abstract class SodiumCaptureBlockVisibilityMixin {
    @Shadow @Final private ClientLevel level;

    @Inject(
            method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("RETURN"),
            cancellable = true)
    private void glass$sourceTerminal(
            int x, int y, int z, CallbackInfoReturnable<BlockState> callback) {
        // The slice keeps its world on worker threads, where the render-thread scope is absent.
        callback.setReturnValue(
                GlassChannelBlockVisibility.stateForSnapshot(
                        callback.getReturnValue(), level != Minecraft.getInstance().level));
    }
}
