package io.github.recrivenvi.glass.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.junit.jupiter.api.Test;

class GlassAffinePortalTest {
    @Test
    void stationaryPortalKeepsCheckpointHalfTurn() {
        var map =
                GlassAffinePortal.mapping(
                        new Matrix4d(),
                        new Matrix4d(),
                        new Vector3d(.5, 200.5, .5),
                        new Vector3d(16.5, 200.5, 17.5),
                        new Quaterniond().rotationY(Math.PI));
        var p = map.transformPosition(new Vector3d(.5, 200.5, -5.5));
        assertEquals(16.5, p.x, 1e-10);
        assertEquals(200.5, p.y, 1e-10);
        assertEquals(23.5, p.z, 1e-10);
    }

    @Test
    void tiltedScaledPlanePreservesTheKeptSide() {
        var pose =
                new Matrix4d()
                        .translate(7, 11, -9)
                        .rotateXYZ(.37, -.51, .29)
                        .scale(.7, 1.4, 2.1)
                        .translate(-1.25, -2.5, .75);
        var center = new Vector3d(1.25, 2.5, 4);
        var normal = new Vector3d(0, 0, 1);
        var plane = GlassAffinePortal.worldPlane(pose, center, normal);
        var on = pose.transformPosition(new Vector3d(4, -3, 4));
        var front = pose.transformPosition(new Vector3d(4, -3, 4.1));
        var back = pose.transformPosition(new Vector3d(4, -3, 3.9));
        assertEquals(0, plane.dot(new Vector4d(on, 1)), 1e-10);
        assertTrue(plane.dot(new Vector4d(front, 1)) > 0);
        assertTrue(plane.dot(new Vector4d(back, 1)) < 0);
    }

    @Test
    void cameraSpacePlaneIncludesTranslationPivotAndShear() {
        var pose =
                new Matrix4d()
                        .translate(3, -4, 5)
                        .rotateXYZ(.4, .7, -.2)
                        .scale(2, .75, 1.2)
                        .translate(-20, -30, -40);
        var camera = new Matrix4d().translate(9, 8, 7).rotateXYZ(-.3, .6, .5).scale(.8, 1.6, 1.1);
        var plane =
                GlassAffinePortal.viewPlane(
                        camera,
                        GlassAffinePortal.worldPlane(
                                pose, new Vector3d(20, 30, 40), new Vector3d(0, 1, 0)));
        var p = pose.transformPosition(new Vector3d(23, 30, 42));
        new Matrix4d(camera).invert().transformPosition(p);
        assertEquals(0, plane.dot(new Vector4d(p, 1)), 1e-9);
    }

    @Test
    void boundsRotateAllCornersAfterScaling() {
        var bounds =
                GlassAffinePortal.bounds(
                        new Matrix4d().translate(10, 20, 30).rotateZ(Math.PI / 2).scale(2, 3, 4),
                        new Vector3d(),
                        new Vector3d(1));
        assertEquals(7, bounds.min().x, 1e-10);
        assertEquals(20, bounds.min().y, 1e-10);
        assertEquals(30, bounds.min().z, 1e-10);
        assertEquals(10, bounds.max().x, 1e-10);
        assertEquals(22, bounds.max().y, 1e-10);
        assertEquals(34, bounds.max().z, 1e-10);
    }
}
