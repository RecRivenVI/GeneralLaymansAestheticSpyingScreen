package io.github.recrivenvi.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class GlassExitPlaneTest {
    static {
        io.github.recrivenvi.glass.ForgeTestBootstrap.initialize();
    }

    private static final double EPSILON = 1.0E-9;

    @Test
    void excludesTheCulledBoundaryLayerWithoutLeavingAThinOutline() {
        Vec3 center = new Vec3(10.5, 20.5, 30.5);
        for (Direction direction : Direction.values()) {
            Vec3 actual = GlassExitPlane.blockAlignedCenter(center, direction);
            double offset = 0.5 - GlassExitPlane.BOUNDARY_MARGIN;
            assertEquals(
                    center.x - direction.getStepX() * offset, actual.x, EPSILON, direction + " x");
            assertEquals(
                    center.y - direction.getStepY() * offset, actual.y, EPSILON, direction + " y");
            assertEquals(
                    center.z - direction.getStepZ() * offset, actual.z, EPSILON, direction + " z");

            Vec3 oldBoundary =
                    center.subtract(
                            direction.getStepX() * 0.5,
                            direction.getStepY() * 0.5,
                            direction.getStepZ() * 0.5);
            double signedDistance =
                    (oldBoundary.x - actual.x) * direction.getStepX()
                            + (oldBoundary.y - actual.y) * direction.getStepY()
                            + (oldBoundary.z - actual.z) * direction.getStepZ();
            assertEquals(
                    -GlassExitPlane.BOUNDARY_MARGIN,
                    signedDistance,
                    EPSILON,
                    direction + " rejected boundary");

            Vec3 keptInterior =
                    oldBoundary.add(
                            direction.getStepX() * GlassExitPlane.BOUNDARY_MARGIN * 2.0,
                            direction.getStepY() * GlassExitPlane.BOUNDARY_MARGIN * 2.0,
                            direction.getStepZ() * GlassExitPlane.BOUNDARY_MARGIN * 2.0);
            double keptDistance =
                    (keptInterior.x - actual.x) * direction.getStepX()
                            + (keptInterior.y - actual.y) * direction.getStepY()
                            + (keptInterior.z - actual.z) * direction.getStepZ();
            assertEquals(
                    GlassExitPlane.BOUNDARY_MARGIN,
                    keptDistance,
                    EPSILON,
                    direction + " kept interior");
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
