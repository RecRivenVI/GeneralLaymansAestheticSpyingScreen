package io.github.recrivenvi.glass.state;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

final class GlassChannelAccessTest {
    private static final UUID CHANNEL_ID = UUID.fromString("1e2d9867-3cf7-4fcb-9f86-07d57b72a9fc");
    private static final UUID OWNER_ID = UUID.fromString("4aa607bf-9b47-4c5c-8a9d-f753f66f59f4");
    private static final UUID VISITOR_ID = UUID.fromString("dad71bc2-05ed-4f89-8324-d68c6b3a2f4e");
    private static final GlobalPos PROJECTOR_POS = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
    private static final GlobalPos TERMINAL_POS =
            GlobalPos.of(Level.OVERWORLD, new BlockPos(4, 0, 0));

    @Test
    void rejectsBoundProjectorWhenItsTerminalCannotBeResolved() {
        assertFalse(GlassServerState.canAccess(boundProjector(), List.of(), OWNER_ID));
    }

    @Test
    void rejectsPrivateChannelForNonOwner() {
        assertFalse(
                GlassServerState.canAccess(boundProjector(), List.of(terminal(false)), VISITOR_ID));
    }

    @Test
    void allowsPrivateChannelForOwner() {
        assertTrue(
                GlassServerState.canAccess(boundProjector(), List.of(terminal(false)), OWNER_ID));
    }

    @Test
    void allowsPublicChannelForNonOwner() {
        assertTrue(
                GlassServerState.canAccess(boundProjector(), List.of(terminal(true)), VISITOR_ID));
    }

    @Test
    void allowsUnboundProjectorWithoutAnyTerminal() {
        GlassStateSnapshot.Projector projector =
                new GlassStateSnapshot.Projector(PROJECTOR_POS, Optional.empty(), List.of());

        assertTrue(GlassServerState.canAccess(projector, List.of(), VISITOR_ID));
    }

    private static GlassStateSnapshot.Projector boundProjector() {
        return new GlassStateSnapshot.Projector(PROJECTOR_POS, Optional.of(CHANNEL_ID), List.of());
    }

    private static GlassStateSnapshot.Terminal terminal(boolean publicChannel) {
        return new GlassStateSnapshot.Terminal(
                TERMINAL_POS,
                new GlassStateSnapshot.Channel(
                        CHANNEL_ID, "测试频道", Optional.of(OWNER_ID), publicChannel));
    }
}
