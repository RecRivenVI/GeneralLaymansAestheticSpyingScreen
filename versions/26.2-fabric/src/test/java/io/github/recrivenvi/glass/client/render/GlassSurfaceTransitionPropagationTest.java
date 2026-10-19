package io.github.recrivenvi.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

final class GlassSurfaceTransitionPropagationTest {
    @Test
    void addedBranchOpensFromRetainedBoundaryInsteadOfProjectorDistance() {
        var retained = surface(0, Direction.UP);
        var firstAdded = surface(1, Direction.UP);
        var secondAdded = surface(2, Direction.UP);

        var plan =
                GlassSurfaceTransitionPropagation.between(
                        Map.of(retained, 20),
                        Map.of(retained, 20, firstAdded, 21, secondAdded, 22));

        assertEquals(0, plan.openingDistances().get(firstAdded));
        assertEquals(1, plan.openingDistances().get(secondAdded));
    }

    @Test
    void removedBranchClosesFromCutBoundaryInsteadOfProjectorDistance() {
        var retained = surface(0, Direction.UP);
        var firstRemoved = surface(1, Direction.UP);
        var secondRemoved = surface(2, Direction.UP);

        var plan =
                GlassSurfaceTransitionPropagation.between(
                        Map.of(retained, 20, firstRemoved, 21, secondRemoved, 22),
                        Map.of(retained, 20));

        assertEquals(0, plan.closingDistances().get(firstRemoved));
        assertEquals(1, plan.closingDistances().get(secondRemoved));
    }

    @Test
    void localWaveUsesTheSameCornerAdjacencyAsTopologyPropagation() {
        var retained = new GlassSurfaceTransitionPropagation.Surface(BlockPos.ZERO, Direction.UP);
        var outerCorner =
                new GlassSurfaceTransitionPropagation.Surface(BlockPos.ZERO, Direction.NORTH);
        var innerCorner =
                new GlassSurfaceTransitionPropagation.Surface(
                        BlockPos.ZERO.north().above(), Direction.SOUTH);

        var plan =
                GlassSurfaceTransitionPropagation.between(
                        Map.of(retained, 7), Map.of(retained, 7, outerCorner, 8, innerCorner, 8));

        assertEquals(0, plan.openingDistances().get(outerCorner));
        assertEquals(0, plan.openingDistances().get(innerCorner));
    }

    @Test
    void completelyReplacedComponentFallsBackToProjectorDistance() {
        var oldSurface = surface(0, Direction.UP);
        var newSurface = surface(10, Direction.UP);

        var plan =
                GlassSurfaceTransitionPropagation.between(
                        Map.of(oldSurface, 3), Map.of(newSurface, 12));

        assertEquals(3, plan.closingDistances().get(oldSurface));
        assertEquals(12, plan.openingDistances().get(newSurface));
    }

    private static GlassSurfaceTransitionPropagation.Surface surface(int x, Direction face) {
        return new GlassSurfaceTransitionPropagation.Surface(new BlockPos(x, 0, 0), face);
    }
}
