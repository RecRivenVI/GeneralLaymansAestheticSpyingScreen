package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.GlassClient;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final
    private net.minecraft.client.multiplayer.ClientLevel level;

    @Inject(method = "replaceWithPacketData", at = @At("RETURN"))
    private void glass$loadInteriors(
            int x,
            int z,
            net.minecraft.network.FriendlyByteBuf data,
            net.minecraft.nbt.CompoundTag heightmaps,
            java.util.function.Consumer<
                            net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData
                                    .BlockEntityTagOutput>
                    blockEntities,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<
                            net.minecraft.world.level.chunk.LevelChunk>
                    callback) {
        if (callback.getReturnValue() != null)
            io.github.recrivenvi.glass.client.render.GlassBlockInteriors.chunkLoaded(
                    level, callback.getReturnValue());
    }

    @Inject(method = "drop", at = @At("HEAD"))
    private void glass$unloadInteriors(
            net.minecraft.world.level.ChunkPos pos, CallbackInfo callback) {
        io.github.recrivenvi.glass.client.render.GlassBlockInteriors.chunkUnloaded(level, pos);
    }

    @Inject(method = "onLightUpdate", at = @At("TAIL"))
    private void glass$forwardLightUpdate(LightLayer layer, SectionPos pos, CallbackInfo callback) {
        GlassClient.VIEW_RENDERER.setSectionDirty(pos.x(), pos.y(), pos.z());
    }
}
