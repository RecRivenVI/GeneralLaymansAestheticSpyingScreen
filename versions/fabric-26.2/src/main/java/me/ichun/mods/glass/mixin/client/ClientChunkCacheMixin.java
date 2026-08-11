package me.ichun.mods.glass.mixin.client;

import me.ichun.mods.glass.GlassClient;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientChunkCache.class)
public abstract class ClientChunkCacheMixin {
    @Inject(method = "onLightUpdate", at = @At("TAIL"))
    private void glass$forwardLightUpdate(LightLayer layer, SectionPos pos, CallbackInfo callback) {
        GlassClient.VIEW_RENDERER.setSectionDirty(pos.x(), pos.y(), pos.z());
    }
}
