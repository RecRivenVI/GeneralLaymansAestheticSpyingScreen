package io.github.recrivenvi.glass.state;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

final class GlassStateSnapshotTest {
    static {
        io.github.recrivenvi.glass.ForgeTestBootstrap.initialize();
    }

    @Test
    void roundTripsModernWorldStateAcrossDimensions() {
        io.github.recrivenvi.glass.ForgeTestBootstrap.initialize();
        UUID channelId = UUID.fromString("1e2d9867-3cf7-4fcb-9f86-07d57b72a9fc");
        UUID ownerId = UUID.fromString("ff33da38-45a7-4b38-8324-53e79eea012b");
        GlobalPos terminalPos = GlobalPos.of(Level.NETHER, new BlockPos(400, 80, -300));
        GlobalPos projectorPos = GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 70, 20));
        GlobalPos wirelessPos = GlobalPos.of(Level.OVERWORLD, new BlockPos(14, 72, 25));
        GlassStateSnapshot.Channel channel =
                new GlassStateSnapshot.Channel(channelId, "跨维度演示：北区", Optional.of(ownerId), false);
        GlassStateSnapshot snapshot =
                new GlassStateSnapshot(
                        List.of(new GlassStateSnapshot.Terminal(terminalPos, channel)),
                        List.of(
                                new GlassStateSnapshot.Projector(
                                        projectorPos,
                                        Optional.of(channelId),
                                        List.of(wirelessPos))),
                        List.of(
                                new GlassStateSnapshot.Network(
                                        projectorPos,
                                        channelId,
                                        terminalPos,
                                        Direction.UP,
                                        Direction.WEST,
                                        true,
                                        42L,
                                        List.of(
                                                new GlassStateSnapshot.Surface(
                                                        projectorPos, Direction.WEST, 0)),
                                        List.of(wirelessPos))),
                        new GlassStateSnapshot.Settings(2, 10, 512, 2, 512, 80, 12, true));

        assertEquals(snapshot, GlassStateSnapshot.fromTag(snapshot.toTag()));
    }
}
