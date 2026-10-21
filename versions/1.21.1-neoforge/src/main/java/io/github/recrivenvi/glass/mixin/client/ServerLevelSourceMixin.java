package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.remote.GlassRemoteSubscriptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelSourceMixin {
    @Inject(method = "levelEvent", at = @At("TAIL"))
    private void glass$levelEvent(
            net.minecraft.world.entity.player.Player excluded,
            int type,
            BlockPos pos,
            int data,
            CallbackInfo ci) {
        GlassRemoteSubscriptions.visualEvent(
                (ServerLevel) (Object) this,
                pos,
                new net.minecraft.network.protocol.game.ClientboundLevelEventPacket(
                        type, pos, data, false));
    }

    @Inject(method = "destroyBlockProgress", at = @At("TAIL"))
    private void glass$breaking(int breaker, BlockPos pos, int progress, CallbackInfo ci) {
        GlassRemoteSubscriptions.visualEvent(
                (ServerLevel) (Object) this,
                pos,
                new net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket(
                        breaker, pos, progress));
    }

    @Inject(method = "sendBlockUpdated", at = @At("TAIL"))
    private void glass$sourceChanged(
            BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        GlassRemoteSubscriptions.blockChanged((ServerLevel) (Object) this, pos);
    }

    @Inject(method = "doBlockEvent", at = @At("RETURN"))
    private void glass$blockEvent(
            net.minecraft.world.level.BlockEventData event,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValueZ())
            GlassRemoteSubscriptions.visualEvent(
                    (ServerLevel) (Object) this,
                    event.pos(),
                    new net.minecraft.network.protocol.game.ClientboundBlockEventPacket(
                            event.pos(), event.block(), event.paramA(), event.paramB()));
    }

    @Inject(
            method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
            at = @At("HEAD"))
    private <T extends net.minecraft.core.particles.ParticleOptions> void glass$particles(
            T particle,
            double x,
            double y,
            double z,
            int count,
            double dx,
            double dy,
            double dz,
            double speed,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> ci) {
        GlassRemoteSubscriptions.visualEvent(
                (ServerLevel) (Object) this,
                BlockPos.containing(x, y, z),
                new net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket(
                        particle,
                        false,
                        x,
                        y,
                        z,
                        (float) dx,
                        (float) dy,
                        (float) dz,
                        (float) speed,
                        count));
    }
}
