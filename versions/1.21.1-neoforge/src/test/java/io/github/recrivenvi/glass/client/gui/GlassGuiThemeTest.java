package io.github.recrivenvi.glass.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class GlassGuiThemeTest {
    @Test
    void largePanelExpandsWithAvailableScreenSpaceUpToItsReadableLimit() {
        GlassGuiTheme.Layout minimum = GlassGuiTheme.centered(600, 360, true);
        GlassGuiTheme.Layout expanded = GlassGuiTheme.centered(960, 540, true);
        GlassGuiTheme.Layout maximum = GlassGuiTheme.centered(1600, 1000, true);

        assertEquals(560, minimum.width());
        assertEquals(300, minimum.height());
        assertEquals(787, expanded.width());
        assertEquals(443, expanded.height());
        assertEquals(GlassGuiTheme.LARGE_PANEL_MAX_WIDTH, maximum.width());
        assertEquals(GlassGuiTheme.LARGE_PANEL_MAX_HEIGHT, maximum.height());
    }

    @Test
    void ordinaryScreensKeepTheirCompactPanelSize() {
        GlassGuiTheme.Layout compact = GlassGuiTheme.centered(1600, 1000, false);

        assertEquals(GlassGuiTheme.PANEL_WIDTH, compact.width());
        assertEquals(GlassGuiTheme.PANEL_HEIGHT, compact.height());
    }
}
