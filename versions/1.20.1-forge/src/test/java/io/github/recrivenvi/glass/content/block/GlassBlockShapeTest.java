package io.github.recrivenvi.glass.content.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class GlassBlockShapeTest {
    static {
        io.github.recrivenvi.glass.ForgeTestBootstrap.initialize();
    }

    private static final double EPSILON = 1.0E-12;

    @BeforeAll
    static void bootstrapMinecraft() {
        io.github.recrivenvi.glass.ForgeTestBootstrap.initialize();
    }

    @Test
    void projectionSeriesUsesCenteredEightPixelCores() {
        assertBounds(GlassConnectedBlock.coreShape().bounds(), 4, 4, 4, 12, 12, 12);
        assertBounds(GlassWirelessExtensionBlock.shape().bounds(), 4, 4, 4, 12, 12, 12);
    }

    @Test
    void everyWiredConnectionIsSixBySixByFourPixels() {
        for (Direction direction : Direction.values()) {
            AABB bounds = GlassConnectedBlock.connectorShape(direction).bounds();
            double alongX = pixels(bounds.maxX - bounds.minX);
            double alongY = pixels(bounds.maxY - bounds.minY);
            double alongZ = pixels(bounds.maxZ - bounds.minZ);
            long sixPixelAxes =
                    java.util.stream.Stream.of(alongX, alongY, alongZ)
                            .filter(length -> Math.abs(length - 6.0) < EPSILON)
                            .count();
            long fourPixelAxes =
                    java.util.stream.Stream.of(alongX, alongY, alongZ)
                            .filter(length -> Math.abs(length - 4.0) < EPSILON)
                            .count();
            assertEquals(2, sixPixelAxes, direction + " cross-section");
            assertEquals(1, fourPixelAxes, direction + " half length");
        }
    }

    @Test
    void attachmentSupportsAreSixBySixByFourPixels() {
        for (Direction direction : Direction.values()) {
            AABB bounds = GlassAttachmentSupport.shape(direction).bounds();
            java.util.List<Double> dimensions =
                    java.util.stream.Stream.of(
                                    pixels(bounds.maxX - bounds.minX),
                                    pixels(bounds.maxY - bounds.minY),
                                    pixels(bounds.maxZ - bounds.minZ))
                            .sorted()
                            .toList();
            assertEquals(java.util.List.of(4.0, 6.0, 6.0), dimensions, direction.toString());
        }
    }

    @Test
    void terminalUsesEightPixelCoreAndBothMarkersUseRequestedSizes() {
        assertBounds(GlassTerminalBlock.coreShape().bounds(), 4, 4, 4, 12, 12, 12);
        assertBounds(GlassTerminalBlock.frontMarker(Direction.NORTH).bounds(), 5, 5, 3, 11, 11, 4);
        assertBounds(GlassProjectorBlock.frontMarker(Direction.NORTH).bounds(), 5, 5, 3, 11, 11, 4);
    }

    @Test
    void projectorRejectsWiredConnectionOnlyOnItsFrontFace() {
        assertFalse(
                GlassProjectorBlock.allowsWiredConnection(Direction.NORTH, Direction.NORTH, true));
        assertTrue(
                GlassProjectorBlock.allowsWiredConnection(Direction.NORTH, Direction.SOUTH, true));
        assertFalse(
                GlassProjectorBlock.allowsWiredConnection(Direction.NORTH, Direction.SOUTH, false));
    }

    @Test
    void smallProjectionBlocksStillOfferFullAttachmentSupport() {
        assertEquals(
                net.minecraft.world.phys.shapes.Shapes.block(),
                GlassConnectedBlock.attachmentSupportShape());
    }

    @Test
    void compactConnectionStateEliminatesImpossibleWiredSupportPairs() {
        int connectionStates = 1;
        assertEquals(6, GlassConnectedBlock.PROPERTY_BY_DIRECTION.size());
        for (Direction direction : Direction.values()) {
            var property = GlassConnectedBlock.PROPERTY_BY_DIRECTION.get(direction);
            assertEquals(direction.getSerializedName(), property.getName());
            assertEquals(3, property.getPossibleValues().size(), direction.toString());
            assertTrue(property.getPossibleValues().contains(GlassConnectedBlock.Connection.NONE));
            assertTrue(property.getPossibleValues().contains(GlassConnectedBlock.Connection.WIRED));
            assertTrue(
                    property.getPossibleValues().contains(GlassConnectedBlock.Connection.SUPPORT));
            connectionStates *= property.getPossibleValues().size();
        }
        assertEquals(729, connectionStates);
        assertEquals(
                8_748,
                connectionStates * GlassProjectorBlock.FACING.getPossibleValues().size() * 2);
    }

    @Test
    void baseShapesAreCachedOnlyByTheSixPhysicalConnectorAxes() {
        EnumMap<Direction, GlassConnectedBlock.Connection> wired = new EnumMap<>(Direction.class);
        wired.put(Direction.NORTH, GlassConnectedBlock.Connection.WIRED);
        EnumMap<Direction, GlassConnectedBlock.Connection> supported =
                new EnumMap<>(Direction.class);
        supported.put(Direction.NORTH, GlassConnectedBlock.Connection.SUPPORT);

        assertEquals(64, GlassConnectedBlock.cachedShapeCount());
        assertSame(
                GlassConnectedBlock.cachedShape(wired), GlassConnectedBlock.cachedShape(supported));
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

    private static void assertBounds(
            AABB bounds,
            double minX,
            double minY,
            double minZ,
            double maxX,
            double maxY,
            double maxZ) {
        assertEquals(minX, pixels(bounds.minX), EPSILON);
        assertEquals(minY, pixels(bounds.minY), EPSILON);
        assertEquals(minZ, pixels(bounds.minZ), EPSILON);
        assertEquals(maxX, pixels(bounds.maxX), EPSILON);
        assertEquals(maxY, pixels(bounds.maxY), EPSILON);
        assertEquals(maxZ, pixels(bounds.maxZ), EPSILON);
    }

    private static double pixels(double blocks) {
        return blocks * 16.0;
    }
}
