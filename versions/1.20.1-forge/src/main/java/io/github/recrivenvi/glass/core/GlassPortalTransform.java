package io.github.recrivenvi.glass.core;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/**
 * 与 iChunUtil 7.1.3 {@code QuaternionFormula.createFromPlanes} 等价的平面变换。
 *
 * <p>GLASS 的画面依赖这个变换的右手性；静态纹理翻转不能替代它。
 */
public final class GlassPortalTransform {
    private static final Quaternion IDENTITY = new Quaternion(0.0F, 0.0F, 0.0F, 1.0F);

    private final Quaternion inHorizontal;
    private final Quaternion outHorizontal;
    private final Quaternion inVertical;
    private final Quaternion outVertical;

    private GlassPortalTransform(
            Quaternion inHorizontal,
            Quaternion outHorizontal,
            Quaternion inVertical,
            Quaternion outVertical) {
        this.inHorizontal = inHorizontal;
        this.outHorizontal = outHorizontal;
        this.inVertical = inVertical;
        this.outVertical = outVertical;
    }

    public static GlassPortalTransform fromPlanes(
            Direction sourceFace,
            Direction sourceUp,
            Direction destinationFace,
            Direction destinationUp) {
        Direction sourceDirection = sourceFace.getOpposite();
        Direction adjustedSourceUp = sourceFace == Direction.UP ? sourceUp : sourceUp.getOpposite();
        Direction adjustedDestinationUp =
                destinationFace == Direction.DOWN ? destinationUp : destinationUp.getOpposite();
        return fromFaces(sourceDirection, adjustedSourceUp, destinationFace, adjustedDestinationUp);
    }

    private static GlassPortalTransform fromFaces(
            Direction sourceDirection,
            Direction sourceUp,
            Direction destinationDirection,
            Direction destinationUp) {
        boolean vertical =
                sourceDirection.getAxis().isVertical()
                        || destinationDirection.getAxis().isVertical();
        Direction sourceHorizontal =
                sourceDirection.getAxis().isVertical() ? sourceUp : sourceDirection;
        Direction destinationHorizontal =
                destinationDirection.getAxis().isVertical() ? destinationUp : destinationDirection;
        return new GlassPortalTransform(
                horizontal(sourceHorizontal),
                horizontal(destinationHorizontal),
                vertical ? vertical(sourceDirection) : IDENTITY,
                vertical ? vertical(destinationDirection) : IDENTITY);
    }

    public Vec3 transformVector(Vec3 vector) {
        Quaternion rotated =
                rotate(new Quaternion((float) vector.x, (float) vector.y, (float) vector.z, 0.0F));
        return new Vec3(rotated.x, rotated.y, rotated.z);
    }

    public Vector3f transformVector(Vector3f vector) {
        Quaternion rotated = rotate(new Quaternion(vector.x, vector.y, vector.z, 0.0F));
        return vector.set(rotated.x, rotated.y, rotated.z);
    }

    public Vec3 transformPosition(Vec3 sourcePosition, Vec3 sourceCenter, Vec3 destinationCenter) {
        return destinationCenter.add(transformVector(sourcePosition.subtract(sourceCenter)));
    }

    public Quaternionf transformOrientation(Quaternionfc sourceOrientation) {
        Vector3f x = transformVector(new Vector3f(1.0F, 0.0F, 0.0F).rotate(sourceOrientation));
        Vector3f y = transformVector(new Vector3f(0.0F, 1.0F, 0.0F).rotate(sourceOrientation));
        Vector3f z = transformVector(new Vector3f(0.0F, 0.0F, 1.0F).rotate(sourceOrientation));
        Matrix3f orientation = new Matrix3f().setColumn(0, x).setColumn(1, y).setColumn(2, z);
        return new Quaternionf().setFromNormalized(orientation).normalize();
    }

    /** 从最终相机基底生成 Minecraft 仍需保存的 yaw/pitch 元数据。 正上或正下时前向量无法定义 yaw，改用屏幕上方向选择唯一且稳定的等价角度。 */
    public static CameraAngles cameraAngles(Quaternionfc orientation) {
        Vector3f forward = new Vector3f(0.0F, 0.0F, -1.0F).rotate(orientation).normalize();
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F).rotate(orientation).normalize();
        double horizontalLength = Math.hypot(forward.x, forward.z);
        float pitch = (float) Math.toDegrees(Math.atan2(-forward.y, horizontalLength));
        double yaw;
        if (horizontalLength > 1.0E-4) {
            yaw = Math.atan2(-forward.x, forward.z);
        } else {
            float pitchSign = forward.y <= 0.0F ? 1.0F : -1.0F;
            yaw = Math.atan2(-up.x * pitchSign, up.z * pitchSign);
        }
        return new CameraAngles((float) Math.toDegrees(yaw), pitch);
    }

    /** 选择与上一帧最接近的等价 yaw，避免跨过正负 180 度时数值跳转一整圈。 */
    public static float unwrapYaw(float previousYaw, float wrappedYaw) {
        float delta = wrappedYaw - previousYaw;
        delta -= 360.0F * (float) Math.floor((delta + 180.0F) / 360.0F);
        return previousYaw + delta;
    }

    public Matrix3f rotationMatrix() {
        Vector3f x = transformVector(new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f y = transformVector(new Vector3f(0.0F, 1.0F, 0.0F));
        Vector3f z = transformVector(new Vector3f(0.0F, 0.0F, 1.0F));
        return new Matrix3f().setColumn(0, x).setColumn(1, y).setColumn(2, z);
    }

    private Quaternion rotate(Quaternion position) {
        Quaternion rotated = inHorizontal.multiply(position).multiply(inHorizontal.conjugate());
        rotated = inVertical.multiply(rotated).multiply(inVertical.conjugate());
        rotated = outVertical.conjugate().multiply(rotated).multiply(outVertical);
        return outHorizontal.conjugate().multiply(rotated).multiply(outHorizontal);
    }

    private static Quaternion horizontal(Direction direction) {
        float angle =
                switch (direction) {
                    case SOUTH -> 0.0F;
                    case WEST -> 90.0F;
                    case NORTH -> 180.0F;
                    case EAST -> 270.0F;
                    case UP, DOWN -> 0.0F;
                };
        return fromZxyEuler(0.0F, angle, 0.0F);
    }

    private static Quaternion vertical(Direction direction) {
        return fromZxyEuler(direction.getStepY() * 90.0F, 0.0F, 0.0F);
    }

    private static Quaternion fromZxyEuler(float xDegrees, float yDegrees, float zDegrees) {
        double x = Math.toRadians(xDegrees);
        double y = Math.toRadians(yDegrees);
        double z = Math.toRadians(zDegrees);
        float cx = (float) Math.cos(x);
        float sx = (float) Math.sin(x);
        float cy = (float) Math.cos(y);
        float sy = (float) Math.sin(y);
        float cz = (float) Math.cos(z);
        float sz = (float) Math.sin(z);
        return Quaternion.fromMatrix(
                cy * cz - sx * sy * sz,
                -cx * sz,
                cz * sy + cy * sx * sz,
                cz * sx * sy + cy * sz,
                cx * cz,
                -cy * cz * sx + sy * sz,
                -cx * sy,
                sx,
                cx * cy);
    }

    public record CameraAngles(float yaw, float pitch) {}

    private record Quaternion(float x, float y, float z, float w) {
        Quaternion conjugate() {
            return new Quaternion(-x, -y, -z, w);
        }

        Quaternion multiply(Quaternion right) {
            return new Quaternion(
                    x * right.w + w * right.x + y * right.z - z * right.y,
                    y * right.w + w * right.y + z * right.x - x * right.z,
                    z * right.w + w * right.z + x * right.y - y * right.x,
                    w * right.w - x * right.x - y * right.y - z * right.z);
        }

        static Quaternion fromMatrix(
                float m00,
                float m01,
                float m02,
                float m10,
                float m11,
                float m12,
                float m20,
                float m21,
                float m22) {
            float trace = m00 + m11 + m22;
            if (trace >= 0.0F) {
                float magnitude = (float) Math.sqrt(trace + 1.0F);
                float w = magnitude * 0.5F;
                magnitude = 0.5F / magnitude;
                return new Quaternion(
                        (m21 - m12) * magnitude,
                        (m02 - m20) * magnitude,
                        (m10 - m01) * magnitude,
                        w);
            }
            if (m00 >= m11 && m00 >= m22) {
                float magnitude = (float) Math.sqrt(m00 - m11 - m22 + 1.0F);
                float x = magnitude * 0.5F;
                magnitude = 0.5F / magnitude;
                return new Quaternion(
                        x,
                        (m01 + m10) * magnitude,
                        (m20 + m02) * magnitude,
                        (m21 - m12) * magnitude);
            }
            if (m11 >= m22) {
                float magnitude = (float) Math.sqrt(m11 - m22 - m00 + 1.0F);
                float y = magnitude * 0.5F;
                magnitude = 0.5F / magnitude;
                return new Quaternion(
                        (m01 + m10) * magnitude,
                        y,
                        (m12 + m21) * magnitude,
                        (m02 - m20) * magnitude);
            }
            float magnitude = (float) Math.sqrt(m22 - m00 - m11 + 1.0F);
            float z = magnitude * 0.5F;
            magnitude = 0.5F / magnitude;
            return new Quaternion(
                    (m20 + m02) * magnitude, (m12 + m21) * magnitude, z, (m10 - m01) * magnitude);
        }
    }
}
