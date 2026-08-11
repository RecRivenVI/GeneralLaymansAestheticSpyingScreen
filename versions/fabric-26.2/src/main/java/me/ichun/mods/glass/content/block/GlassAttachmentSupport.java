package me.ichun.mods.glass.content.block;

import java.util.EnumMap;
import java.util.Map;
import me.ichun.mods.glass.content.GlassContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 把依赖承载面的相邻方块连接到小型 GLASS 核心，不把普通独立方块误认成节点。 */
final class GlassAttachmentSupport {
    static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = properties();
    private static final Map<Direction, VoxelShape> SHAPES =
            Map.of(
                    Direction.NORTH, Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 4.0),
                    Direction.SOUTH, Block.box(5.0, 5.0, 12.0, 11.0, 11.0, 16.0),
                    Direction.WEST, Block.box(0.0, 5.0, 5.0, 4.0, 11.0, 11.0),
                    Direction.EAST, Block.box(12.0, 5.0, 5.0, 16.0, 11.0, 11.0),
                    Direction.DOWN, Block.box(5.0, 0.0, 5.0, 11.0, 4.0, 11.0),
                    Direction.UP, Block.box(5.0, 12.0, 5.0, 11.0, 16.0, 11.0));

    private GlassAttachmentSupport() {}

    static BlockState withSupports(BlockState state, LevelReader level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            state =
                    state.setValue(
                            PROPERTY_BY_DIRECTION.get(direction),
                            needsSupport(level, pos.relative(direction), direction.getOpposite()));
        }
        return state;
    }

    static boolean needsSupport(LevelReader level, BlockPos neighbourPos, Direction attachedFace) {
        BlockState neighbour = level.getBlockState(neighbourPos);
        if (neighbour.isAir()
                || GlassContent.isSurface(neighbour)
                || !neighbour.getFluidState().isEmpty()
                || !neighbour.canSurvive(level, neighbourPos)
                || !neighbour.getBlockSupportShape(level, neighbourPos).isEmpty()) {
            return false;
        }
        VoxelShape shape = neighbour.getShape(level, neighbourPos);
        return !shape.isEmpty() && !shape.getFaceShape(attachedFace).isEmpty();
    }

    static VoxelShape shape(Direction direction) {
        return SHAPES.get(direction);
    }

    private static Map<Direction, BooleanProperty> properties() {
        EnumMap<Direction, BooleanProperty> result = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            result.put(
                    direction, BooleanProperty.create("support_" + direction.getSerializedName()));
        }
        return Map.copyOf(result);
    }
}
