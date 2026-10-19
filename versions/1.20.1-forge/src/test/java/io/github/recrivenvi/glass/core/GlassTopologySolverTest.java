package io.github.recrivenvi.glass.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

final class GlassTopologySolverTest {
    static {
        io.github.recrivenvi.glass.ForgeTestBootstrap.initialize();
    }

    @Test
    void closedRoomReachesAllSixInteriorFacesWithoutWaveConflicts() {
        Set<BlockPos> shell = new HashSet<>();
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 5; z++) {
                    if (x == 0 || x == 4 || y == 0 || y == 4 || z == 0 || z == 4) {
                        shell.add(new BlockPos(x, y, z));
                    }
                }
            }
        }

        var surfaces =
                GlassTopologySolver.solve(
                        new BlockPos(0, 2, 2),
                        Direction.EAST,
                        64,
                        shell::contains,
                        exposedAgainst(shell));
        EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
        surfaces.forEach(surface -> faces.add(surface.face()));

        assertEquals(EnumSet.allOf(Direction.class), faces);
        assertEquals(54, surfaces.size(), "5x5x5 封闭房间只能激活 6 个 3x3 内表面");
        assertTrue(surfaces.stream().allMatch(surface -> surface.distance() <= 64));
        assertTrue(
                surfaces.stream()
                        .allMatch(
                                surface -> {
                                    BlockPos air = surface.pos().relative(surface.face());
                                    return air.getX() >= 1
                                            && air.getX() <= 3
                                            && air.getY() >= 1
                                            && air.getY() <= 3
                                            && air.getZ() >= 1
                                            && air.getZ() <= 3;
                                }),
                "每个活动面都必须朝向封闭房间内部，不能泄露到外侧");
        for (int coordinate = 1; coordinate < 4; coordinate++) {
            for (int other = 1; other < 4; other++) {
                int along = coordinate;
                int across = other;
                assertTrue(
                        surfaces.stream()
                                .anyMatch(
                                        value ->
                                                value.pos().equals(new BlockPos(0, along, across))
                                                        && value.face() == Direction.EAST));
                assertTrue(
                        surfaces.stream()
                                .anyMatch(
                                        value ->
                                                value.pos().equals(new BlockPos(4, along, across))
                                                        && value.face() == Direction.WEST));
                assertTrue(
                        surfaces.stream()
                                .anyMatch(
                                        value ->
                                                value.pos().equals(new BlockPos(along, 0, across))
                                                        && value.face() == Direction.UP));
                assertTrue(
                        surfaces.stream()
                                .anyMatch(
                                        value ->
                                                value.pos().equals(new BlockPos(along, 4, across))
                                                        && value.face() == Direction.DOWN));
                assertTrue(
                        surfaces.stream()
                                .anyMatch(
                                        value ->
                                                value.pos().equals(new BlockPos(along, across, 0))
                                                        && value.face() == Direction.SOUTH));
                assertTrue(
                        surfaces.stream()
                                .anyMatch(
                                        value ->
                                                value.pos().equals(new BlockPos(along, across, 4))
                                                        && value.face() == Direction.NORTH));
            }
        }
        assertTrue(
                surfaces.stream()
                        .anyMatch(
                                surface ->
                                        surface.pos().equals(new BlockPos(0, 2, 2))
                                                && surface.face() == Direction.EAST
                                                && surface.distance() == 0),
                "投影仪自身正面必须是传播源面");
    }

    @Test
    void configuredDistanceIsAnActualGraphLimit() {
        Set<BlockPos> line =
                Set.of(
                        new BlockPos(0, 0, 0),
                        new BlockPos(1, 0, 0),
                        new BlockPos(2, 0, 0),
                        new BlockPos(3, 0, 0));
        var surfaces =
                GlassTopologySolver.solve(
                        BlockPos.ZERO, Direction.UP, 2, line::contains, exposedAgainst(line));
        assertTrue(
                surfaces.stream()
                        .noneMatch(surface -> surface.pos().equals(new BlockPos(3, 0, 0))));
    }

    @Test
    void projectorSeedsOnlyItsFrontFaceAndBroadcastsAlongThatFacesTangents() {
        BlockPos projector = BlockPos.ZERO;
        Set<BlockPos> tangentSurface =
                Set.of(
                        projector,
                        projector.relative(Direction.UP),
                        projector.relative(Direction.NORTH));

        var surfaces =
                GlassTopologySolver.solve(
                        projector,
                        Direction.EAST,
                        64,
                        tangentSurface::contains,
                        exposedAgainst(tangentSurface));

        assertTrue(
                surfaces.stream()
                        .anyMatch(
                                value ->
                                        value.pos().equals(projector)
                                                && value.face() == Direction.EAST
                                                && value.distance() == 0));
        assertTrue(
                surfaces.stream()
                        .anyMatch(
                                value ->
                                        value.pos().equals(projector.relative(Direction.UP))
                                                && value.face() == Direction.EAST));
        assertTrue(
                surfaces.stream()
                        .anyMatch(
                                value ->
                                        value.pos().equals(projector.relative(Direction.NORTH))
                                                && value.face() == Direction.EAST));

        Set<BlockPos> normalSurface = Set.of(projector, projector.relative(Direction.EAST));
        var normalSurfaces =
                GlassTopologySolver.solve(
                        projector,
                        Direction.EAST,
                        64,
                        normalSurface::contains,
                        exposedAgainst(normalSurface));
        assertFalse(
                normalSurfaces.stream()
                        .anyMatch(
                                value ->
                                        value.pos().equals(projector)
                                                && value.face() == Direction.EAST),
                "被挡住的初始面不能进入渲染结果");
        assertTrue(
                normalSurfaces.stream()
                        .anyMatch(
                                value ->
                                        value.pos().equals(projector)
                                                && value.face() == Direction.UP
                                                && value.distance() == 1),
                "被挡住的初始面仍须作为虚拟种子向相邻外露面传播");
        assertTrue(
                normalSurfaces.stream()
                        .anyMatch(
                                value ->
                                        value.pos().equals(projector.relative(Direction.EAST))
                                                && value.face() == Direction.UP
                                                && value.distance() == 2));
    }

    @Test
    void ordinaryBlockInFrontDoesNotSuppressTheWholeNetwork() {
        BlockPos projector = BlockPos.ZERO;
        Set<BlockPos> surfaces = Set.of(projector);
        Set<BlockPos> occluders = Set.of(projector, projector.relative(Direction.NORTH));

        var result =
                GlassTopologySolver.solve(
                        projector,
                        Direction.NORTH,
                        8,
                        surfaces::contains,
                        (pos, face) -> !occluders.contains(pos.relative(face)));

        assertFalse(
                result.stream()
                        .anyMatch(
                                value ->
                                        value.pos().equals(projector)
                                                && value.face() == Direction.NORTH));
        assertEquals(
                EnumSet.of(
                        Direction.DOWN,
                        Direction.UP,
                        Direction.SOUTH,
                        Direction.WEST,
                        Direction.EAST),
                result.stream()
                        .map(GlassTopologySolver.Surface::face)
                        .collect(
                                () -> EnumSet.noneOf(Direction.class),
                                EnumSet::add,
                                EnumSet::addAll));
    }

    @Test
    void roomFloorInnerTurnDoesNotAlsoLeakToTheShellUnderside() {
        Set<BlockPos> shell = boxShell(7);
        BlockPos projector = new BlockPos(3, 1, 3);
        Set<BlockPos> surfacesAndProjector = new HashSet<>(shell);
        surfacesAndProjector.add(projector);
        Set<BlockPos> occluders = new HashSet<>(surfacesAndProjector);
        occluders.add(projector.above());
        occluders.add(projector.above(2));

        var surfaces =
                GlassTopologySolver.solve(
                        projector,
                        Direction.NORTH,
                        64,
                        surfacesAndProjector::contains,
                        (pos, face) -> !occluders.contains(pos.relative(face)));

        assertTrue(
                surfaces.stream()
                        .anyMatch(
                                value ->
                                        value.pos().equals(new BlockPos(3, 0, 2))
                                                && value.face() == Direction.UP
                                                && value.distance() == 1),
                "投影仪北面向下必须转入地板的房间内表面");
        assertFalse(
                surfaces.stream()
                        .anyMatch(
                                value ->
                                        value.pos().equals(new BlockPos(3, 0, 3))
                                                && value.face() == Direction.DOWN),
                "同一条边不能同时转到地板外侧");
        assertFalse(
                surfaces.stream()
                        .anyMatch(
                                value ->
                                        value.pos().equals(projector)
                                                && value.face() == Direction.UP),
                "普通实体方块必须阻挡投影仪侧面的外角传播");
        assertTrue(
                surfaces.stream()
                        .allMatch(
                                value ->
                                        facesRoomInterior(value, 7)
                                                || value.pos().equals(projector)),
                "封闭房间传播不得到达外壳外侧");
    }

    @Test
    void exposedSingleBlockCanWrapAroundRealOuterEdges() {
        Set<BlockPos> single = Set.of(BlockPos.ZERO);
        var surfaces =
                GlassTopologySolver.solve(
                        BlockPos.ZERO,
                        Direction.NORTH,
                        64,
                        single::contains,
                        exposedAgainst(single));

        assertEquals(
                EnumSet.allOf(Direction.class),
                surfaces.stream()
                        .map(GlassTopologySolver.Surface::face)
                        .collect(
                                () -> EnumSet.noneOf(Direction.class),
                                EnumSet::add,
                                EnumSet::addAll));
    }

    @Test
    void everyExposedFaceSharingTheSameEdgeIsReachable() {
        BlockPos diagonal = new BlockPos(1, 1, 0);
        Set<BlockPos> blocks = Set.of(BlockPos.ZERO, diagonal);

        var surfaces =
                GlassTopologySolver.solve(
                        BlockPos.ZERO, Direction.UP, 2, blocks::contains, exposedAgainst(blocks));

        assertTrue(
                surfaces.stream()
                        .anyMatch(
                                value ->
                                        value.pos().equals(BlockPos.ZERO)
                                                && value.face() == Direction.EAST
                                                && value.distance() == 1));
        assertTrue(
                surfaces.stream()
                        .anyMatch(
                                value ->
                                        value.pos().equals(diagonal)
                                                && value.face() == Direction.WEST
                                                && value.distance() == 1),
                "共用同一条边的对角外露面无需依赖特定内角结构");
    }

    @Test
    void facesBetweenTouchingGlassBlocksNeverEnterTheGraph() {
        BlockPos east = BlockPos.ZERO.east();
        Set<BlockPos> blocks = Set.of(BlockPos.ZERO, east);

        var surfaces =
                GlassTopologySolver.solve(
                        BlockPos.ZERO, Direction.UP, 8, blocks::contains, exposedAgainst(blocks));

        assertFalse(
                surfaces.stream()
                        .anyMatch(
                                value ->
                                        value.pos().equals(BlockPos.ZERO)
                                                && value.face() == Direction.EAST));
        assertFalse(
                surfaces.stream()
                        .anyMatch(
                                value ->
                                        value.pos().equals(east)
                                                && value.face() == Direction.WEST));
    }

    private static java.util.function.BiPredicate<BlockPos, Direction> exposedAgainst(
            Set<BlockPos> occluders) {
        return (pos, face) -> !occluders.contains(pos.relative(face));
    }

    private static Set<BlockPos> boxShell(int size) {
        Set<BlockPos> shell = new HashSet<>();
        int maximum = size - 1;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                for (int z = 0; z < size; z++) {
                    if (x == 0
                            || x == maximum
                            || y == 0
                            || y == maximum
                            || z == 0
                            || z == maximum) {
                        shell.add(new BlockPos(x, y, z));
                    }
                }
            }
        }
        return shell;
    }

    private static boolean facesRoomInterior(GlassTopologySolver.Surface surface, int size) {
        BlockPos air = surface.pos().relative(surface.face());
        int maximumInterior = size - 2;
        return air.getX() >= 1
                && air.getX() <= maximumInterior
                && air.getY() >= 1
                && air.getY() <= maximumInterior
                && air.getZ() >= 1
                && air.getZ() <= maximumInterior;
    }
}
