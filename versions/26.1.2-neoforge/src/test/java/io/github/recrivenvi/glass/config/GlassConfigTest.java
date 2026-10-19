package io.github.recrivenvi.glass.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class GlassConfigTest {
    @Test
    void defaultsMatchThePublishedConfigurationContract() {
        GlassConfig.Values defaults = GlassConfig.Values.defaults();

        assertEquals(64, defaults.propagationLimit());
        assertEquals(2, defaults.propagationDelayTicks());
        assertEquals(8, defaults.surfaceAnimationTicks());
        assertEquals(256, defaults.observationDistance());
        assertEquals(2, defaults.wirelessMinNodes());
        assertEquals(256, defaults.wirelessMaxNodes());
        assertEquals(64, defaults.channelNameLength());
        assertEquals(8, defaults.channelRenderDistance());
        assertFalse(defaults.legacyFadeAnimation());
    }

    @Test
    void normalizationClampsEveryValueAndKeepsWirelessRangeOrdered() {
        GlassConfig.Values normalized =
                new GlassConfig.Values(
                                Integer.MAX_VALUE,
                                Integer.MIN_VALUE,
                                Integer.MAX_VALUE,
                                Integer.MIN_VALUE,
                                5000,
                                1,
                                0,
                                500,
                                true)
                        .normalized();

        assertEquals(GlassConfig.MAX_PROPAGATION_LIMIT, normalized.propagationLimit());
        assertEquals(GlassConfig.MIN_PROPAGATION_DELAY_TICKS, normalized.propagationDelayTicks());
        assertEquals(GlassConfig.MAX_SURFACE_ANIMATION_TICKS, normalized.surfaceAnimationTicks());
        assertEquals(GlassConfig.MIN_OBSERVATION_DISTANCE, normalized.observationDistance());
        assertEquals(GlassConfig.MAX_WIRELESS_NODES, normalized.wirelessMinNodes());
        assertEquals(GlassConfig.MAX_WIRELESS_NODES, normalized.wirelessMaxNodes());
        assertEquals(GlassConfig.MIN_CHANNEL_NAME_LENGTH, normalized.channelNameLength());
        assertEquals(GlassConfig.MAX_CHANNEL_RENDER_DISTANCE, normalized.channelRenderDistance());
    }
}
