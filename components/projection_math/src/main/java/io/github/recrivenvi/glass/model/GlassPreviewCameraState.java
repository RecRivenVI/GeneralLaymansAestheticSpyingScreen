package io.github.recrivenvi.glass.model;

/** 预览相机相对于投影源正面的可持久化调整量。 */
public record GlassPreviewCameraState(
        float yawOffset, float pitchOffset, float zoom, float panX, float panY) {
    public static final float DEFAULT_VIEW_WIDTH = 5.0F;
    private static final float MIN_VIEW_WIDTH = 0.25F;
    private static final float MAX_VIEW_WIDTH = 16384.0F;
    private static final float MAX_PAN = 8192.0F;

    public static GlassPreviewCameraState defaults() {
        return new GlassPreviewCameraState(0.0F, 0.0F, DEFAULT_VIEW_WIDTH, 0.0F, 0.0F);
    }

    public GlassPreviewCameraState normalized() {
        return new GlassPreviewCameraState(
                wrap(finiteOr(yawOffset, 0.0F)),
                clamp(finiteOr(pitchOffset, 0.0F), -180.0F, 180.0F),
                clamp(finiteOr(zoom, DEFAULT_VIEW_WIDTH), MIN_VIEW_WIDTH, MAX_VIEW_WIDTH),
                clamp(finiteOr(panX, 0.0F), -MAX_PAN, MAX_PAN),
                clamp(finiteOr(panY, 0.0F), -MAX_PAN, MAX_PAN));
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float wrap(float value) {
        float wrapped = value % 360.0F;
        if (wrapped <= -180.0F) wrapped += 360.0F;
        if (wrapped > 180.0F) wrapped -= 360.0F;
        return wrapped;
    }
}
