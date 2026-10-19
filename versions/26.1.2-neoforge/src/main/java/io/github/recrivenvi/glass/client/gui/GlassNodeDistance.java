package io.github.recrivenvi.glass.client.gui;

import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;

final class GlassNodeDistance {
    private GlassNodeDistance() {}

    static double between(GlobalPos origin, GlobalPos target) {
        if (!origin.dimension().equals(target.dimension())) return Double.POSITIVE_INFINITY;
        return Math.sqrt(origin.pos().distSqr(target.pos()));
    }

    static <T> Comparator<T> comparator(GlobalPos origin, Function<T, GlobalPos> position) {
        return Comparator.comparingDouble((T value) -> between(origin, position.apply(value)))
                .thenComparing(value -> position.apply(value).dimension().identifier().toString())
                .thenComparingInt(value -> position.apply(value).pos().getX())
                .thenComparingInt(value -> position.apply(value).pos().getY())
                .thenComparingInt(value -> position.apply(value).pos().getZ());
    }

    static Component description(GlobalPos origin, GlobalPos target) {
        double distance = between(origin, target);
        if (!Double.isFinite(distance)) {
            return Component.translatable("glass.gui.distance_cross_dimension");
        }
        return Component.translatable(
                "glass.gui.distance", String.format(Locale.ROOT, "%.1f", distance));
    }

    static Component descriptionWithCoordinates(GlobalPos origin, GlobalPos target) {
        return Component.empty()
                .append(description(origin, target))
                .append("  ·  ")
                .append(GlassGuiTheme.coordinates(target.pos()));
    }

    static boolean possiblyUnloaded(GlobalPos target) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !client.level.dimension().equals(target.dimension())) {
            return true;
        }
        BlockPos pos = target.pos();
        if (!client.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) return true;
        if (client.player == null) return true;
        double visibleRadius = client.options.getEffectiveRenderDistance() * 16.0 + 16.0;
        return pos.distToCenterSqr(client.player.position()) > visibleRadius * visibleRadius;
    }
}
