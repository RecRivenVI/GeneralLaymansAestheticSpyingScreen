package io.github.recrivenvi.glass.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GlassTopologyUpdateQueueTest {
    @Test
    void coalescesEveryUpdateInOneTickIntoOneRebuild() {
        GlassTopologyUpdateQueue queue = new GlassTopologyUpdateQueue();

        queue.mark();
        queue.mark();
        queue.mark();

        assertTrue(queue.consume());
        assertFalse(queue.consume());
    }

    @Test
    void immediateSnapshotRebuildClearsQueuedWork() {
        GlassTopologyUpdateQueue queue = new GlassTopologyUpdateQueue();
        queue.mark();

        queue.clear();

        assertFalse(queue.consume());
    }
}
