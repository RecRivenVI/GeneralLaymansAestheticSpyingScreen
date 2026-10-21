package io.github.recrivenvi.glass.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Empty render-only guard cells outside the bounded source; these are never server source chunks.
 */
final class GlassSodiumBoundary {
    private GlassSodiumBoundary() {}

    static java.util.Set<Long> update(
            ClientLevel level, CompoundTag header, java.util.Set<Long> previous) {
        int radius = header.getInt("radius") + 1,
                cx = header.getInt("centerX"),
                cz = header.getInt("centerZ");
        if (!net.neoforged.fml.ModList.get().isLoaded("sodium")) {
            level.getChunkSource().updateViewCenter(cx, cz);
            return java.util.Set.of();
        }
        var source =
                java.util.Arrays.stream(header.getLongArray("sourceChunks"))
                        .boxed()
                        .collect(java.util.stream.Collectors.toSet());
        java.util.Set<Long> next = new java.util.HashSet<>();
        for (int x = -radius; x <= radius; x++)
            for (int z = -radius; z <= radius; z++)
                if (!source.contains(ChunkPos.asLong(cx + x, cz + z)))
                    next.add(ChunkPos.asLong(cx + x, cz + z));
        for (long key : previous)
            if (!next.contains(key)) level.getChunkSource().drop(new ChunkPos(key));
        level.getChunkSource().updateViewCenter(cx, cz);
        // Sodium compiles a chunk only with a complete neighbor ring. Outside this view's
        // explicit source domain the background is empty, matching vanilla's bounded view.
        for (int x = -radius; x <= radius; x++)
            for (int z = -radius; z <= radius; z++) {
                if (source.contains(ChunkPos.asLong(cx + x, cz + z))) continue;
                if (previous.contains(ChunkPos.asLong(cx + x, cz + z))) continue;
                var empty = new LevelChunk(level, new ChunkPos(cx + x, cz + z));
                new ClientboundLevelChunkWithLightPacket(empty, level.getLightEngine(), null, null)
                        .handle(Minecraft.getInstance().getConnection());
            }
        return java.util.Set.copyOf(next);
    }
}
