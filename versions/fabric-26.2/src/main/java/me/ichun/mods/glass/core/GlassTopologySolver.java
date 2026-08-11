package me.ichun.mods.glass.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** 以投影仪正面为源，在所有真实共边的外露 GLASS 面之间求最短传播距离。 */
public final class GlassTopologySolver {
    private GlassTopologySolver() {}

    public static List<Surface> solve(
            BlockPos projector,
            Direction initialFace,
            int maximumDistance,
            Predicate<BlockPos> isSurface,
            BiPredicate<BlockPos, Direction> isFaceExposed) {
        if (maximumDistance < 0 || !isSurface.test(projector)) {
            return List.of();
        }
        Map<Key, Integer> distances = new HashMap<>();
        ArrayDeque<Key> queue = new ArrayDeque<>();
        Key origin = new Key(projector.immutable(), initialFace);
        distances.put(origin, 0);
        queue.add(origin);

        while (!queue.isEmpty()) {
            Key current = queue.removeFirst();
            int distance = distances.get(current);
            if (distance >= maximumDistance) {
                continue;
            }
            for (Direction travel : Direction.values()) {
                if (travel.getAxis() == current.face().getAxis()) {
                    continue;
                }

                // 同一方块的垂直外露面：绕过凸角。
                if (isFaceExposed.test(current.pos(), travel)) {
                    add(new Key(current.pos(), travel), distance + 1, distances, queue);
                }

                BlockPos neighbour = current.pos().relative(travel);
                if (isSurface.test(neighbour)) {
                    if (isFaceExposed.test(neighbour, current.face())) {
                        // 相邻方块的同向外露面：沿平面越过共边。
                        add(
                                new Key(neighbour.immutable(), current.face()),
                                distance + 1,
                                distances,
                                queue);
                    }
                }

                // 对角方块的垂直外露面：绕过凹角；无需再判断是哪一种转角结构。
                BlockPos diagonal = neighbour.relative(current.face());
                Direction diagonalFace = travel.getOpposite();
                if (isSurface.test(diagonal) && isFaceExposed.test(diagonal, diagonalFace)) {
                    add(
                            new Key(diagonal.immutable(), diagonalFace),
                            distance + 1,
                            distances,
                            queue);
                }
            }
        }

        ArrayList<Surface> result = new ArrayList<>(distances.size());
        distances.forEach(
                (key, distance) -> {
                    // Projector 正面即使被遮挡也作为虚拟种子参与遍历，但内部面本身不进入渲染结果。
                    if (isFaceExposed.test(key.pos(), key.face())) {
                        result.add(new Surface(key.pos(), key.face(), distance));
                    }
                });
        result.sort(
                Comparator.comparingInt(Surface::distance)
                        .thenComparingLong(surface -> surface.pos().asLong())
                        .thenComparingInt(surface -> surface.face().get3DDataValue()));
        return List.copyOf(result);
    }

    private static void add(
            Key key, int distance, Map<Key, Integer> distances, ArrayDeque<Key> queue) {
        Integer previous = distances.putIfAbsent(key, distance);
        if (previous == null) {
            queue.addLast(key);
        }
    }

    private record Key(BlockPos pos, Direction face) {}

    public record Surface(BlockPos pos, Direction face, int distance) {}
}
