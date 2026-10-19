package io.github.recrivenvi.glass.model;

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
        return apply(projection, keepPositivePlane, zeroToOne, true);
    }

    public static Matrix4f applyForwardZ(
            Matrix4fc projection, Vector4f keepPositivePlane, boolean zeroToOne) {
        return apply(projection, keepPositivePlane, zeroToOne, false);
    }

    private static Matrix4f apply(
            Matrix4fc projection,
            Vector4f keepPositivePlane,
            boolean zeroToOne,
            boolean reversedDepth) {
        Matrix4f result = new Matrix4f(projection);
        Vector4f plane = new Vector4f(keepPositivePlane);
        float length = (float) Math.sqrt(plane.x * plane.x + plane.y * plane.y + plane.z * plane.z);
        if (length < EPSILON) {
            return result;
        }
        plane.div(length);

        Vector4f clipCorner =
                new Vector4f(
                        copySignOne(plane.x),
                        copySignOne(plane.y),
                        reversedDepth ? (zeroToOne ? 0.0F : -1.0F) : 1.0F,
                        1.0F);
        Vector4f oppositeCorner = new Matrix4f(projection).invert().transform(clipCorner);
        float denominator = plane.dot(oppositeCorner);
        if (Math.abs(denominator) < EPSILON) {
            return result;
        }
        float scale = (zeroToOne ? 1.0F : 2.0F) / denominator;
        plane.mul(scale);

        if (reversedDepth) {
            result.m02(result.m03() - plane.x);
            result.m12(result.m13() - plane.y);
            result.m22(result.m23() - plane.z);
            result.m32(result.m33() - plane.w);
        } else {
            float subtractW = zeroToOne ? 0.0F : 1.0F;
            result.m02(plane.x - subtractW * result.m03());
            result.m12(plane.y - subtractW * result.m13());
            result.m22(plane.z - subtractW * result.m23());
            result.m32(plane.w - subtractW * result.m33());
        }
        return result;
    }

    private static float copySignOne(float value) {
        return value < 0.0F ? -1.0F : 1.0F;
    }
}
