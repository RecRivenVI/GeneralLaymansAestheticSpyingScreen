package me.ichun.mods.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import net.minecraft.client.renderer.state.level.ChunkLoadingRenderState;
import org.junit.jupiter.api.Test;

final class GlassChunkLoadingStateIsolationTest {
    @Test
    void detachesEveryChunkTrackingSetFromTheMainWorldState() {
        ChunkLoadingRenderState source = new ChunkLoadingRenderState();
        source.addedEmptySections.add(1L);
        source.removedEmptySections.add(2L);
        source.addedLoadedChunks.add(3L);
        source.removedLoadedChunks.add(4L);
        source.loadedExpectedChunks.add(5L);
        ChunkLoadingRenderState target = new ChunkLoadingRenderState();
        GlassChunkLoadingStateIsolation isolation = new GlassChunkLoadingStateIsolation();

        isolation.copyDetached(source, target);

        assertDetachedCopy(source.addedEmptySections, target.addedEmptySections, 1L);
        assertDetachedCopy(source.removedEmptySections, target.removedEmptySections, 2L);
        assertDetachedCopy(source.addedLoadedChunks, target.addedLoadedChunks, 3L);
        assertDetachedCopy(source.removedLoadedChunks, target.removedLoadedChunks, 4L);
        assertDetachedCopy(source.loadedExpectedChunks, target.loadedExpectedChunks, 5L);
    }

    @Test
    void reusesTheChannelOwnedSetsWithoutKeepingStaleEntries() {
        ChunkLoadingRenderState source = new ChunkLoadingRenderState();
        ChunkLoadingRenderState target = new ChunkLoadingRenderState();
        GlassChunkLoadingStateIsolation isolation = new GlassChunkLoadingStateIsolation();
        source.addedLoadedChunks.add(11L);
        isolation.copyDetached(source, target);
        it.unimi.dsi.fastutil.longs.LongOpenHashSet firstCopy = target.addedLoadedChunks;

        source.addedLoadedChunks.clear();
        source.addedLoadedChunks.add(12L);
        isolation.copyDetached(source, target);

        assertSame(firstCopy, target.addedLoadedChunks);
        assertEquals(java.util.Set.of(12L), target.addedLoadedChunks);
    }

    private static void assertDetachedCopy(
            it.unimi.dsi.fastutil.longs.LongOpenHashSet source,
            it.unimi.dsi.fastutil.longs.LongOpenHashSet target,
            long expected) {
        assertNotSame(source, target);
        assertEquals(java.util.Set.of(expected), target);
        target.clear();
        assertEquals(java.util.Set.of(expected), source);
    }
}
