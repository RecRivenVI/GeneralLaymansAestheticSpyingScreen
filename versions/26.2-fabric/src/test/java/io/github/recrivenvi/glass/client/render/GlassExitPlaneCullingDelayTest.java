package io.github.recrivenvi.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GlassExitPlaneCullingDelayTest {
    @Test
    void topologyClosureKeepsTheDisplayedCullingUntilItsAnimationEnds() {
        var state = GlassExitPlaneCullingDelay.update(null, false, true, 18.0, 10.0);

        assertFalse(state.enabled());
        assertTrue(state.active(17.99));
        assertFalse(state.active(18.0));
    }

    @Test
    void repeatedSnapshotDoesNotDiscardAnActiveDelay() {
        var existing = new GlassExitPlaneCullingDelay.State(true, 24.0);

        var state = GlassExitPlaneCullingDelay.update(existing, true, false, 12.0, 10.0);

        assertTrue(state.enabled());
        assertTrue(state.active(23.99));
    }

    @Test
    void matchingTargetRemovesTheDelayImmediately() {
        var existing = new GlassExitPlaneCullingDelay.State(true, 24.0);

        assertNull(GlassExitPlaneCullingDelay.update(existing, true, true, 20.0, 10.0));
    }
}
