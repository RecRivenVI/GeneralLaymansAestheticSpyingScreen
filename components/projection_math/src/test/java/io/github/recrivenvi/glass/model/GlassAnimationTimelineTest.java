package io.github.recrivenvi.glass.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GlassAnimationTimelineTest {
    @Test
    void interruptedOpeningPreservesVelocityBeforeReversingSmoothly() {
        GlassAnimationTimeline opening = GlassAnimationTimeline.startFull(true, 0.0, 8);
        GlassAnimationTimeline closing = opening.interrupt(false, 3.0, 8);
        double turningProgress = opening.progress(3.0);

        assertEquals(turningProgress, closing.progress(3.0), 1.0e-6);
        assertEquals(opening.velocity(3.0), closing.velocity(3.0), 1.0e-6);
        assertTrue(closing.progress(3.1) > turningProgress);
        assertEquals(0.0, closing.progress(11.0), 1.0e-6);
    }

    @Test
    void futureInterruptionKeepsTheCurrentAnimationMovingUntilItsStart() {
        GlassAnimationTimeline opening = GlassAnimationTimeline.startFull(true, 0.0, 8);
        GlassAnimationTimeline closing = opening.interrupt(false, 6.0, 8);

        assertEquals(opening.progress(4.0), closing.progress(4.0), 1.0e-6);
        assertEquals(opening.progress(6.0), closing.progress(6.0), 1.0e-6);
    }

    @Test
    void channelSwitchCanShowNewNearFacesWhileFarFacesStillShowTheOldChannel() {
        GlassAnimationTimeline nearOld = GlassAnimationTimeline.startFull(false, 0.0, 8);
        GlassAnimationTimeline nearNew =
                GlassAnimationTimeline.startFull(true, nearOld.endTime(), 8);
        GlassAnimationTimeline farOld = GlassAnimationTimeline.startFull(false, 6.0, 8);

        assertTrue(nearNew.progress(10.0) > 0.0);
        assertTrue(farOld.progress(10.0) > 0.0);
        assertEquals(0.0, nearOld.progress(10.0), 1.0e-6);
    }

    @Test
    void reopeningWaveDoesNotWaitForTheClosingWaveToFinish() {
        GlassAnimationTimeline nearClosing = GlassAnimationTimeline.startFull(false, 0.0, 8);
        GlassAnimationTimeline farClosing = GlassAnimationTimeline.startFull(false, 6.0, 8);
        GlassAnimationTimeline nearReopening = nearClosing.interrupt(true, 4.0, 8);
        GlassAnimationTimeline farReopening = farClosing.interrupt(true, 10.0, 8);

        assertTrue(nearReopening.progress(8.0) > nearClosing.progress(8.0));
        assertEquals(farClosing.progress(8.0), farReopening.progress(8.0), 1.0e-6);
        assertEquals(farClosing.velocity(10.0), farReopening.velocity(10.0), 1.0e-6);
    }

    @Test
    void latestStartTimeIdentifiesTheNewlyScheduledHandoffSegment() {
        GlassAnimationTimeline opening = GlassAnimationTimeline.startFull(true, 0.0, 8);
        GlassAnimationTimeline handoff = opening.interrupt(false, 6.0, 12);

        assertEquals(6.0, handoff.latestStartTime(), 1.0e-6);
        assertEquals(18.0, handoff.endTime(), 1.0e-6);
    }
}
