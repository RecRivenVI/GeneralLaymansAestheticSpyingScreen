package io.github.recrivenvi.glass.mixin;

import io.github.recrivenvi.glass.state.GlassServerState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerChunkSender.class)
public abstract class PlayerChunkSenderMixin {
    // Resident chunks can be resent within the unload grace period without another load event.
    @Inject(method = "sendChunk", at = @At("RETURN"))
    private static void glass$restoreObservedNetwork(
            ServerGamePacketListenerImpl connection,
            ServerLevel level,
            LevelChunk chunk,
            CallbackInfo ci) {
        GlassServerState.chunkLoaded(level, chunk.getPos());
    }
}
