package me.ichun.mods.glass.core;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/**
 * Replaces the reversed-Z perspective near plane with an arbitrary camera-space plane. This is the
 * backend-neutral equivalent of iChunUtil's per-block exit plane culling and works for both
 * Vulkan's zero-to-one and OpenGL clip ranges.
 */
public final class GlassObliqueProjection {
    private static final float EPSILON = 1.0E-6F;

    private GlassObliqueProjection() {}

    public static Matrix4f apply(
            Matrix4fc projection, Vector4f keepPositivePlane, boolean zeroToOne) {
        Matrix4f result = new Matrix4f(projection);
        Vector4f plane = new Vector4f(keepPositivePlane);
        float length = (float) Math.sqrt(plane.x * plane.x + plane.y * plane.y + plane.z * plane.z);
        if (length < EPSILON) {
            return result;
        }
        plane.div(length);

        Vector4f clipCorner =
                new Vector4f(
                        copySignOne(plane.x), copySignOne(plane.y), zeroToOne ? 0.0F : -1.0F, 1.0F);
        Vector4f oppositeCorner = new Matrix4f(projection).invert().transform(clipCorner);
        float denominator = plane.dot(oppositeCorner);
        if (Math.abs(denominator) < EPSILON) {
            return result;
        }
        float scale = (zeroToOne ? 1.0F : 2.0F) / denominator;
        plane.mul(scale);

        // 26.2 uses reversed depth: the near boundary is w - z = 0.
        result.m02(result.m03() - plane.x);
        result.m12(result.m13() - plane.y);
        result.m22(result.m23() - plane.z);
        result.m32(result.m33() - plane.w);
        return result;
    }

    private static float copySignOne(float value) {
        return value < 0.0F ? -1.0F : 1.0F;
    }
}
