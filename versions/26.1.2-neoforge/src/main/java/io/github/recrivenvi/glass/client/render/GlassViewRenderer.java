package io.github.recrivenvi.glass.client.render;

import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface GlassViewRenderer extends AutoCloseable {
    void replaceViews(List<GlassProjectorRenderState> states);

    void capture(DeltaTracker deltaTracker);

    void drawProjector(LevelRenderState levelState);

    boolean isChannelExtraction();

    void blockChanged(BlockPos pos, int updateFlags);

    void blockStateChanged(BlockPos pos, BlockState oldState, BlockState newState);

    void setSectionDirty(int sectionX, int sectionY, int sectionZ);

    void setSectionDirtyWithNeighbors(int sectionX, int sectionY, int sectionZ);

    void setSectionRangeDirty(
            int minSectionX,
            int minSectionY,
            int minSectionZ,
            int maxSectionX,
            int maxSectionY,
            int maxSectionZ);

    void chunkLoaded(net.minecraft.world.level.ChunkPos pos);

    void reset();

    @Override
    void close();
}
