package io.github.recrivenvi.glass.client;

import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;

/** 客户端只读镜像；不从方块实体发现或推断任何 GLASS 数据。 */
public final class GlassClientState {
    private static final GlassStateSnapshot EMPTY =
            new GlassStateSnapshot(
                    List.of(),
                    List.of(),
                    List.of(),
                    new GlassStateSnapshot.Settings(2, 8, 256, 2, 256, 64, 8, false));
    private static GlassStateSnapshot snapshot = EMPTY;

    private GlassClientState() {}

    public static GlassStateSnapshot snapshot() {
        return snapshot;
    }

    public static void replace(GlassStateSnapshot value) {
        snapshot = value;
    }

    public static void clear() {
        snapshot = EMPTY;
    }

    public static Optional<GlassStateSnapshot.Terminal> terminal(GlobalPos pos) {
        return snapshot.terminals().stream().filter(value -> value.pos().equals(pos)).findFirst();
    }

    public static Optional<GlassStateSnapshot.Projector> projector(GlobalPos pos) {
        return snapshot.projectors().stream().filter(value -> value.pos().equals(pos)).findFirst();
    }

    public static Optional<GlassStateSnapshot.Terminal> terminal(UUID channelId) {
        return snapshot.terminals().stream()
                .filter(value -> value.channel().id().equals(channelId))
                .findFirst();
    }
}
