package io.github.recrivenvi.glass.client.render;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.renderer.state.level.ChunkLoadingRenderState;

final class GlassChunkLoadingStateIsolation {
    private final LongOpenHashSet addedEmptySections = new LongOpenHashSet();
    private final LongOpenHashSet removedEmptySections = new LongOpenHashSet();
    private final LongOpenHashSet addedLoadedChunks = new LongOpenHashSet();
    private final LongOpenHashSet removedLoadedChunks = new LongOpenHashSet();
    private final LongOpenHashSet loadedExpectedChunks = new LongOpenHashSet();

    void copyDetached(ChunkLoadingRenderState source, ChunkLoadingRenderState target) {
        // LevelExtractor 暂时把这些字段指向 ClientChunkCache 的双缓冲集合；不能就地清空，否则会破坏主世界下一帧的增量通知。
        target.addedEmptySections = copy(source.addedEmptySections, addedEmptySections);
        target.removedEmptySections = copy(source.removedEmptySections, removedEmptySections);
        target.addedLoadedChunks = copy(source.addedLoadedChunks, addedLoadedChunks);
        target.removedLoadedChunks = copy(source.removedLoadedChunks, removedLoadedChunks);
        target.loadedExpectedChunks = copy(source.loadedExpectedChunks, loadedExpectedChunks);
    }

    private static LongOpenHashSet copy(LongOpenHashSet source, LongOpenHashSet detached) {
        detached.clear();
        detached.addAll(source);
        return detached;
    }
}
