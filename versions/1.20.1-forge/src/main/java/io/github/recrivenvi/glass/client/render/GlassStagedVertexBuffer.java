package io.github.recrivenvi.glass.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.function.Supplier;

/** Owns the native BufferBuilder's consecutive draw ranges for one frame. */
final class GlassStagedVertexBuffer implements AutoCloseable {
    private final BufferBuilder builder;
    private final ArrayList<Draw> draws = new ArrayList<>();
    private Draw current;

    GlassStagedVertexBuffer(Supplier<String> label, int capacity) {
        builder = new BufferBuilder(capacity);
    }

    Draw appendDraw(VertexFormat format, VertexFormat.Mode mode) {
        finishCurrent();
        current = new Draw();
        draws.add(current);
        builder.begin(mode, format);
        return current;
    }

    VertexConsumer getVertexBuilder(Draw draw) {
        if (draw != current)
            throw new IllegalStateException("GLASS draw is not the active vertex range");
        return builder;
    }

    private void finishCurrent() {
        if (current != null) {
            current.mesh = builder.endOrDiscardIfEmpty();
            current = null;
        }
    }

    void upload() {
        finishCurrent();
    }

    void endFrame() {
        for (Draw draw : draws)
            if (draw.mesh != null) {
                draw.mesh.release();
                draw.mesh = null;
            }
        draws.clear();
        builder.discard();
        current = null;
    }

    @Override
    public void close() {
        endFrame();
    }

    static final class Draw {
        BufferBuilder.RenderedBuffer mesh;
    }
}
