package io.github.recrivenvi.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

final class GlassLiveCapturePositionsTest {
    static {
        io.github.recrivenvi.glass.ForgeTestBootstrap.initialize();
    }

    @Test
    void keepsClosingSurfacePositionsWhenNoProjectionIsStillActive() {
        BlockPos closing = new BlockPos(4, 5, 6);

        assertEquals(
                List.of(closing),
                GlassLiveCapturePositions.combine(List.of(), List.of(closing), List.of()));
    }

    @Test
    void keepsClosingWirelessGeometryAndDeduplicatesSharedPositions() {
        BlockPos projector = new BlockPos(1, 2, 3);
        BlockPos wireless = new BlockPos(7, 8, 9);

        assertEquals(
                List.of(projector, wireless),
                GlassLiveCapturePositions.combine(
                        List.of(projector), List.of(), List.of(projector, wireless)));
    }
}
