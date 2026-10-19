package io.github.recrivenvi.glass.client.render;

final class GlassWirelessPropagation {
    private GlassWirelessPropagation() {}

    static double sourceFirstStartTime(double now, int triangleIndex, int propagationDelayTicks) {
        return now + (double) triangleIndex * propagationDelayTicks;
    }
}
