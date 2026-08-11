package me.ichun.mods.glass.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.ichun.mods.glass.content.GlassContent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Converts stable GLASS surface and wireless geometry into composite vertices. */
final class GlassProjectionMesh {
    private GlassProjectionMesh() {}

    static Vec3 wirelessVertex(BlockPos projectorPos, Direction face, BlockPos pos) {
        double x = face.getStepX() * GlassSurfaceGeometry.HALF_SIZE;
        double y = face.getStepY() * GlassSurfaceGeometry.HALF_SIZE;
        double z = face.getStepZ() * GlassSurfaceGeometry.HALF_SIZE;
        if (face.getStepX() > 0 && pos.getX() > projectorPos.getX()) x = -x;
        if (face.getStepY() > 0 && pos.getY() > projectorPos.getY()) y = -y;
        if (face.getStepZ() > 0 && pos.getZ() > projectorPos.getZ()) z = -z;
        return Vec3.atCenterOf(pos).add(x, y, z);
    }

    static void emitGrowingTriangle(
            Matrix4f matrix,
            VertexConsumer vertices,
            Vec3 pivot,
            Vec3 first,
            Vec3 second,
            Vec3 third,
            GlassProjectionAnimation.Frame frame) {
        addVertex(
                matrix,
                vertices,
                pivot.add(first.subtract(pivot).scale(frame.geometryScale())),
                frame);
        addVertex(
                matrix,
                vertices,
                pivot.add(second.subtract(pivot).scale(frame.geometryScale())),
                frame);
        addVertex(
                matrix,
                vertices,
                pivot.add(third.subtract(pivot).scale(frame.geometryScale())),
                frame);
    }

    static boolean shouldRenderFace(ClientLevel level, BlockPos pos, Direction face) {
        BlockState neighborState = level.getBlockState(pos.relative(face));
        if (GlassContent.isSurface(neighborState)) return false;
        BlockState state = level.getBlockState(pos);
        return Block.shouldRenderFace(state, neighborState, face);
    }

    static void emitSurfaceQuad(
            Matrix4f matrix,
            VertexConsumer vertices,
            BlockPos pos,
            Direction face,
            GlassProjectionAnimation.Frame frame) {
        GlassSurfaceGeometry.Basis basis = GlassSurfaceGeometry.at(pos, face);
        double halfSize = GlassSurfaceGeometry.HALF_SIZE * frame.geometryScale();
        addVertex(matrix, vertices, basis.point(-halfSize, -halfSize), frame);
        addVertex(matrix, vertices, basis.point(halfSize, -halfSize), frame);
        addVertex(matrix, vertices, basis.point(halfSize, halfSize), frame);
        addVertex(matrix, vertices, basis.point(-halfSize, halfSize), frame);
    }

    private static void addVertex(
            Matrix4f matrix,
            VertexConsumer vertices,
            Vec3 pos,
            GlassProjectionAnimation.Frame frame) {
        vertices.addVertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(frame.whiteMix(), frame.whiteMix(), frame.whiteMix(), frame.alpha());
    }
}
