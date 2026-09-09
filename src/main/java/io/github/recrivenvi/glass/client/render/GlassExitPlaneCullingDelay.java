package io.github.recrivenvi.glass.client.render;

/** 让出口裁切状态等待受影响面的关闭动画完成后再切换。 */
final class GlassExitPlaneCullingDelay {
    private GlassExitPlaneCullingDelay() {}

    static State update(
            State existing, boolean displayed, boolean target, double closingEnd, double now) {
        if (displayed == target) return null;
        if (closingEnd > now) {
            double until =
                    existing != null && existing.enabled() == displayed
                            ? Math.max(closingEnd, existing.until())
                            : closingEnd;
            return new State(displayed, until);
        }
        return existing != null && existing.active(now) ? existing : null;
    }

    record State(boolean enabled, double until) {
        boolean active(double now) {
            return now < until;
        }
    }
}
