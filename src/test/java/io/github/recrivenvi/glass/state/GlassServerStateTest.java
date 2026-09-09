package io.github.recrivenvi.glass.state;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

final class GlassServerStateTest {
    @Test
    void rebuildsWhenAStoredNodeOrReachablePropagationChunkChanges() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        UUID channelId = UUID.fromString("1e2d9867-3cf7-4fcb-9f86-07d57b72a9fc");
        GlobalPos terminal = GlobalPos.of(Level.OVERWORLD, new BlockPos(33, 80, -17));
        GlobalPos projector = GlobalPos.of(Level.OVERWORLD, new BlockPos(-1, 80, 48));
        GlobalPos wireless = GlobalPos.of(Level.NETHER, new BlockPos(64, 80, 64));
        GlassStateSnapshot.Terminal terminalState =
                new GlassStateSnapshot.Terminal(
                        terminal,
                        new GlassStateSnapshot.Channel(channelId, "测试", Optional.empty(), true));
        GlassStateSnapshot.Projector projectorState =
                new GlassStateSnapshot.Projector(
                        projector, Optional.of(channelId), List.of(wireless));

        assertTrue(
                shouldRebuild(
                        List.of(terminalState),
                        List.of(projectorState),
                        Level.OVERWORLD,
                        new ChunkPos(2, -2),
                        64));
        assertTrue(
                shouldRebuild(
                        List.of(terminalState),
                        List.of(projectorState),
                        Level.OVERWORLD,
                        new ChunkPos(-1, 3),
                        64));
        assertTrue(
                shouldRebuild(
                        List.of(terminalState),
                        List.of(projectorState),
                        Level.NETHER,
                        new ChunkPos(4, 4),
                        64));
        assertTrue(
                shouldRebuild(
                        List.of(terminalState),
                        List.of(projectorState),
                        Level.OVERWORLD,
                        new ChunkPos(3, 3),
                        64));
        assertFalse(
                shouldRebuild(
                        List.of(terminalState),
                        List.of(projectorState),
                        Level.OVERWORLD,
                        new ChunkPos(4, 4),
                        64));
        assertFalse(
                shouldRebuild(
                        List.of(terminalState),
                        List.of(projectorState),
                        Level.OVERWORLD,
                        new ChunkPos(20, 20),
                        64));
    }

    @Test
    void propagationUsesVanillaFaceOcclusionAndStillRejectsInternalGlassFaces() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        var surface =
                Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState();
        for (Direction face : Direction.values()) {
            assertFalse(
                    GlassServerState.isPropagationFaceExposed(
                            surface, Blocks.OBSIDIAN.defaultBlockState(), face, ignored -> false),
                    "完整实心方块必须阻断 " + face);
            assertTrue(
                    GlassServerState.isPropagationFaceExposed(
                            surface, Blocks.AIR.defaultBlockState(), face, ignored -> false),
                    "空气必须保持外露 " + face);
            assertTrue(
                    GlassServerState.isPropagationFaceExposed(
                            surface, Blocks.GLASS.defaultBlockState(), face, ignored -> false),
                    "原版透明方块必须保留自身非遮挡特性 " + face);
            assertFalse(
                    GlassServerState.isPropagationFaceExposed(
                            surface, Blocks.AIR.defaultBlockState(), face, ignored -> true),
                    "相邻 GLASS 方块无论模型形状如何都必须阻断 " + face);
        }
    }

    private static boolean shouldRebuild(
            List<GlassStateSnapshot.Terminal> terminals,
            List<GlassStateSnapshot.Projector> projectors,
            net.minecraft.resources.ResourceKey<Level> dimension,
            ChunkPos chunkPos,
            int propagationLimit) {
        return GlassServerState.shouldRebuildForChangedChunk(
                terminals, projectors, dimension, chunkPos, propagationLimit);
    }
}
