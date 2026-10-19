package io.github.recrivenvi.glass.content.block;

import io.github.recrivenvi.glass.state.GlassServerState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

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
            BlockPos neighbourPos,
            boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, neighbourPos, movedByPiston);
        if (level instanceof ServerLevel serverLevel) {
            GlassServerState.topologyChanged(serverLevel);
        }
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel)
            GlassServerState.removed(serverLevel, pos, state);
    }
}
