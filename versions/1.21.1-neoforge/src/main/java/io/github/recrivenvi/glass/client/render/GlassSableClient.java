package io.github.recrivenvi.glass.client.render;

import io.github.recrivenvi.glass.model.GlassAffinePortal;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3d;

/** Optional Sable calls are reached only when its complete runtime is installed. */
public final class GlassSableClient {
    private static final boolean ENABLED = net.neoforged.fml.ModList.get().isLoaded("sable");

    private GlassSableClient() {}

    static Matrix4d pose(ClientLevel level, BlockPos pos) {
        return ENABLED ? GlassSableClientAccess.pose(level, pos) : new Matrix4d();
    }

    public static Vec3 center(ClientLevel level, BlockPos pos) {
        var point =
                pose(level, pos)
                        .transformPosition(
                                new Vector3d(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5));
        return new Vec3(point.x, point.y, point.z);
    }

    public static AABB bounds(ClientLevel level, BlockPos pos) {
        var b =
                GlassAffinePortal.bounds(
                        pose(level, pos),
                        new Vector3d(pos.getX(), pos.getY(), pos.getZ()),
                        new Vector3d(pos.getX() + 1., pos.getY() + 1., pos.getZ() + 1.));
        return new AABB(b.min().x, b.min().y, b.min().z, b.max().x, b.max().y, b.max().z);
    }

    static Vec3 wirelessVertex(
            ClientLevel level, BlockPos root, net.minecraft.core.Direction face, BlockPos node) {
        var world = center(level, node);
        var point =
                pose(level, root)
                        .invert()
                        .transformPosition(new Vector3d(world.x, world.y, world.z))
                        .sub(root.getX(), root.getY(), root.getZ());
        double x = face.getStepX() * GlassSurfaceGeometry.HALF_SIZE,
                y = face.getStepY() * GlassSurfaceGeometry.HALF_SIZE,
                z = face.getStepZ() * GlassSurfaceGeometry.HALF_SIZE;
        if (face.getStepX() > 0 && point.x > .5) x = -x;
        if (face.getStepY() > 0 && point.y > .5) y = -y;
        if (face.getStepZ() > 0 && point.z > .5) z = -z;
        return new Vec3(point.x + x, point.y + y, point.z + z);
    }

    static Matrix4f surface(Matrix4f base, ClientLevel level, BlockPos pos) {
        return new Matrix4f(
                new Matrix4d(base)
                        .mul(pose(level, pos))
                        .translate(pos.getX(), pos.getY(), pos.getZ()));
    }

    static void receive(ClientLevel level, net.minecraft.nbt.CompoundTag header) {
        if (ENABLED) GlassSableClientAccess.receive(level, header);
    }

    static void close(ClientLevel level) {
        if (ENABLED) GlassSableClientAccess.close(level);
    }

    static void tick(ClientLevel level) {
        if (ENABLED) GlassSableClientAccess.tick(level);
    }
}
