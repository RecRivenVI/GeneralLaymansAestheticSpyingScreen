package io.github.recrivenvi.glass.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

class GlassObliqueProjectionForwardTest {
    @Test
    void forwardDepthPlacesTheExitPlaneOnTheNearBoundaryForBothClipRanges() {
        for (boolean zeroToOne : new boolean[] {false, true}) {
            Matrix4f projection =
                    new Matrix4f()
                            .perspective((float) Math.toRadians(70), 1.5f, 0.05f, 100, zeroToOne);
            Matrix4f clipped =
                    GlassObliqueProjection.applyForwardZ(
                            projection, new Vector4f(0, 0, -1, -4), zeroToOne);
            Vector4f boundary = clipped.transform(new Vector4f(0, 0, -4, 1));
            Vector4f kept = clipped.transform(new Vector4f(0, 0, -5, 1));
            Vector4f removed = clipped.transform(new Vector4f(0, 0, -3, 1));
            assertEquals(0, boundary.z + (zeroToOne ? 0 : boundary.w), 1e-5);
            assertTrue(kept.z + (zeroToOne ? 0 : kept.w) > 0);
            assertTrue(removed.z + (zeroToOne ? 0 : removed.w) < 0);
        }
    }
}
