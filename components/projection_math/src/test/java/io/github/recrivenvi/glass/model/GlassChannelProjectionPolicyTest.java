package io.github.recrivenvi.glass.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

final class GlassChannelProjectionPolicyTest {
    private static final float EPSILON = 1.0E-6F;

    @Test
    void keepsExitPlaneClippingOutOfOcclusionProjection() {
        Matrix4f base =
                new Matrix4f()
                        .setPerspective(
                                (float) Math.toRadians(70.0), 16.0F / 9.0F, 512.0F, 0.05F, true);
        Matrix4f renderProjection =
                GlassObliqueProjection.apply(base, new Vector4f(0.0F, 0.0F, -1.0F, -2.0F), true);

        Matrix4f occlusionProjection = GlassChannelProjectionPolicy.forOcclusionGraph(base);

        assertNotSame(base, occlusionProjection);
        assertTrue(base.equals(occlusionProjection, EPSILON));
        assertFalse(renderProjection.equals(occlusionProjection, EPSILON));
    }
}
