package io.github.recrivenvi.glass.model;

public final class GlassWirelessPropagation {
    private GlassWirelessPropagation() {}

    public static double sourceFirstStartTime(
            double now, int triangleIndex, int propagationDelayTicks) {
        return now + (double) triangleIndex * propagationDelayTicks;
    }
}
