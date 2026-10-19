package io.github.recrivenvi.glass.model;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class GlassChannelProjectionPolicy {
    private GlassChannelProjectionPolicy() {}

    public static Matrix4f forOcclusionGraph(Matrix4fc baseProjection) {
        // 原版遮挡图会反复平移视锥直到完整包含相机区块；任意斜切面可能让该循环永远无法收敛。
        return new Matrix4f(baseProjection);
    }
}
