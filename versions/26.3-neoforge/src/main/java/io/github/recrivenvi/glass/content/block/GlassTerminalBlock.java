package io.github.recrivenvi.glass.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GlassTerminalBlock extends GlassChannelBlock {

    public GlassTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.or(CORE, frontMarker(state.getValue(FACING)));
    }

    static VoxelShape coreShape() {
        return CORE;
    }

    static VoxelShape frontMarker(Direction facing) {
        return switch (facing) {
            case NORTH -> Block.box(5.0, 5.0, 3.0, 11.0, 11.0, 4.0);
            case SOUTH -> Block.box(5.0, 5.0, 12.0, 11.0, 11.0, 13.0);
            case WEST -> Block.box(3.0, 5.0, 5.0, 4.0, 11.0, 11.0);
            case EAST -> Block.box(12.0, 5.0, 5.0, 13.0, 11.0, 11.0);
            case DOWN -> Block.box(5.0, 3.0, 5.0, 11.0, 4.0, 11.0);
            case UP -> Block.box(5.0, 12.0, 5.0, 11.0, 13.0, 11.0);
        };
    }

    private static final VoxelShape CORE = Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0);
}
