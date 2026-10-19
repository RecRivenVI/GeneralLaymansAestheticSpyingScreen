package io.github.recrivenvi.glass.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class GlassNodeDistanceTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void calculatesStraightLineDistanceInTheSameDimension() {
        GlobalPos origin = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        GlobalPos target = GlobalPos.of(Level.OVERWORLD, new BlockPos(3, 4, 12));

        assertEquals(13.0, GlassNodeDistance.between(origin, target));
    }

    @Test
    void sortsNearbyNodesBeforeFarAndCrossDimensionNodes() {
        GlobalPos origin = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        GlobalPos near = GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 1, 1));
        GlobalPos far = GlobalPos.of(Level.OVERWORLD, new BlockPos(30, 0, 0));
        GlobalPos crossDimension = GlobalPos.of(Level.NETHER, BlockPos.ZERO);
        ArrayList<GlobalPos> values = new ArrayList<>(List.of(crossDimension, far, near));

        values.sort(GlassNodeDistance.comparator(origin, value -> value));

        assertEquals(List.of(near, far, crossDimension), values);
        assertTrue(Double.isInfinite(GlassNodeDistance.between(origin, crossDimension)));
    }
}
