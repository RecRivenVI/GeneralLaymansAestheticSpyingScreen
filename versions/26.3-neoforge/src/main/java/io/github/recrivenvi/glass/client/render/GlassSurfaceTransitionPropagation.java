package io.github.recrivenvi.glass.client.render;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** 根据新旧外露面之差，让拓扑热更新从变化边界局部传播。 */
final class GlassSurfaceTransitionPropagation {
    private GlassSurfaceTransitionPropagation() {}

    static Plan between(Map<Surface, Integer> previous, Map<Surface, Integer> current) {
        Set<Surface> retained = new HashSet<>(previous.keySet());
        retained.retainAll(current.keySet());

        Map<Surface, Integer> added = difference(current, previous.keySet());
        Map<Surface, Integer> removed = difference(previous, current.keySet());
        return new Plan(localDistances(added, retained), localDistances(removed, retained));
    }

    private static Map<Surface, Integer> difference(
            Map<Surface, Integer> source, Set<Surface> excluded) {
        Map<Surface, Integer> result = new HashMap<>();
        source.forEach(
                (surface, distance) -> {
                    if (!excluded.contains(surface)) result.put(surface, distance);
                });
        return result;
    }

    private static Map<Surface, Integer> localDistances(
            Map<Surface, Integer> changedSourceDistances, Set<Surface> retained) {
        if (changedSourceDistances.isEmpty()) return Map.of();

        Set<Surface> changed = changedSourceDistances.keySet();
        Map<Surface, Integer> result = new HashMap<>();
        ArrayDeque<Surface> queue = new ArrayDeque<>();
        for (Surface surface : changed) {
            if (neighbours(surface).stream().anyMatch(retained::contains)) {
                result.put(surface, 0);
                queue.addLast(surface);
            }
        }

        while (!queue.isEmpty()) {
            Surface current = queue.removeFirst();
            int distance = result.get(current);
            for (Surface neighbour : neighbours(current)) {
                if (changed.contains(neighbour)
                        && result.putIfAbsent(neighbour, distance + 1) == null) {
                    queue.addLast(neighbour);
                }
            }
        }

        // 完全替换的网络或无法与保留面相接的分量没有局部边界，只能沿用投影源距离。
        changedSourceDistances.forEach(result::putIfAbsent);
        return Map.copyOf(result);
    }

    private static List<Surface> neighbours(Surface surface) {
        ArrayList<Surface> result = new ArrayList<>(12);
        for (Direction travel : Direction.values()) {
            if (travel.getAxis() == surface.face().getAxis()) continue;
            result.add(new Surface(surface.pos(), travel));
            result.add(new Surface(surface.pos().relative(travel), surface.face()));
            result.add(
                    new Surface(
                            surface.pos().relative(travel).relative(surface.face()),
                            travel.getOpposite()));
        }
        return result;
    }

    record Surface(BlockPos pos, Direction face) {
        Surface {
            pos = pos.immutable();
        }
    }

    record Plan(Map<Surface, Integer> openingDistances, Map<Surface, Integer> closingDistances) {
        Plan {
            openingDistances = Map.copyOf(openingDistances);
            closingDistances = Map.copyOf(closingDistances);
        }

        static Plan empty() {
            return new Plan(Map.of(), Map.of());
        }

        int distance(Surface surface, boolean opening, int fallback) {
            return (opening ? openingDistances : closingDistances).getOrDefault(surface, fallback);
        }

        boolean hasClosingSurfaces() {
            return !closingDistances.isEmpty();
        }
    }
}
