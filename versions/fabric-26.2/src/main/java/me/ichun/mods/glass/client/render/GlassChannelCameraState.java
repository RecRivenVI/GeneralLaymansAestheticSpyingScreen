package me.ichun.mods.glass.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

final class GlassChannelCameraState {
    private GlassChannelCameraState() {}

    static BlockPos immutableBlockPosition(Vec3 cameraPosition) {
        return BlockPos.containing(cameraPosition);
    }
}
