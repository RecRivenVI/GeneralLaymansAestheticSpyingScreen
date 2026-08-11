package me.ichun.mods.glass.client.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 单个投影面的动画时间线；新传播波抵达时从当前运动状态平滑反转。 */
final class GlassAnimationTimeline {
    private final List<Segment> segments;

    private GlassAnimationTimeline(List<Segment> segments) {
        this.segments = List.copyOf(segments);
    }

    static GlassAnimationTimeline startFull(boolean opening, double startTime, int duration) {
        return new GlassAnimationTimeline(List.of(Segment.full(opening, startTime, duration)));
    }

    GlassAnimationTimeline interrupt(boolean opening, double startTime, int duration) {
        double startProgress = progress(startTime);
        double startVelocity = velocity(startTime);
        ArrayList<Segment> updated = new ArrayList<>();
        for (Segment segment : segments) {
            if (segment.startTime() < startTime) updated.add(segment);
        }
        updated.add(
                new Segment(
                        startProgress, opening ? 1.0 : 0.0, startTime, duration, startVelocity));
        updated.sort(Comparator.comparingDouble(Segment::startTime));
        return new GlassAnimationTimeline(updated);
    }

    double progress(double now) {
        Segment active = activeSegment(now);
        if (now < active.startTime()) return active.startProgress();
        double amount = clamp((now - active.startTime()) / active.duration());
        if (Double.isNaN(active.startVelocity())) {
            double eased = 1.0 - Math.pow(1.0 - amount, 3.0);
            return active.startProgress()
                    + (active.targetProgress() - active.startProgress()) * eased;
        }
        double startTangent = active.startVelocity() * active.duration();
        double amountSquared = amount * amount;
        double amountCubed = amountSquared * amount;
        double startWeight = 2.0 * amountCubed - 3.0 * amountSquared + 1.0;
        double tangentWeight = amountCubed - 2.0 * amountSquared + amount;
        double targetWeight = -2.0 * amountCubed + 3.0 * amountSquared;
        return clamp(
                startWeight * active.startProgress()
                        + tangentWeight * startTangent
                        + targetWeight * active.targetProgress());
    }

    double velocity(double now) {
        Segment active = activeSegment(now);
        if (now < active.startTime()) return 0.0;
        double amount = clamp((now - active.startTime()) / active.duration());
        if (amount >= 1.0) return 0.0;
        if (Double.isNaN(active.startVelocity())) {
            double remaining = 1.0 - amount;
            return (active.targetProgress() - active.startProgress())
                    * 3.0
                    * remaining
                    * remaining
                    / active.duration();
        }
        double startTangent = active.startVelocity() * active.duration();
        double amountSquared = amount * amount;
        double derivative =
                (6.0 * amountSquared - 6.0 * amount) * active.startProgress()
                        + (3.0 * amountSquared - 4.0 * amount + 1.0) * startTangent
                        + (-6.0 * amountSquared + 6.0 * amount) * active.targetProgress();
        return derivative / active.duration();
    }

    boolean finished(double now) {
        return now >= endTime();
    }

    double endTime() {
        Segment last = segments.getLast();
        return last.startTime() + last.duration();
    }

    double latestStartTime() {
        return segments.getLast().startTime();
    }

    boolean opening(double now) {
        return activeSegment(now).targetProgress() == 1.0;
    }

    boolean started(double now) {
        return now >= activeSegment(now).startTime();
    }

    double segmentAmount(double now) {
        Segment active = activeSegment(now);
        return clamp((now - active.startTime()) / active.duration());
    }

    private Segment activeSegment(double now) {
        Segment active = segments.getFirst();
        for (Segment segment : segments) {
            if (segment.startTime() > now) break;
            active = segment;
        }
        return active;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record Segment(
            double startProgress,
            double targetProgress,
            double startTime,
            int duration,
            double startVelocity) {
        private static Segment full(boolean opening, double startTime, int duration) {
            return new Segment(
                    opening ? 0.0 : 1.0, opening ? 1.0 : 0.0, startTime, duration, Double.NaN);
        }
    }
}
