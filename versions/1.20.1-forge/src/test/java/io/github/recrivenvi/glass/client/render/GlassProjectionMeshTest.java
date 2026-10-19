package io.github.recrivenvi.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class GlassProjectionMeshTest {
    static {
        io.github.recrivenvi.glass.ForgeTestBootstrap.initialize();
    }

    private static final double EPSILON = 1.0E-12;

    @Test
    void wirelessVertexUsesTheRequestedFaceBoundaryForEveryDirection() {
        BlockPos projector = BlockPos.ZERO;
        BlockPos node = new BlockPos(-3, 4, -5);
        for (Direction face : Direction.values()) {
            Vec3 vertex = GlassProjectionMesh.wirelessVertex(projector, face, node);
            Vec3 center = Vec3.atCenterOf(node);
            assertEquals(Math.abs(face.getStepX()) * 0.5, Math.abs(vertex.x - center.x), EPSILON);
            assertEquals(Math.abs(face.getStepY()) * 0.5, Math.abs(vertex.y - center.y), EPSILON);
            assertEquals(Math.abs(face.getStepZ()) * 0.5, Math.abs(vertex.z - center.z), EPSILON);
        }
    }

    @Test
    void positiveFaceFlipsAtNodesBeyondTheProjector() {
        BlockPos projector = BlockPos.ZERO;
        assertEquals(
                2.0,
                GlassProjectionMesh.wirelessVertex(projector, Direction.EAST, new BlockPos(2, 0, 0))
                        .x,
                EPSILON);
        assertEquals(
                -1.0,
                GlassProjectionMesh.wirelessVertex(
                                projector, Direction.EAST, new BlockPos(-2, 0, 0))
                        .x,
                EPSILON);
    }
}
