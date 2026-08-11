package me.ichun.mods.glass.client.render;

/** 投影透明度不变；单段几何缩放使用三次缓出，段间顺序由动画时间线管理。 */
final class GlassProjectionVisualState {
    private GlassProjectionVisualState() {}

    static double progress(
            double startProgress, boolean opening, double elapsedTicks, int durationTicks) {
        double target = opening ? 1.0 : 0.0;
        if (durationTicks <= 0) return target;
        double amount = clamp(elapsedTicks / durationTicks);
        double eased = 1.0 - Math.pow(1.0 - amount, 3.0);
        return startProgress + (target - startProgress) * eased;
    }

    static boolean usesBlockSurfaces(boolean projector, int wirelessPositionCount) {
        return !projector || wirelessPositionCount == 0;
    }

    static boolean isHotChannelSwitch(
            boolean previousActive,
            String previousChannel,
            boolean currentActive,
            String currentChannel) {
        return previousActive && currentActive && !previousChannel.equals(currentChannel);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
