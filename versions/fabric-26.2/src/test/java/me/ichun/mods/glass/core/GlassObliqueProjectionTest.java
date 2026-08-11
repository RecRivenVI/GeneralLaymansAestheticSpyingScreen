package me.ichun.mods.glass.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

final class GlassObliqueProjectionTest {
    @Test
    void zeroToOneReversedProjectionKeepsPositivePlaneSide() {
        verify(true);
    }

    @Test
    void negativeOneToOneReversedProjectionKeepsPositivePlaneSide() {
        verify(false);
    }

    private static void verify(boolean zeroToOne) {
        Matrix4f base =
                new Matrix4f()
                        .setPerspective(
                                (float) Math.toRadians(70.0),
                                16.0F / 9.0F,
                                512.0F,
                                0.05F,
                                zeroToOne);
        // Camera at origin looks down -Z. Keep geometry beyond z=-2.
        Matrix4f clipped =
                GlassObliqueProjection.apply(
                        base, new Vector4f(0.0F, 0.0F, -1.0F, -2.0F), zeroToOne);
        Vector4f kept = clipped.transform(new Vector4f(0.0F, 0.0F, -4.0F, 1.0F));
        Vector4f rejected = clipped.transform(new Vector4f(0.0F, 0.0F, -1.0F, 1.0F));
        assertTrue(isInside(kept, zeroToOne), "far-side point should remain in the clip volume");
        assertTrue(!isInside(rejected, zeroToOne), "camera-side point should be clipped");
    }

    private static boolean isInside(Vector4f clip, boolean zeroToOne) {
        boolean zInside =
                zeroToOne
                        ? clip.z >= 0.0F && clip.z <= clip.w
                        : clip.z >= -clip.w && clip.z <= clip.w;
        return Math.abs(clip.x) <= clip.w && Math.abs(clip.y) <= clip.w && zInside;
    }
}
