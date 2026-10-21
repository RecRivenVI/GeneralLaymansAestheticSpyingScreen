package io.github.recrivenvi.glass.remote;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelObserver;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelTicketInfo;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import io.github.recrivenvi.glass.state.GlassSavedState;
import io.github.recrivenvi.glass.state.GlassServerState;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

/** Native Sable save pointers keep server-owned GLASS links usable after source unloading. */
public final class GlassSablePersistence {
    private GlassSablePersistence() {}

    public static void initialize() {
        if (GlassSableServer.enabled()) GlassSableServer.ticketType();
    }

    public static void start(MinecraftServer server) {
        if (!GlassSableServer.enabled()) return;
        for (var level : server.getAllLevels())
            SubLevelContainer.getContainer(level)
                    .addObserver(
                            new SubLevelObserver() {
                                @Override
                                public void onSubLevelRemoved(
                                        SubLevel sub, SubLevelRemovalReason reason) {
                                    if (reason != SubLevelRemovalReason.REMOVED) return;
                                    var saved = GlassServerState.saved(server);
                                    var positions =
                                            saved.sableLinks().entrySet().stream()
                                                    .filter(
                                                            e ->
                                                                    e.getKey()
                                                                                    .dimension()
                                                                                    .equals(
                                                                                            level
                                                                                                    .dimension())
                                                                            && e.getValue()
                                                                                    .getUUID("id")
                                                                                    .equals(
                                                                                            sub
                                                                                                    .getUniqueId()))
                                                    .map(java.util.Map.Entry::getKey)
                                                    .toList();
                                    for (var pos : positions) saved.remove(pos);
                                    if (!positions.isEmpty())
                                        GlassServerState.topologyChanged(level);
                                }
                            });
    }

    public static void remember(ServerLevel level, GlobalPos pos) {
        var sub =
                dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.getContaining(
                        level, pos.pos());
        if (!(sub instanceof ServerSubLevel ship)) return;
        var saved = GlassServerState.saved(level.getServer());
        var link = new CompoundTag();
        link.putUUID("id", ship.getUniqueId());
        var pointer = ship.getLastSerializationPointer();
        if (pointer != null)
            link.put(
                    "pointer",
                    GlobalSavedSubLevelPointer.CODEC
                            .encodeStart(NbtOps.INSTANCE, pointer)
                            .getOrThrow());
        saved.sableLinks().put(pos, link);
        saved.setDirty();
    }

    public static boolean ensureLoaded(ServerLevel level, GlobalPos pos) {
        if (!GlassSableServer.enabled()) return true;
        var link = GlassServerState.saved(level.getServer()).sableLinks().get(pos);
        if (link == null
                || dev.ryanhcode.sable.companion.SableCompanion.INSTANCE.getContaining(
                                level, pos.pos())
                        != null) return true;
        var container = SubLevelContainer.getContainer(level);
        var holding = container.getHoldingChunkMap().getHoldingSubLevel(link.getUUID("id"));
        if (holding != null) container.getHoldingChunkMap().loadHoldingSubLevel(holding);
        else
            container
                    .getHoldingChunkMap()
                    .snatchAndLoad(
                            GlobalSavedSubLevelPointer.CODEC
                                    .parse(NbtOps.INSTANCE, link.get("pointer"))
                                    .getOrThrow(),
                            link.getUUID("id"));
        return container.getSubLevel(link.getUUID("id")) != null;
    }

    public static void pointerChanged(
            ServerLevel level, UUID id, GlobalSavedSubLevelPointer pointer) {
        var saved = GlassServerState.saved(level.getServer());
        saved.sableLinks()
                .forEach(
                        (pos, link) -> {
                            if (pos.dimension().equals(level.dimension())
                                    && link.getUUID("id").equals(id)) {
                                link.put(
                                        "pointer",
                                        GlobalSavedSubLevelPointer.CODEC
                                                .encodeStart(NbtOps.INSTANCE, pointer)
                                                .getOrThrow());
                                saved.setDirty();
                            }
                        });
    }

    public static void flush(ServerLevel level) {
        // Sable's save can run after vanilla SavedData saving; persist updated pointers after its
        // storage flush.
        var server = level.getServer();
        var saved = GlassServerState.saved(server);
        saved.save(
                server.getWorldPath(LevelResource.ROOT)
                        .resolve("data")
                        .resolve(GlassSavedState.ID + ".dat")
                        .toFile(),
                server.registryAccess());
    }

    public static Object2ObjectMap<UUID, SubLevelTicketInfo> restoredTickets(
            Object2ObjectMap<UUID, SubLevelTicketInfo> incoming) {
        var result = new Object2ObjectOpenHashMap<UUID, SubLevelTicketInfo>();
        incoming.forEach(
                (id, info) -> {
                    var tickets =
                            new it.unimi.dsi.fastutil.objects.ObjectOpenHashSet<>(info.tickets());
                    tickets.removeIf(ticket -> ticket.type() == GlassSableServer.ticketType());
                    if (!tickets.isEmpty())
                        result.put(id, new SubLevelTicketInfo(info.getPointer(), tickets));
                });
        return result;
    }
}
