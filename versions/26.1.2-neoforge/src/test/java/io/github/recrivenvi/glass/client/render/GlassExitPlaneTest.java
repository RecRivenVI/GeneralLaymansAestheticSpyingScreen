package io.github.recrivenvi.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class GlassExitPlaneTest {

    @Test
    void retainsBoundaryAndOneTexelGapButRejectsThePreviousLayer() {
        Vec3 center = new Vec3(10.5, 20.5, 30.5);
        for (Direction direction : Direction.values()) {
            Vec3 normal = Vec3.atLowerCornerOf(direction.getUnitVec3i());
            Vec3 boundary = center.subtract(normal.scale(0.5));
            Vec3 plane = GlassExitPlane.blockAlignedCenter(center, direction);
            assertTrue(
                    boundary.subtract(plane).dot(normal) > 0,
                    direction + " touching face retained");
            assertTrue(
                    boundary.add(normal.scale(1.0 / 16.0)).subtract(plane).dot(normal) > 0,
                    direction + " one texel gap retained");
            assertTrue(
                    boundary.subtract(normal.scale(1.0 / 16.0)).subtract(plane).dot(normal) < 0,
                    direction + " previous layer rejected");
        }
    }

    @Test
    void followsTheCurrentTopologyInsteadOfRememberingOldFaces() {
        assertFalse(GlassExitPlane.shouldCull(EnumSet.of(Direction.NORTH, Direction.UP), false));
        assertTrue(GlassExitPlane.shouldCull(EnumSet.of(Direction.NORTH), false));
        assertFalse(GlassExitPlane.shouldCull(EnumSet.of(Direction.NORTH), true));
        assertFalse(GlassExitPlane.shouldCull(EnumSet.noneOf(Direction.class), false));
    }
}
