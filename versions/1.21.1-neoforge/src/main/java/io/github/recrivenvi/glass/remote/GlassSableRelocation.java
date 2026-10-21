package io.github.recrivenvi.glass.remote;

import io.github.recrivenvi.glass.state.GlassServerState;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;

/** Preserves world-level GLASS channel data across Sable's native block relocation callbacks. */
public final class GlassSableRelocation {
    private static final ThreadLocal<Batch> BATCH = ThreadLocal.withInitial(Batch::new);

    private GlassSableRelocation() {}

    public static void before(ServerLevel from, ServerLevel to, BlockPos oldPos, BlockPos newPos) {
        var source = GlobalPos.of(from.dimension(), oldPos.immutable());
        var target = GlobalPos.of(to.dimension(), newPos.immutable());
        var saved = GlassServerState.saved(from.getServer());
        var batch = BATCH.get();
        batch.destinations.put(source, target);
        batch.pending.put(
                source,
                new Move(
                        saved.terminals().get(source),
                        saved.projectors().get(source),
                        saved.projectors().values().stream()
                                .filter(p -> p.wirelessPositions().contains(source))
                                .toList()));
    }

    public static void after(ServerLevel from, ServerLevel to, BlockPos oldPos, BlockPos newPos) {
        var source = GlobalPos.of(from.dimension(), oldPos.immutable());
        var target = GlobalPos.of(to.dimension(), newPos.immutable());
        var saved = GlassServerState.saved(from.getServer());
        var batch = BATCH.get();
        var move = batch.pending.remove(source);
        saved.remove(source);
        if (move.terminal() != null)
            saved.putTerminal(new GlassStateSnapshot.Terminal(target, move.terminal().channel()));
        if (move.projector() != null)
            saved.putProjector(
                    new GlassStateSnapshot.Projector(
                            target,
                            move.projector().channelId(),
                            move.projector().wirelessPositions().stream()
                                    .map(p -> batch.destinations.getOrDefault(p, p))
                                    .toList()));
        for (var previous : move.wirelessOwners()) {
            var root = batch.destinations.getOrDefault(previous.pos(), previous.pos());
            var current = saved.projectors().get(root);
            var value = current == null ? previous : current;
            var nodes = new java.util.ArrayList<>(value.wirelessPositions());
            int index = previous.wirelessPositions().indexOf(source);
            nodes.remove(source);
            nodes.remove(target);
            nodes.add(Math.min(index, nodes.size()), target);
            saved.putProjector(
                    new GlassStateSnapshot.Projector(root, value.channelId(), List.copyOf(nodes)));
        }
        if (move.terminal() != null || move.projector() != null)
            GlassSablePersistence.remember(to, target);
        GlassServerState.topologyChanged(to);
        if (batch.pending.isEmpty()) BATCH.remove();
    }

    private static final class Batch {
        final Map<GlobalPos, GlobalPos> destinations = new LinkedHashMap<>();
        final Map<GlobalPos, Move> pending = new LinkedHashMap<>();
    }

    private record Move(
            GlassStateSnapshot.Terminal terminal,
            GlassStateSnapshot.Projector projector,
            List<GlassStateSnapshot.Projector> wirelessOwners) {}
}
