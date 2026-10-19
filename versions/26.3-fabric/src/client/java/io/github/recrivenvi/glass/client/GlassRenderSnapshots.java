package io.github.recrivenvi.glass.client;

import io.github.recrivenvi.glass.client.render.GlassProjectorRenderState;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** 把全局同步状态转换成渲染器的逐网络视图，不依赖方块实体观察回调。 */
public final class GlassRenderSnapshots {
    private GlassRenderSnapshots() {}

    public static List<GlassProjectorRenderState> current(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null) return List.of();
        GlassStateSnapshot snapshot = GlassClientState.snapshot();
        double now =
                level.getGameTime() + client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        return states(level.dimension(), snapshot.networks(), now);
    }

    static List<GlassProjectorRenderState> states(
            ResourceKey<Level> dimension, List<GlassStateSnapshot.Network> networks, double now) {
        ArrayList<GlassProjectorRenderState> result = new ArrayList<>();
        for (GlassStateSnapshot.Network network : networks) {
            if (!network.projector().dimension().equals(dimension)) continue;
            GlassProjectorRenderState terminalState = base(network.terminal(), now);
            terminalState.network = network.projector();
            terminalState.terminal = true;
            terminalState.channel = network.channelId().toString();
            terminalState.facing = network.terminalFacing();
            result.add(terminalState);

            Map<BlockPos, Map<Direction, Integer>> surfaces =
                    surfaceDistances(dimension, network.surfaces());
            boolean hasProjectorSurface = false;
            for (Map.Entry<BlockPos, Map<Direction, Integer>> entry : surfaces.entrySet()) {
                GlassProjectorRenderState state =
                        base(GlobalPos.of(dimension, entry.getKey()), now);
                state.network = network.projector();
                state.projector = entry.getKey().equals(network.projector().pos());
                hasProjectorSurface |= state.projector;
                state.active = network.active();
                state.channel = network.channelId().toString();
                state.facing = network.facing();
                state.activeFaces = EnumSet.copyOf(entry.getValue().keySet());
                state.propagationDistances = Map.copyOf(entry.getValue());
                if (state.projector) {
                    state.wirelessPositions =
                            network.wirelessPositions().stream().map(GlobalPos::pos).toList();
                }
                result.add(state);
            }

            // 投影源是捕获配对的身份根，不等同于一个必须可见的投影表面。正面被实体方块
            // 挡住时，拓扑求解器会正确地省略该表面，但捕获仍须从这个根读取频道与相机。
            if (!hasProjectorSurface) {
                GlassProjectorRenderState state = base(network.projector(), now);
                state.network = network.projector();
                state.projector = true;
                state.active = network.active();
                state.channel = network.channelId().toString();
                state.facing = network.facing();
                state.activeFaces =
                        network.wirelessPositions().isEmpty()
                                ? EnumSet.noneOf(Direction.class)
                                : EnumSet.of(network.facing());
                state.wirelessPositions =
                        network.wirelessPositions().stream().map(GlobalPos::pos).toList();
                result.add(state);
            }
        }
        return List.copyOf(result);
    }

    static Map<BlockPos, Map<Direction, Integer>> surfaceDistances(
            ResourceKey<Level> dimension, List<GlassStateSnapshot.Surface> values) {
        Map<BlockPos, EnumMap<Direction, Integer>> grouped = new LinkedHashMap<>();
        for (GlassStateSnapshot.Surface surface : values) {
            if (!surface.pos().dimension().equals(dimension)) continue;
            grouped.computeIfAbsent(surface.pos().pos(), ignored -> new EnumMap<>(Direction.class))
                    .merge(surface.face(), surface.distance(), Math::min);
        }
        Map<BlockPos, Map<Direction, Integer>> result = new LinkedHashMap<>();
        grouped.forEach((pos, distances) -> result.put(pos, Map.copyOf(distances)));
        return Map.copyOf(result);
    }

    private static GlassProjectorRenderState base(GlobalPos pos, double now) {
        GlassProjectorRenderState state = new GlassProjectorRenderState();
        state.pos = pos.pos().immutable();
        state.dimension = pos.dimension();
        state.facing = Direction.NORTH;
        state.animationTicks = (float) now;
        return state;
    }
}
