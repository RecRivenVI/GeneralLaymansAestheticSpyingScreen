package io.github.recrivenvi.glass.probe.performance;

public final class PerformanceTimeline {
    public static final java.util.concurrent.ConcurrentLinkedQueue<java.util.Map<String, Object>>
            EVENTS = new java.util.concurrent.ConcurrentLinkedQueue<>();

    private PerformanceTimeline() {}

    public static void record(String kind, Object details) {
        EVENTS.add(
                java.util.Map.of(
                        "utcMillis", System.currentTimeMillis(), "kind", kind, "details", details));
    }
}
