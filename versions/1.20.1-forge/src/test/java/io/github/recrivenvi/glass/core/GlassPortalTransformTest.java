package io.github.recrivenvi.glass.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

final class GlassPortalTransformTest {
    static {
        io.github.recrivenvi.glass.ForgeTestBootstrap.initialize();
    }

    private static final float EPSILON = 0.0002F;

    @Test
    void everyPlanePairMapsSourceFrontToDestinationBackWithoutMirroring() {
        for (Direction source : Direction.values()) {
            for (Direction destination : Direction.values()) {
                GlassPortalTransform transform =
                        GlassPortalTransform.fromPlanes(
                                source, Direction.UP, destination, Direction.UP);
                Vector3f actual = transform.transformVector(direction(source));
                Vector3f expected = direction(destination.getOpposite());
                assertVector(expected, actual, source + " -> " + destination);
                assertTrue(
                        transform.rotationMatrix().determinant() > 0.999F,
                        source + " -> " + destination + " mirrored");
            }
        }
    }

    @Test
    void everyDirectionPairPreservesLengthsAnglesAndRightHandedness() {
        Vector3f a = new Vector3f(0.25F, -0.5F, 0.75F);
        Vector3f b = new Vector3f(-0.6F, 0.7F, 0.2F);
        for (Direction source : Direction.values()) {
            for (Direction destination : Direction.values()) {
                GlassPortalTransform transform =
                        GlassPortalTransform.fromPlanes(
                                source, Direction.UP, destination, Direction.UP);
                Vector3f transformedA = transform.transformVector(new Vector3f(a));
                Vector3f transformedB = transform.transformVector(new Vector3f(b));
                assertEquals(
                        a.length(),
                        transformedA.length(),
                        EPSILON,
                        source + " -> " + destination + " length A");
                assertEquals(
                        b.length(),
                        transformedB.length(),
                        EPSILON,
                        source + " -> " + destination + " length B");
                assertEquals(
                        a.dot(b),
                        transformedA.dot(transformedB),
                        EPSILON,
                        source + " -> " + destination + " angle");
                assertTrue(
                        transform.rotationMatrix().determinant() > 0.999F,
                        source + " -> " + destination + " mirrored");
            }
        }
    }

    @Test
    void positionUsesProjectorRelativeOffsetAtTerminalPairCenter() {
        GlassPortalTransform transform =
                GlassPortalTransform.fromPlanes(
                        Direction.NORTH, Direction.UP, Direction.EAST, Direction.UP);
        Vec3 sourceCenter = new Vec3(10.5, 64.5, 20.5);
        Vec3 destinationCenter = new Vec3(-4.5, 70.5, 8.5);
        Vec3 transformed =
                transform.transformPosition(
                        sourceCenter.add(0.0, 1.0, -2.0), sourceCenter, destinationCenter);
        assertEquals(destinationCenter.x - 2.0, transformed.x, EPSILON);
        assertEquals(destinationCenter.y + 1.0, transformed.y, EPSILON);
        assertEquals(destinationCenter.z, transformed.z, EPSILON);
    }

    @Test
    void orientationUsesTheSameProperRotationAsPosition() {
        GlassPortalTransform transform =
                GlassPortalTransform.fromPlanes(
                        Direction.NORTH, Direction.UP, Direction.WEST, Direction.UP);
        Quaternionf original =
                new Quaternionf().rotationYXZ((float) Math.PI, (float) Math.toRadians(-15.0), 0.0F);
        Quaternionf transformed = transform.transformOrientation(original);
        Vector3f expectedForward =
                transform.transformVector(new Vector3f(0.0F, 0.0F, -1.0F).rotate(original));
        Vector3f actualForward = new Vector3f(0.0F, 0.0F, -1.0F).rotate(transformed);
        assertVector(expectedForward, actualForward, "orientation forward");
    }

    @Test
    void orientationCarriesTheCompleteCameraBasisAcrossEveryPlanePair() {
        Quaternionf original =
                new Quaternionf()
                        .rotationYXZ(
                                (float) Math.toRadians(143.0), (float) Math.toRadians(23.0), 0.0F);
        Vector3f[] basis = {
            new Vector3f(0.0F, 0.0F, -1.0F),
            new Vector3f(0.0F, 1.0F, 0.0F),
            new Vector3f(-1.0F, 0.0F, 0.0F)
        };
        for (Direction source : Direction.values()) {
            for (Direction destination : Direction.values()) {
                GlassPortalTransform transform =
                        GlassPortalTransform.fromPlanes(
                                source, Direction.UP, destination, Direction.UP);
                Quaternionf transformed = transform.transformOrientation(original);
                for (Vector3f axis : basis) {
                    Vector3f expected =
                            transform.transformVector(new Vector3f(axis).rotate(original));
                    Vector3f actual = new Vector3f(axis).rotate(transformed);
                    assertVector(
                            expected, actual, source + " -> " + destination + " basis " + axis);
                }
            }
        }
    }

    @Test
    void cameraAnglesRecoverYawAtBothExactVerticalPoles() {
        for (float pitch : new float[] {-90.0F, 90.0F}) {
            Quaternionf orientation = minecraftOrientation(37.0F, pitch);
            GlassPortalTransform.CameraAngles angles =
                    GlassPortalTransform.cameraAngles(orientation);
            assertEquals(37.0F, angles.yaw(), EPSILON, "yaw at pitch " + pitch);
            assertEquals(pitch, angles.pitch(), EPSILON, "pitch");
        }
    }

    @Test
    void cameraAnglesRemainContinuousOnApproachToVertical() {
        for (float pitch : new float[] {-89.999F, 89.999F}) {
            Quaternionf orientation = minecraftOrientation(-143.0F, pitch);
            GlassPortalTransform.CameraAngles angles =
                    GlassPortalTransform.cameraAngles(orientation);
            assertEquals(-143.0F, angles.yaw(), 0.002F, "yaw at pitch " + pitch);
            assertEquals(pitch, angles.pitch(), 0.002F, "pitch");
        }
    }

    @Test
    void cameraAnglesPreserveTheTransformedForwardAndVerticalScreenUp() {
        float[][] sourceAngles = {
            {0.0F, 0.0F},
            {37.0F, 25.0F},
            {-143.0F, -62.0F},
            {91.0F, 90.0F},
            {-73.0F, -90.0F}
        };
        for (Direction source : Direction.values()) {
            for (Direction destination : Direction.values()) {
                GlassPortalTransform transform =
                        GlassPortalTransform.fromPlanes(
                                source, Direction.UP, destination, Direction.UP);
                for (float[] sourceAngle : sourceAngles) {
                    Quaternionf transformed =
                            transform.transformOrientation(
                                    minecraftOrientation(sourceAngle[0], sourceAngle[1]));
                    GlassPortalTransform.CameraAngles angles =
                            GlassPortalTransform.cameraAngles(transformed);
                    Quaternionf metadata = minecraftOrientation(angles.yaw(), angles.pitch());
                    Vector3f expectedForward = new Vector3f(0.0F, 0.0F, -1.0F).rotate(transformed);
                    Vector3f actualForward = new Vector3f(0.0F, 0.0F, -1.0F).rotate(metadata);
                    assertVector(
                            expectedForward,
                            actualForward,
                            source + " -> " + destination + " forward");
                    if (Math.abs(expectedForward.y) > 0.99999F) {
                        Vector3f expectedUp = new Vector3f(0.0F, 1.0F, 0.0F).rotate(transformed);
                        Vector3f actualUp = new Vector3f(0.0F, 1.0F, 0.0F).rotate(metadata);
                        assertVector(
                                expectedUp,
                                actualUp,
                                source + " -> " + destination + " vertical up");
                    }
                }
            }
        }
    }

    @Test
    void yawUnwrapChoosesTheNearestEquivalentAngle() {
        assertEquals(181.0F, GlassPortalTransform.unwrapYaw(179.0F, -179.0F), EPSILON);
        assertEquals(-181.0F, GlassPortalTransform.unwrapYaw(-179.0F, 179.0F), EPSILON);
        assertEquals(540.0F, GlassPortalTransform.unwrapYaw(530.0F, 180.0F), EPSILON);
    }

    private static Quaternionf minecraftOrientation(float yaw, float pitch) {
        return new Quaternionf()
                .rotationYXZ(
                        (float) Math.PI - (float) Math.toRadians(yaw),
                        -(float) Math.toRadians(pitch),
                        0.0F);
    }

    private static Vector3f direction(Direction direction) {
        return new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private static void assertVector(Vector3f expected, Vector3f actual, String message) {
        assertEquals(expected.x, actual.x, EPSILON, message + " x");
        assertEquals(expected.y, actual.y, EPSILON, message + " y");
        assertEquals(expected.z, actual.z, EPSILON, message + " z");
    }
}
