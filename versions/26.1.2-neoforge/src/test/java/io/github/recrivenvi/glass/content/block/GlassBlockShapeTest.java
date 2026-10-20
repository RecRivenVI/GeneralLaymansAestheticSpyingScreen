package io.github.recrivenvi.glass.content.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void completeBlocksKeepNativeFullSelectionAndCollision() {
        java.util.List<net.minecraft.world.level.block.Block> blocks =
                java.util.List.of(
                        io.github.recrivenvi.glass.content.GlassContent.PROJECTOR.get(),
                        io.github.recrivenvi.glass.content.GlassContent.EXTENSION.get(),
                        io.github.recrivenvi.glass.content.GlassContent.WIRELESS_EXTENSION.get(),
                        io.github.recrivenvi.glass.content.GlassContent.TERMINAL.get());
        for (var block : blocks) {
            for (var state : block.getStateDefinition().getPossibleStates()) {
                var world = net.minecraft.world.level.EmptyBlockGetter.INSTANCE;
                var pos = net.minecraft.core.BlockPos.ZERO;
                assertTrue(
                        net.minecraft.world.level.block.Block.isShapeFullBlock(
                                state.getShape(world, pos)));
                assertTrue(
                        net.minecraft.world.level.block.Block.isShapeFullBlock(
                                state.getCollisionShape(world, pos)));
                assertFalse(block instanceof net.minecraft.world.level.block.EntityBlock);
            }
        }
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
