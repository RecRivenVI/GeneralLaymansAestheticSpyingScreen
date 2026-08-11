package me.ichun.mods.glass.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

final class GlassPropagationRulesTest {
    @Test
    void oneFaceSpreadsOnlyAlongItsTwoTangentAxes() {
        assertEquals(
                EnumSet.of(Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST),
                GlassPropagationRules.propagationDirections(EnumSet.of(Direction.NORTH)));
        assertEquals(
                EnumSet.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST),
                GlassPropagationRules.propagationDirections(EnumSet.of(Direction.UP)));
    }

    @Test
    void twoPerpendicularFacesCanSpreadOnAllThreeAxes() {
        assertEquals(
                EnumSet.allOf(Direction.class),
                GlassPropagationRules.propagationDirections(
                        EnumSet.of(Direction.NORTH, Direction.EAST)));
    }

    @Test
    void innerAndOuterCornersTurnToOppositeSides() {
        BlockPos origin = new BlockPos(0, 64, 0);
        BlockPos current = origin.east();
        assertEquals(
                Direction.WEST,
                GlassPropagationRules.innerCornerFace(current, origin, Direction.NORTH));
        assertEquals(
                Direction.EAST,
                GlassPropagationRules.outerCornerFace(current, origin, Direction.NORTH));
    }
}
