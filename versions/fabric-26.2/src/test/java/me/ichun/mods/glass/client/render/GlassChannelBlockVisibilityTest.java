package me.ichun.mods.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GlassChannelBlockVisibilityTest {
    @Test
    void hidesOnlyTerminalAndOnlyInsideChannelSnapshots() {
        assertTrue(GlassChannelBlockVisibility.shouldHide(true, true));
        assertFalse(GlassChannelBlockVisibility.shouldHide(true, false));
        assertFalse(GlassChannelBlockVisibility.shouldHide(false, true));
    }
}
