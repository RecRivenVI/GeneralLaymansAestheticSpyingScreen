package io.github.recrivenvi.glass.content.block;

import io.github.recrivenvi.glass.network.GlassNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class GlassProjectorBlock extends GlassStateBlock {
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction>
            FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public GlassProjectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                defaultBlockState()
                        .setValue(FACING, net.minecraft.core.Direction.NORTH)
                        .setValue(POWERED, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (!level.isClientSide()) {
            boolean powered = level.hasNeighborSignal(pos);
            if (state.getValue(POWERED) != powered) {
                level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, POWERED);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            GlassNetworking.openScreen(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
