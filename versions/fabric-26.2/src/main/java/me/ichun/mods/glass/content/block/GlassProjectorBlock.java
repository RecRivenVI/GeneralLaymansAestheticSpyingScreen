package me.ichun.mods.glass.content.block;

import com.mojang.serialization.MapCodec;
import me.ichun.mods.glass.network.GlassNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class GlassProjectorBlock extends GlassConnectedBlock {
    public static final MapCodec<GlassProjectorBlock> CODEC = simpleCodec(GlassProjectorBlock::new);
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

    @Override
    protected MapCodec<GlassProjectorBlock> codec() {
        return CODEC;
    }

    @Nullable @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withConnectionsAndSupports(
                defaultBlockState()
                        .setValue(FACING, context.getNearestLookingDirection().getOpposite())
                        .setValue(
                                POWERED,
                                context.getLevel().hasNeighborSignal(context.getClickedPos())),
                context.getLevel(),
                context.getClickedPos());
    }

    @Override
    protected boolean connectsTo(BlockState state, Direction direction, BlockState neighbourState) {
        return allowsWiredConnection(
                state.getValue(FACING),
                direction,
                super.connectsTo(state, direction, neighbourState));
    }

    static boolean allowsWiredConnection(
            Direction facing, Direction direction, boolean connectedNeighbour) {
        return direction != facing && connectedNeighbour;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.or(
                super.getShape(state, level, pos, context), frontMarker(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.or(
                super.getCollisionShape(state, level, pos, context),
                frontMarker(state.getValue(FACING)));
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
