package io.github.recrivenvi.glass.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/** 方块边界上的精确投影基底；法向和两条切向均不使用补偿偏移。 */
final class GlassSurfaceGeometry {
    static final double HALF_SIZE = 0.5;

    private GlassSurfaceGeometry() {}

    static Basis at(BlockPos pos, Direction face) {
        Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
        Vec3 center = Vec3.atCenterOf(pos).add(normal.scale(HALF_SIZE));
        Vec3 right;
        Vec3 up;
        if (face.getAxis().isVertical()) {
            right = new Vec3(1.0, 0.0, 0.0);
            up = new Vec3(0.0, 0.0, -face.getStepY());
        } else {
            right = new Vec3(face.getStepZ(), 0.0, -face.getStepX());
            up = new Vec3(0.0, 1.0, 0.0);
        }
        return new Basis(center, normal, right, up);
    }

    record Basis(Vec3 center, Vec3 normal, Vec3 right, Vec3 up) {
        Vec3 point(double horizontal, double vertical) {
            return center.add(right.scale(horizontal)).add(up.scale(vertical));
        }
    }
}
