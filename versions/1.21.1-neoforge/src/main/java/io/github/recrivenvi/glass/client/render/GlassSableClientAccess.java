package io.github.recrivenvi.glass.client.render;

import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import org.joml.Matrix4d;

/** Loaded only after the optional Sable runtime has been detected. */
final class GlassSableClientAccess {
    private GlassSableClientAccess() {}

    static Matrix4d pose(ClientLevel level, BlockPos pos) {
        var sub = SableCompanion.INSTANCE.getContaining(level, pos);
        return sub instanceof ClientSubLevelAccess client
                ? client.renderPose(
                                Minecraft.getInstance()
                                        .getTimer()
                                        .getGameTimeDeltaPartialTick(false))
                        .bakeIntoMatrix(new Matrix4d())
                : new Matrix4d();
    }

    static void receive(ClientLevel level, net.minecraft.nbt.CompoundTag header) {
        if (!header.contains("sableShips")) return;
        var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);
        int tick = header.getInt("serverTick");
        container.getInterpolation().receiveInfo(100, tick, false);
        java.util.Set<java.util.UUID> present = new java.util.HashSet<>();
        for (var value : header.getList("sableShips", 10)) {
            var tag = (net.minecraft.nbt.CompoundTag) value;
            var id = tag.getUUID("id");
            present.add(id);
            var pose =
                    dev.ryanhcode.sable.companion.math.Pose3d.CODEC
                            .parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("pose"))
                            .getOrThrow();
            var ship = (dev.ryanhcode.sable.sublevel.ClientSubLevel) container.getSubLevel(id);
            if (ship == null)
                ship =
                        (dev.ryanhcode.sable.sublevel.ClientSubLevel)
                                container.allocateSubLevel(
                                        id, tag.getInt("plotX"), tag.getInt("plotZ"), pose);
            int[] b = tag.getIntArray("bounds");
            var bounds =
                    new dev.ryanhcode.sable.companion.math.BoundingBox3i(
                            b[0], b[1], b[2], b[3], b[4], b[5]);
            if (!bounds.equals(ship.getPlot().getBoundingBox()) || !ship.isFinalized()) {
                ship.getPlot().setBoundingBox(bounds);
                if (!ship.isFinalized()) ship.updateRenderData();
                ship.setFinalized();
                ship.onPlotBoundsChanged();
            }
            container
                    .getInterpolation()
                    .receiveSnapshot(
                            ship,
                            tick,
                            pose,
                            dev.ryanhcode.sable.network.packets.PacketReceiveMode.TCP);
        }
        for (var ship : java.util.List.copyOf(container.getAllSubLevels()))
            if (!present.contains(ship.getUniqueId()))
                container.removeSubLevel(
                        ship, dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.UNLOADED);
        container.processSubLevelRemovals();
    }

    static void close(ClientLevel level) {
        var container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);
        for (var ship : java.util.List.copyOf(container.getAllSubLevels()))
            container.removeSubLevel(
                    ship, dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.UNLOADED);
        container.processSubLevelRemovals();
    }

    static void tick(ClientLevel level) {
        dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level).tick();
    }
}
