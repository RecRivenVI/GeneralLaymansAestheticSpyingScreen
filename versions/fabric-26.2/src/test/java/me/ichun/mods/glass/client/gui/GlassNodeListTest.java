package me.ichun.mods.glass.client.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GlassNodeListTest {
    @Test
    void clippedPartOfARowCannotHoverThroughControlsInFrontOfIt() {
        assertTrue(GlassNodeList.isVisibleRowHovered(50, 79, 10, 20, 100, 80, 70, 32));
        assertFalse(GlassNodeList.isVisibleRowHovered(50, 85, 10, 20, 100, 80, 70, 32));
    }
}
