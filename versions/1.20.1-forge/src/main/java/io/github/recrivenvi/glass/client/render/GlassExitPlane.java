package io.github.recrivenvi.glass.client.render;

import java.util.Collection;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

final class GlassExitPlane {
    /**
     * 裁切面不能与出口方块层的外表面严格共面，否则一个方块面的两个三角形可能因浮点 舍入分别落在近裁切面的两侧。使用可精确表示的 1/1024 方块，把平面推入保留侧，
     * 从而完整排除被剔除方块层，避免其侧面只剩一圈极薄线框。
     */
    static final double BOUNDARY_MARGIN = 1.0 / 1024.0;

    private GlassExitPlane() {}

    static Vec3 blockAlignedCenter(Vec3 portalCenter, Direction keepDirection) {
        // 进入保留层的微小余量只裁去该层贴近边界的不可见薄片，不会留下上一层的残余侧面。
        double offset = 0.5 - BOUNDARY_MARGIN;
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
