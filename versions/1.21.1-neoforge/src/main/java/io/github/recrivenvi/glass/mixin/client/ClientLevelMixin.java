package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.GlassClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "shouldTickDeath", at = @At("HEAD"), cancellable = true)
    private void glass$sourceDeath(
            net.minecraft.world.entity.Entity entity,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean>
                    callback) {
        var scope = io.github.recrivenvi.glass.client.render.GlassCaptureWorld.current();
        // The native check measures against the local player's chunk, which is in another view.
        if (scope != null && scope.level() == (Object) this) callback.setReturnValue(true);
    }

    @Inject(method = "sendBlockUpdated", at = @At("TAIL"))
    private void glass$forwardBlockChanged(
            BlockPos pos,
            BlockState oldState,
            BlockState newState,
            int updateFlags,
            CallbackInfo callback) {
        GlassClient.VIEW_RENDERER.blockChanged(pos, updateFlags);
    }

    @Inject(method = "setBlocksDirty", at = @At("TAIL"))
    private void glass$forwardBlockStateChanged(
            BlockPos pos, BlockState oldState, BlockState newState, CallbackInfo callback) {
        GlassClient.VIEW_RENDERER.blockStateChanged(pos, oldState, newState);
        io.github.recrivenvi.glass.client.render.GlassBlockInteriors.blockChanged(
                (ClientLevel) (Object) this, pos, newState);
    }

    @Inject(method = "setSectionDirtyWithNeighbors", at = @At("TAIL"))
    private void glass$forwardSectionDirtyWithNeighbors(
            int sectionX, int sectionY, int sectionZ, CallbackInfo callback) {
        GlassClient.VIEW_RENDERER.setSectionDirtyWithNeighbors(sectionX, sectionY, sectionZ);
    }

    @Inject(method = "onChunkLoaded", at = @At("TAIL"))
    private void glass$forwardChunkLoaded(
            net.minecraft.world.level.ChunkPos pos, CallbackInfo callback) {
        GlassClient.VIEW_RENDERER.chunkLoaded(pos);
    }
}
