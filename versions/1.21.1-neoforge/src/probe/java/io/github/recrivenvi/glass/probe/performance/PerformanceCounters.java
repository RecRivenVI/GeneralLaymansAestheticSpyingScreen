package io.github.recrivenvi.glass.probe.performance;

import java.util.concurrent.atomic.LongAdder;

/** Measurement-only counters; this class has no client references and also loads on a server. */
public final class PerformanceCounters {
    public static final LongAdder serverNanos = new LongAdder(), serverTicks = new LongAdder();
    public static final LongAdder compileNanos = new LongAdder(), compileCount = new LongAdder();
    public static final LongAdder outboundBytes = new LongAdder(), inboundBytes = new LongAdder();
    public static final java.util.concurrent.ConcurrentLinkedQueue<Long> tickSamples =
            new java.util.concurrent.ConcurrentLinkedQueue<>();
    public static volatile boolean measuring;
    public static final java.util.concurrent.ConcurrentLinkedQueue<Long> tickCpuSamples =
            new java.util.concurrent.ConcurrentLinkedQueue<>();
    public static final LongAdder sourceCompiles = new LongAdder(), mainCompiles = new LongAdder();
    public static final LongAdder sodiumCompiles = new LongAdder(),
            sodiumCompileNanos = new LongAdder();
    public static final LongAdder sodiumSourceCompiles = new LongAdder();

    private PerformanceCounters() {}

    public static void tick(long nanos, long cpuNanos) {
        if (!measuring) return;
        serverNanos.add(nanos);
        serverTicks.increment();
        tickSamples.add(nanos);
        tickCpuSamples.add(cpuNanos);
    }

    public static void reset() {
        tickCpuSamples.clear();
        sourceCompiles.reset();
        mainCompiles.reset();
        sodiumCompiles.reset();
        sodiumCompileNanos.reset();
        sodiumSourceCompiles.reset();
        serverNanos.reset();
        serverTicks.reset();
        compileNanos.reset();
        compileCount.reset();
        outboundBytes.reset();
        inboundBytes.reset();
        tickSamples.clear();
    }
}
