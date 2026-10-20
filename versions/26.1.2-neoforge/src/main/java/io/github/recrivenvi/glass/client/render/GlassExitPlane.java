package io.github.recrivenvi.glass.client.render;

import java.util.Collection;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

final class GlassExitPlane {
    /** 向被剔除侧留出可精确表示的余量，使紧贴出口边界的面落在保留侧。 */
    static final double BOUNDARY_MARGIN = 1.0 / 4096.0;

    private GlassExitPlane() {}

    static Vec3 blockAlignedCenter(Vec3 portalCenter, Direction keepDirection) {
        double offset = 0.5 + BOUNDARY_MARGIN;
        return portalCenter.subtract(
                keepDirection.getStepX() * offset,
                keepDirection.getStepY() * offset,
                keepDirection.getStepZ() * offset);
    }

    /** 拓扑每次重建后重新判定；拆掉拐角时可自动恢复唯一出口面的裁切。 */
    static boolean shouldCull(Collection<Direction> activeFaces, boolean wireless) {
        if (wireless || activeFaces.isEmpty()) return false;
        Direction only = null;
        for (Direction face : activeFaces) {
            if (only == null) only = face;
            else if (only != face) return false;
        }
        return true;
    }
}
