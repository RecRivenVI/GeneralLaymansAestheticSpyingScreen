package io.github.recrivenvi.glass.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.recrivenvi.glass.client.render.GlassProjectorRenderState;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class GlassRenderSnapshotsTest {
    static {
        io.github.recrivenvi.glass.ForgeTestBootstrap.initialize();
    }

    @BeforeAll
    static void bootstrapMinecraft() {
        io.github.recrivenvi.glass.ForgeTestBootstrap.initialize();
    }

    @Test
    void preservesIndependentPropagationDistanceForEveryFaceOnTheSameBlock() {
        BlockPos pos = new BlockPos(99, 29, -12);
        List<GlassStateSnapshot.Surface> surfaces =
                List.of(
                        new GlassStateSnapshot.Surface(
                                GlobalPos.of(Level.OVERWORLD, pos), Direction.UP, 3),
                        new GlassStateSnapshot.Surface(
                                GlobalPos.of(Level.OVERWORLD, pos), Direction.DOWN, 25));

        Map<BlockPos, Map<Direction, Integer>> result =
                GlassRenderSnapshots.surfaceDistances(Level.OVERWORLD, surfaces);

        assertEquals(Map.of(Direction.UP, 3, Direction.DOWN, 25), result.get(pos));
    }

    @Test
    void keepsProjectorIdentityRootWhenItsFrontSurfaceIsBlocked() {
        BlockPos projectorPos = new BlockPos(100, 29, -12);
        BlockPos extensionPos = projectorPos.east();
        UUID channel = UUID.randomUUID();
        GlassStateSnapshot.Network network =
                new GlassStateSnapshot.Network(
                        GlobalPos.of(Level.OVERWORLD, projectorPos),
                        channel,
                        GlobalPos.of(Level.OVERWORLD, new BlockPos(120, 30, -12)),
                        Direction.WEST,
                        Direction.UP,
                        true,
                        1L,
                        List.of(
                                new GlassStateSnapshot.Surface(
                                        GlobalPos.of(Level.OVERWORLD, extensionPos),
                                        Direction.UP,
                                        1)),
                        List.of());

        List<GlassProjectorRenderState> result =
                GlassRenderSnapshots.states(Level.OVERWORLD, List.of(network), 10.0);

        GlassProjectorRenderState root =
                result.stream().filter(state -> state.projector).findFirst().orElseThrow();
        assertEquals(projectorPos, root.pos);
        assertEquals(channel.toString(), root.channel);
        assertTrue(root.active);
        assertTrue(root.activeFaces.isEmpty());
        assertTrue(
                result.stream()
                        .anyMatch(
                                state ->
                                        state.pos.equals(extensionPos)
                                                && !state.projector
                                                && state.activeFaces.contains(Direction.UP)));
    }
}
