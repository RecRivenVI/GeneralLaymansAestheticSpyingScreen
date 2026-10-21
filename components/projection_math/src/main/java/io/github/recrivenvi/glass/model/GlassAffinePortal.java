package io.github.recrivenvi.glass.model;

import org.joml.Matrix4d;
import org.joml.Matrix4dc;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector4d;
import org.joml.Vector4dc;

/** Affine portal math; poses include translation, rotation point, rotation, and scale. */
public final class GlassAffinePortal {
    private GlassAffinePortal() {}

    public static Matrix4d mapping(
            Matrix4dc projectorPose,
            Matrix4dc terminalPose,
            Vector3dc projectorCenter,
            Vector3dc terminalCenter,
            Quaterniondc localRotation) {
        return new Matrix4d(terminalPose)
                .translate(terminalCenter)
                .rotate(localRotation)
                .translate(-projectorCenter.x(), -projectorCenter.y(), -projectorCenter.z())
                .mul(new Matrix4d(projectorPose).invert());
    }

    public static Matrix4d camera(Matrix4dc mapping, Vector3dc position, Quaterniondc orientation) {
        return new Matrix4d(mapping).translate(position).rotate(orientation);
    }

    public static Matrix4d relativeView(Matrix4dc camera) {
        return new Matrix4d(camera).m30(0).m31(0).m32(0).invert();
    }

    public static Vector4d worldPlane(Matrix4dc pose, Vector3dc center, Vector3dc normal) {
        var plane = new Vector4d(normal, -new Vector3d(normal).dot(center));
        new Matrix4d(pose).invert().transpose().transform(plane);
        return plane.div(Math.sqrt(plane.x * plane.x + plane.y * plane.y + plane.z * plane.z));
    }

    public static Vector4d viewPlane(Matrix4dc camera, Vector4dc worldPlane) {
        return new Matrix4d(camera).transpose().transform(new Vector4d(worldPlane));
    }

    public static Bounds bounds(Matrix4dc pose, Vector3dc min, Vector3dc max) {
        Vector3d low = new Vector3d(Double.POSITIVE_INFINITY),
                high = new Vector3d(Double.NEGATIVE_INFINITY);
        for (int i = 0; i < 8; i++) {
            Vector3d p =
                    new Matrix4d(pose)
                            .transformPosition(
                                    new Vector3d(
                                            (i & 1) == 0 ? min.x() : max.x(),
                                            (i & 2) == 0 ? min.y() : max.y(),
                                            (i & 4) == 0 ? min.z() : max.z()));
            low.min(p);
            high.max(p);
        }
        return new Bounds(low, high);
    }

    public record Bounds(Vector3d min, Vector3d max) {}
}
