package me.ichun.mods.glass.content.block;

import me.ichun.mods.glass.state.GlassServerState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

/** 普通方块物理行为之上，仅附加世界级 GLASS 状态维护。 */
abstract class GlassStateBlock extends Block {
    protected GlassStateBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel) {
            if (!oldState.is(state.getBlock())) {
                GlassServerState.placed(serverLevel, pos, state);
            } else if (oldState != state) {
                GlassServerState.topologyChanged(serverLevel);
            }
        }
    }

    /** 原版由每个 GLASS 方块的 neighborChanged 驱动传播修正。现代实现集中求解拓扑， 但仍必须保留相同的更新入口，才能在相邻遮挡或连接状态变化后丢弃旧快照。 */
    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        if (level instanceof ServerLevel serverLevel) {
            GlassServerState.topologyChanged(serverLevel);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        GlassServerState.removed(level, pos, state);
    }
}
