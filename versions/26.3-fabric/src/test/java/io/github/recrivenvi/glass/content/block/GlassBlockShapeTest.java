package io.github.recrivenvi.glass.content.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class GlassBlockShapeTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void onlyProjectorAndTerminalStoreSixWayFacing() {
        assertEquals(true, GlassChannelBlock.class.isAssignableFrom(GlassTerminalBlock.class));
        assertEquals(false, GlassChannelBlock.class.isAssignableFrom(GlassExtensionBlock.class));
        assertEquals(
                false, GlassChannelBlock.class.isAssignableFrom(GlassWirelessExtensionBlock.class));
        assertEquals(6, GlassProjectorBlock.FACING.getPossibleValues().size());
        assertEquals(6, GlassChannelBlock.FACING.getPossibleValues().size());
    }
}
