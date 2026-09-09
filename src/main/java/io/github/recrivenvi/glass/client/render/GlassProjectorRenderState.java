package io.github.recrivenvi.glass.client.render;

import java.util.EnumSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** 由服务端世界状态生成的纯客户端渲染数据，不属于方块实体生命周期。 */
public final class GlassProjectorRenderState {
    public BlockPos pos = BlockPos.ZERO;
    public ResourceKey<Level> dimension = Level.OVERWORLD;
    public GlobalPos network = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
    public Direction facing = Direction.NORTH;
    public boolean terminal;
    public boolean projector;
    public boolean active;
    public EnumSet<Direction> activeFaces = EnumSet.noneOf(Direction.class);
    public Map<Direction, Integer> propagationDistances = Map.of();
    public String channel = "";
    public java.util.List<BlockPos> wirelessPositions = java.util.List.of();
    public float animationTicks;
}
