package io.github.recrivenvi.glass.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GlassChannelRulesTest {
    @Test
    void createsLegacyCompatiblePublicAndPrivateChannels() {
        assertEquals("public:Lobby", GlassChannelRules.create("Alice", "Lobby", true));
        assertEquals("Alice:Lobby", GlassChannelRules.create("Alice", "Lobby", false));
        assertEquals("", GlassChannelRules.create("Alice", "", false));
    }

    @Test
    void sanitizesDelimiterControlsAndLegacyLengthLimit() {
        assertEquals("abcdefghijklmno", GlassChannelRules.sanitizeName("abcdefghijklmnoqrst"));
        assertEquals("ab", GlassChannelRules.sanitizeName("a:\u0001b"));
    }

    @Test
    void enforcesOwnershipWhileLeavingPublicChannelsAccessible() {
        assertTrue(GlassChannelRules.canAccess("public:Lobby", "Bob"));
        assertTrue(GlassChannelRules.canAccess("Alice:Lobby", "alice"));
        assertFalse(GlassChannelRules.canAccess("Alice:Lobby", "Bob"));
        assertFalse(GlassChannelRules.canAccess("malformed", "Alice"));
    }
}
