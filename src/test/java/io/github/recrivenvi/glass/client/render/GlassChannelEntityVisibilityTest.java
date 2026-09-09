package io.github.recrivenvi.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GlassChannelEntityVisibilityTest {
    @Test
    void treatsFirstPersonCameraAsDetachedOnlyDuringRemoteExtraction() {
        assertFalse(GlassChannelEntityVisibility.isDetachedForExtraction(false, false));
        assertTrue(GlassChannelEntityVisibility.isDetachedForExtraction(false, true));
        assertTrue(GlassChannelEntityVisibility.isDetachedForExtraction(true, false));
        assertTrue(GlassChannelEntityVisibility.isDetachedForExtraction(true, true));
    }

    @Test
    void bypassesSectionVisibilityOnlyForTheRemoteLocalPlayer() {
        assertFalse(GlassChannelEntityVisibility.isVisibleForExtraction(false, true, false));
        assertFalse(GlassChannelEntityVisibility.isVisibleForExtraction(false, false, true));
        assertTrue(GlassChannelEntityVisibility.isVisibleForExtraction(false, true, true));
        assertTrue(GlassChannelEntityVisibility.isVisibleForExtraction(true, false, false));
    }
}
