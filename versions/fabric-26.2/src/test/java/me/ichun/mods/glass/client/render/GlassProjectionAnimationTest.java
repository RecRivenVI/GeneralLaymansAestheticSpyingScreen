package me.ichun.mods.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GlassProjectionAnimationTest {
    @Test
    void scaleModeKeepsConfiguredDurationAndCurrentGeometryAnimation() {
        GlassAnimationTimeline timeline = GlassAnimationTimeline.startFull(true, 0.0, 8);

        assertEquals(8, GlassProjectionAnimation.duration(false, 8));
        GlassProjectionAnimation.Frame frame = GlassProjectionAnimation.frame(false, timeline, 4.0);
        assertEquals(0.875, frame.geometryScale(), 0.000001);
        assertEquals(0.0F, frame.whiteMix(), 0.000001F);
        assertEquals(1.0F, frame.alpha(), 0.000001F);
    }

    @Test
    void legacyOpeningUsesUpstreamTwelveTickSquareRootWhiteReveal() {
        GlassAnimationTimeline timeline = GlassAnimationTimeline.startFull(true, 2.0, 12);

        assertEquals(12, GlassProjectionAnimation.duration(true, 8));
        assertFalse(GlassProjectionAnimation.frame(true, timeline, 1.0).visible());
        GlassProjectionAnimation.Frame start = GlassProjectionAnimation.frame(true, timeline, 2.0);
        GlassProjectionAnimation.Frame midpoint =
                GlassProjectionAnimation.frame(true, timeline, 8.0);
        assertEquals(1.0, start.geometryScale(), 0.000001);
        assertEquals(1.0F, start.whiteMix(), 0.000001F);
        assertEquals(Math.sqrt(0.5), midpoint.whiteMix(), 0.000001);
        assertEquals(1.0F, midpoint.alpha(), 0.000001F);
    }

    @Test
    void legacyClosingShowsOnlyTheFadingWhitePlane() {
        GlassAnimationTimeline timeline = GlassAnimationTimeline.startFull(false, 2.0, 12);

        GlassProjectionAnimation.Frame waiting =
                GlassProjectionAnimation.frame(true, timeline, 1.0);
        GlassProjectionAnimation.Frame midpoint =
                GlassProjectionAnimation.frame(true, timeline, 8.0);
        assertEquals(0.0F, waiting.whiteMix(), 0.000001F);
        assertEquals(1.0F, waiting.alpha(), 0.000001F);
        assertEquals(1.0F, midpoint.whiteMix(), 0.000001F);
        assertEquals(Math.sqrt(0.5), midpoint.alpha(), 0.000001);
    }

    @Test
    void legacyChannelSwitchHandsOffExactlyWhenTheNewWhiteMaskAppears() {
        GlassAnimationTimeline outgoing = GlassAnimationTimeline.startFull(false, 6.0, 12);
        double handoff = GlassProjectionAnimation.channelHandoffTime(true, outgoing);
        GlassAnimationTimeline incoming = GlassAnimationTimeline.startFull(true, handoff, 12);

        assertEquals(6.0, handoff, 0.000001);
        assertTrue(GlassProjectionAnimation.outgoingChannelFrame(true, outgoing, 5.999).visible());
        assertFalse(
                GlassProjectionAnimation.outgoingChannelFrame(true, outgoing, handoff).visible());
        GlassProjectionAnimation.Frame mask =
                GlassProjectionAnimation.frame(true, incoming, handoff);
        assertTrue(mask.visible());
        assertEquals(1.0F, mask.whiteMix(), 0.000001F);
        assertEquals(1.0F, mask.alpha(), 0.000001F);
    }

    @Test
    void scaleChannelSwitchStillWaitsForTheOldGeometryToClose() {
        GlassAnimationTimeline outgoing = GlassAnimationTimeline.startFull(false, 6.0, 8);

        assertEquals(14.0, GlassProjectionAnimation.channelHandoffTime(false, outgoing), 0.000001);
        assertTrue(GlassProjectionAnimation.outgoingChannelFrame(false, outgoing, 10.0).visible());
    }
}
