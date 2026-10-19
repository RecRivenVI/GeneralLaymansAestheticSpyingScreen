package io.github.recrivenvi.glass.content.block;

import com.mojang.serialization.MapCodec;
import io.github.recrivenvi.glass.network.GlassNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class GlassWirelessExtensionBlock extends GlassStateBlock {
    public static final MapCodec<GlassWirelessExtensionBlock> CODEC =
            simpleCodec(GlassWirelessExtensionBlock::new);
    private static final VoxelShape CORE = Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0);

    public GlassWirelessExtensionBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : GlassAttachmentSupport.PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<GlassWirelessExtensionBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (var entry : GlassAttachmentSupport.PROPERTY_BY_DIRECTION.entrySet()) {
            if (state.getValue(entry.getValue())) {
                shape = Shapes.or(shape, GlassAttachmentSupport.shape(entry.getKey()));
            }
        }
        return shape;
    }

    static VoxelShape shape() {
        return CORE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return GlassAttachmentSupport.withSupports(
                defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            net.minecraft.core.Direction directionToNeighbour,
            BlockState neighbourState,
            net.minecraft.world.level.LevelAccessor level,
            BlockPos pos,
            BlockPos neighbourPos) {
        return state.setValue(
                GlassAttachmentSupport.PROPERTY_BY_DIRECTION.get(directionToNeighbour),
                GlassAttachmentSupport.needsSupport(
                        level, neighbourPos, directionToNeighbour.getOpposite()));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(
                GlassAttachmentSupport.PROPERTY_BY_DIRECTION
                        .values()
                        .toArray(BooleanProperty[]::new));
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
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
