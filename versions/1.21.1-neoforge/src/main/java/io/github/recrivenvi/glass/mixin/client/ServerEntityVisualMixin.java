package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.remote.GlassRemoteSubscriptions;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerChunkCache.class)
public abstract class ServerEntityVisualMixin {
    @Shadow @Final public ServerLevel level;

    @Inject(
            method = {"broadcast", "broadcastAndSend"},
            at = @At("HEAD"))
    private void glass$visualEvents(Entity entity, Packet<?> packet, CallbackInfo ci) {
        GlassRemoteSubscriptions.entityEvent(level, entity, packet);
    }
}
