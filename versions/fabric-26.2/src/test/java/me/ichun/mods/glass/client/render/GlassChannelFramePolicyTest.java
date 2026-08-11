package me.ichun.mods.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GlassChannelFramePolicyTest {
    @Test
    void reservesSharedPostChainsForTheMainWorld() {
        assertTrue(GlassChannelFramePolicy.mayUseSharedPostChains(false));
        assertFalse(GlassChannelFramePolicy.mayUseSharedPostChains(true));
    }
}
