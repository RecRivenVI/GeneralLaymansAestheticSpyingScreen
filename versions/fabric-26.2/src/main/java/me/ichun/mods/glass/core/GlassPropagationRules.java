package me.ichun.mods.glass.core;

import java.util.Collection;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** 原版 GLASS 的传播几何规则。投影沿每个激活面的两个切向轴传播， 并在玻璃表面的内外角处增加新的激活面。 */
public final class GlassPropagationRules {
    private GlassPropagationRules() {}

    public static EnumSet<Direction> propagationDirections(Collection<Direction> activeFaces) {
        EnumSet<Direction> directions = EnumSet.noneOf(Direction.class);
        for (Direction activeFace : activeFaces) {
            for (Direction direction : Direction.values()) {
                if (direction.getAxis() != activeFace.getAxis()) {
                    directions.add(direction);
                }
            }
        }
        return directions;
    }

    public static Direction innerCornerFace(
            BlockPos current, BlockPos origin, Direction activeFace) {
        BlockPos adjacentFace = current.relative(activeFace);
        BlockPos originFace = origin.relative(activeFace);
        return directionBetween(adjacentFace, originFace);
    }

    public static Direction outerCornerFace(
            BlockPos current, BlockPos origin, Direction activeFace) {
        BlockPos adjacentBack = current.relative(activeFace.getOpposite());
        BlockPos originBack = origin.relative(activeFace.getOpposite());
        return directionBetween(originBack, adjacentBack);
    }

    private static Direction directionBetween(BlockPos from, BlockPos to) {
        return Direction.getNearest(
                to.getX() - from.getX(),
                to.getY() - from.getY(),
                to.getZ() - from.getZ(),
                Direction.NORTH);
    }
}
