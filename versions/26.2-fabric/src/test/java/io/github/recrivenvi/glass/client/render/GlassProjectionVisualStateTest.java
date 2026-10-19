package io.github.recrivenvi.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GlassProjectionVisualStateTest {
    @Test
    void wirelessProjectorNeverOwnsTheProjectorBlockFace() {
        assertTrue(GlassProjectionVisualState.usesBlockSurfaces(true, 0));
        assertFalse(GlassProjectionVisualState.usesBlockSurfaces(true, 1));
        assertFalse(GlassProjectionVisualState.usesBlockSurfaces(true, 3));
        assertTrue(GlassProjectionVisualState.usesBlockSurfaces(false, 3));
    }

    @Test
    void openingUsesCubicEaseOut() {
        assertEquals(0.0, GlassProjectionVisualState.progress(0.0, true, 0.0, 8), 1.0e-6);
        assertEquals(0.875, GlassProjectionVisualState.progress(0.0, true, 4.0, 8), 1.0e-6);
        assertEquals(1.0, GlassProjectionVisualState.progress(0.0, true, 8.0, 8), 1.0e-6);
    }

    @Test
    void progressStaysAtItsEndpointOutsideTheFullAnimationWindow() {
        assertEquals(0.0, GlassProjectionVisualState.progress(0.0, true, -2.0, 8), 1.0e-6);
        assertEquals(1.0, GlassProjectionVisualState.progress(0.0, true, 8.0, 8), 1.0e-6);
        assertEquals(0.0, GlassProjectionVisualState.progress(1.0, false, 8.0, 8), 1.0e-6);
    }

    @Test
    void activeChannelSwitchRequiresTwoDifferentLiveChannels() {
        assertTrue(
                GlassProjectionVisualState.isHotChannelSwitch(
                        true, "channel-a", true, "channel-b"));
        assertFalse(
                GlassProjectionVisualState.isHotChannelSwitch(
                        false, "channel-a", true, "channel-b"));
        assertFalse(
                GlassProjectionVisualState.isHotChannelSwitch(
                        true, "channel-a", true, "channel-a"));
    }
}
