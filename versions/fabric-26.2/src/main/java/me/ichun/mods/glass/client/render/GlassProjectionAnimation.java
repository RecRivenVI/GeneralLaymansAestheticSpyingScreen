package me.ichun.mods.glass.client.render;

/** 在现代缩放与上游十二 tick 白色遮罩动画之间切换。 */
final class GlassProjectionAnimation {
    static final int LEGACY_FADE_TICKS = 12;

    private GlassProjectionAnimation() {}

    static int duration(boolean legacyFade, int scaleDuration) {
        return legacyFade ? LEGACY_FADE_TICKS : scaleDuration;
    }

    static Frame stable() {
        return new Frame(1.0, 0.0F, 1.0F);
    }

    static Frame frame(boolean legacyFade, GlassAnimationTimeline timeline, double now) {
        if (!legacyFade) {
            return new Frame(timeline.progress(now), 0.0F, 1.0F);
        }
        if (!timeline.started(now)) {
            return timeline.opening(now) ? Frame.HIDDEN : stable();
        }
        float fade = (float) Math.sqrt(1.0 - timeline.segmentAmount(now));
        return timeline.opening(now) ? new Frame(1.0, fade, 1.0F) : new Frame(1.0, 1.0F, fade);
    }

    /** 白色遮罩模式在遮罩第一次完全覆盖投影时交接频道；缩放模式仍等待旧画面完整收回。 */
    static double channelHandoffTime(boolean legacyFade, GlassAnimationTimeline outgoing) {
        return legacyFade ? outgoing.latestStartTime() : outgoing.endTime();
    }

    /** 交接前继续显示真实旧投影，交接瞬间由完全不透明的新频道白色遮罩接管。 */
    static Frame outgoingChannelFrame(
            boolean legacyFade, GlassAnimationTimeline timeline, double now) {
        if (!legacyFade) return frame(false, timeline, now);
        return now < timeline.latestStartTime() ? frame(true, timeline, now) : Frame.HIDDEN;
    }

    record Frame(double geometryScale, float whiteMix, float alpha) {
        private static final Frame HIDDEN = new Frame(0.0, 0.0F, 0.0F);

        boolean visible() {
            return geometryScale > 0.0 && alpha > 0.00625F;
        }
    }
}
