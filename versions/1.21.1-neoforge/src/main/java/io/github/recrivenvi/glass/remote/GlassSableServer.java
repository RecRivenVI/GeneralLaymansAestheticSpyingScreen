package io.github.recrivenvi.glass.remote;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;

/** Optional Sable source ownership; native world tickets and plot tickets have separate owners. */
public final class GlassSableServer {
    private GlassSableServer() {}

    public static boolean enabled() {
        return net.neoforged.fml.ModList.get().isLoaded("sable");
    }

    public static Vec3 center(ServerLevel level, BlockPos pos) {
        if (!enabled()) return Vec3.atCenterOf(pos);
        var sub = SableCompanion.INSTANCE.getContaining(level, pos);
        return sub == null
                ? Vec3.atCenterOf(pos)
                : sub.logicalPose().transformPosition(Vec3.atCenterOf(pos));
    }

    public static Scene scene(ServerLevel level, ChunkPos center, int radius) {
        if (!enabled()) return null;
        return new Scene(level, center, radius);
    }

    private static final class Tickets {
        static final SubLevelLoadingTicketType<UUID> TYPE =
                SubLevelLoadingTicketType.create(
                        ResourceLocation.fromNamespaceAndPath("glass", "remote_view"),
                        net.minecraft.core.UUIDUtil.CODEC);
    }

    public static SubLevelLoadingTicketType<UUID> ticketType() {
        return Tickets.TYPE;
    }

    public static final class Scene {
        final ServerLevel level;
        final List<ServerSubLevel> ships = new ArrayList<>();
        final Map<Long, LevelChunk> chunks = new LinkedHashMap<>();

        Scene(ServerLevel level, ChunkPos center, int radius) {
            this.level = level;
            var container = SubLevelContainer.getContainer(level);
            var box =
                    new BoundingBox3d(
                            center.getMinBlockX() - radius * 16,
                            level.getMinBuildHeight(),
                            center.getMinBlockZ() - radius * 16,
                            center.getMaxBlockX() + radius * 16 + 1,
                            level.getMaxBuildHeight(),
                            center.getMaxBlockZ() + radius * 16 + 1);
            for (var sub : container.queryIntersecting(box)) {
                var ship = (ServerSubLevel) sub;
                ships.add(ship);
                for (var holder : ship.getPlot().getLoadedChunks())
                    chunks.put(holder.getChunk().getPos().toLong(), holder.getChunk());
            }
        }

        public List<UUID> ids() {
            return ships.stream().map(ServerSubLevel::getUniqueId).toList();
        }

        public List<Long> chunks() {
            return List.copyOf(chunks.keySet());
        }

        public boolean hasShips() {
            return !ships.isEmpty();
        }

        public LevelChunk chunk(long key) {
            return chunks.get(key);
        }

        public LevelLightEngine lightEngine(long key) {
            var sub = SableCompanion.INSTANCE.getContaining(level, new ChunkPos(key));
            return ((ServerSubLevel) sub).getPlot().getLightEngine();
        }

        public void acquire(UUID owner) {
            var container = SubLevelContainer.getContainer(level);
            for (var ship : ships) container.addForceLoadTicket(ship, Tickets.TYPE, owner);
        }

        public void release(UUID owner) {
            var container = SubLevelContainer.getContainer(level);
            for (var ship : ships) container.removeForceLoadTicket(ship, Tickets.TYPE, owner);
        }

        public ListTag header() {
            ListTag result = new ListTag();
            var origin = SubLevelContainer.getContainer(level).getOrigin();
            for (var ship : ships) {
                CompoundTag tag = new CompoundTag();
                tag.putUUID("id", ship.getUniqueId());
                tag.putInt("plotX", ship.getPlot().plotPos.x - origin.x);
                tag.putInt("plotZ", ship.getPlot().plotPos.z - origin.y);
                tag.put(
                        "pose",
                        Pose3d.CODEC.encodeStart(NbtOps.INSTANCE, ship.logicalPose()).getOrThrow());
                var b = ship.getPlot().getBoundingBox();
                tag.putIntArray(
                        "bounds",
                        new int[] {b.minX(), b.minY(), b.minZ(), b.maxX(), b.maxY(), b.maxZ()});
                result.add(tag);
            }
            return result;
        }
    }
}
