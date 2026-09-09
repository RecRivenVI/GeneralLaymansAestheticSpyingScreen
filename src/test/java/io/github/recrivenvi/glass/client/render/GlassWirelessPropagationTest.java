package io.github.recrivenvi.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class GlassWirelessPropagationTest {
    @Test
    void openingAndClosingWavesStartAtTheProjectorSource() {
        double source = GlassWirelessPropagation.sourceFirstStartTime(100.0, 0, 2);
        double middle = GlassWirelessPropagation.sourceFirstStartTime(100.0, 64, 2);
        double farEnd = GlassWirelessPropagation.sourceFirstStartTime(100.0, 254, 2);

        assertEquals(100.0, source);
        assertTrue(source < middle);
        assertTrue(middle < farEnd);
        assertEquals(608.0, farEnd);
    }
}
