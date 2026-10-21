package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.client.render.GlassCaptureWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ClientPacketListener.class, ClientLevel.class, LevelRenderer.class, LightTexture.class})
public abstract class GlassNativeWorldReadsMixin {
    // Each of these native classes reads a different subset of the three Minecraft fields.
    @Redirect(
            method = {
                "renderSnowAndRain",
                "tickRain",
                "renderLevel",
                "renderSky",
                "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;",
                "createFireworks",
                "addDestroyBlockEffect",
                "updateLightTexture",
                "handleTickingState",
                "handleTickingStep",
                "handleSetTime",
                "handleSetSpawn",
                "handleExplosion",
                "handleBlockEntityData",
                "handleBlockEvent",
                "handleBlockDestruction",
                "handleMapItemData",
                "handleLevelEvent",
                "levelEvent",
                "handleSoundEvent",
                "handleSoundEntityEvent",
                "handleChunksBiomes",
                "handleTakeItemEntity",
                "handleAnimate",
                "handleEntityEvent"
            },
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;",
                            opcode = 180),
            require = 0)
    private static ClientLevel glass$readCaptureLevel(Minecraft client) {
        var context = GlassCaptureWorld.current();
        return context == null ? client.level : context.level();
    }

    @Redirect(
            method = {
                "renderSnowAndRain",
                "tickRain",
                "renderLevel",
                "renderSky",
                "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;",
                "createFireworks",
                "addDestroyBlockEffect",
                "updateLightTexture",
                "handleTickingState",
                "handleTickingStep",
                "handleSetTime",
                "handleSetSpawn",
                "handleExplosion",
                "handleBlockEntityData",
                "handleBlockEvent",
                "handleBlockDestruction",
                "handleMapItemData",
                "handleLevelEvent",
                "levelEvent",
                "handleSoundEvent",
                "handleSoundEntityEvent",
                "handleChunksBiomes",
                "handleTakeItemEntity",
                "handleAnimate",
                "handleEntityEvent"
            },
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/client/Minecraft;levelRenderer:Lnet/minecraft/client/renderer/LevelRenderer;",
                            opcode = 180),
            require = 0)
    private static LevelRenderer glass$readCaptureRenderer(Minecraft client) {
        var context = GlassCaptureWorld.current();
        return context == null ? client.levelRenderer : context.renderer();
    }

    @Redirect(
            method = {
                "renderSnowAndRain",
                "tickRain",
                "renderLevel",
                "renderSky",
                "addParticleInternal(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)Lnet/minecraft/client/particle/Particle;",
                "createFireworks",
                "addDestroyBlockEffect",
                "updateLightTexture",
                "handleTickingState",
                "handleTickingStep",
                "handleSetTime",
                "handleSetSpawn",
                "handleExplosion",
                "handleBlockEntityData",
                "handleBlockEvent",
                "handleBlockDestruction",
                "handleMapItemData",
                "handleLevelEvent",
                "levelEvent",
                "handleSoundEvent",
                "handleSoundEntityEvent",
                "handleChunksBiomes",
                "handleTakeItemEntity",
                "handleAnimate",
                "handleEntityEvent"
            },
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/client/Minecraft;particleEngine:Lnet/minecraft/client/particle/ParticleEngine;",
                            opcode = 180),
            require = 0)
    private static ParticleEngine glass$readCaptureParticles(Minecraft client) {
        var context = GlassCaptureWorld.current();
        return context == null ? client.particleEngine : context.particles();
    }
}
