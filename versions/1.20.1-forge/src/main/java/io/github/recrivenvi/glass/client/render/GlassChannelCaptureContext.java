package io.github.recrivenvi.glass.client.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import io.github.recrivenvi.glass.core.GlassPortalTransform;
import io.github.recrivenvi.glass.mixin.client.MinecraftRenderTargetAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Owns a native world renderer and color/depth framebuffer for each channel. */
final class GlassChannelCaptureContext implements AutoCloseable {
    TextureTarget target;
    LevelRenderer renderer;
    ClientLevel level;
    private long lastAmbientTick = Long.MIN_VALUE;
    private boolean hasUnwrappedYaw;
    private float unwrappedYaw;

    boolean ensure(
            Minecraft client,
            int width,
            int height,
            ResourceKey<Level> dimension,
            BlockPos terminal) {
        if (client.level == null
                || !dimension.equals(client.level.dimension())
                || !client.level.hasChunk(terminal.getX() >> 4, terminal.getZ() >> 4)) return false;
        if (target == null) target = new TextureTarget(width, height, true, Minecraft.ON_OSX);
        else if (target.width != width || target.height != height) {
            target.resize(width, height, Minecraft.ON_OSX);
            if (renderer != null) renderer.resize(width, height);
        }
        if (renderer == null) {
            var previous = client.getMainRenderTarget();
            var access = (MinecraftRenderTargetAccessor) client;
            access.glass$setMainRenderTarget(target);
            try {
                renderer =
                        new LevelRenderer(
                                client,
                                client.getEntityRenderDispatcher(),
                                client.getBlockEntityRenderDispatcher(),
                                client.renderBuffers());
                renderer.onResourceManagerReload(client.getResourceManager());
            } finally {
                access.glass$setMainRenderTarget(previous);
                previous.bindWrite(true);
            }
        }
        if (level != client.level) {
            level = client.level;
            renderer.setLevel(level);
        }
        return true;
    }

    void animateAt(Vec3 position) {
        long time = level.getGameTime();
        if (time == lastAmbientTick) return;
        lastAmbientTick = time;
        level.animateTick(
                net.minecraft.util.Mth.floor(position.x),
                net.minecraft.util.Mth.floor(position.y),
                net.minecraft.util.Mth.floor(position.z));
    }

    float unwrapYaw(float yaw) {
        unwrappedYaw = hasUnwrappedYaw ? GlassPortalTransform.unwrapYaw(unwrappedYaw, yaw) : yaw;
        hasUnwrappedYaw = true;
        return unwrappedYaw;
    }

    @Override
    public void close() {
        if (renderer != null) {
            renderer.setLevel(null);
            renderer.close();
            renderer = null;
        }
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
        level = null;
    }
}
