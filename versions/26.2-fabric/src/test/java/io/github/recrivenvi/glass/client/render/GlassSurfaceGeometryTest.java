package io.github.recrivenvi.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class GlassSurfaceGeometryTest {
    private static final double EPSILON = 1.0E-12;

    @Test
    void everyFaceLiesExactlyOnTheBlockBoundary() {
        BlockPos pos = new BlockPos(10, 20, 30);
        Vec3 blockCenter = Vec3.atCenterOf(pos);
        for (Direction face : Direction.values()) {
            GlassSurfaceGeometry.Basis basis = GlassSurfaceGeometry.at(pos, face);
            assertEquals(
                    0.5,
                    basis.center().subtract(blockCenter).dot(basis.normal()),
                    EPSILON,
                    face + " plane");
            assertEquals(1.0, basis.right().length(), EPSILON, face + " right unit");
            assertEquals(1.0, basis.up().length(), EPSILON, face + " up unit");
            assertEquals(
                    0.0, basis.right().dot(basis.up()), EPSILON, face + " tangent orthogonality");
            assertEquals(
                    1.0,
                    basis.right().cross(basis.up()).dot(basis.normal()),
                    EPSILON,
                    face + " outward winding");
            for (double horizontal : new double[] {-0.5, 0.5}) {
                for (double vertical : new double[] {-0.5, 0.5}) {
                    Vec3 delta = basis.point(horizontal, vertical).subtract(basis.center());
                    assertEquals(
                            horizontal,
                            delta.dot(basis.right()),
                            EPSILON,
                            face + " horizontal boundary");
                    assertEquals(
                            vertical, delta.dot(basis.up()), EPSILON, face + " vertical boundary");
                }
            }
        }
    }
}
