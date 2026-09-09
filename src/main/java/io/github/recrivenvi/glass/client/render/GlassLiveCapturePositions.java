package io.github.recrivenvi.glass.client.render;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.core.BlockPos;

/** 关闭过渡结束前，距离与视锥门禁仍需把正在消失的几何视为实时投影。 */
final class GlassLiveCapturePositions {
    private GlassLiveCapturePositions() {}

    static List<BlockPos> combine(
            Collection<BlockPos> active,
            Collection<BlockPos> closingSurfaces,
            Collection<BlockPos> closingWireless) {
        LinkedHashSet<BlockPos> positions = new LinkedHashSet<>();
        positions.addAll(active);
        positions.addAll(closingSurfaces);
        positions.addAll(closingWireless);
        return List.copyOf(positions);
    }
}
