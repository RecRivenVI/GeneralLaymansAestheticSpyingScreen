package io.github.recrivenvi.glass.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.function.Supplier;

/** Batches portal geometry for the native immediate draw API. */
final class GlassStagedVertexBuffer implements AutoCloseable {
    private final int capacity;
    private final ArrayList<Draw> draws = new ArrayList<>();

    GlassStagedVertexBuffer(Supplier<String> label, int capacity) {
        this.capacity = capacity;
    }

    Draw appendDraw(VertexFormat format, VertexFormat.Mode mode) {
        Draw draw = new Draw(format, mode, capacity);
        draws.add(draw);
        return draw;
    }

    VertexConsumer getVertexBuilder(Draw draw) {
        return draw.builder;
    }

    void upload() {
        for (Draw draw : draws) draw.mesh = draw.builder.build();
    }

    void endFrame() {
        for (Draw draw : draws) {
            if (draw.mesh != null) draw.mesh.close();
            draw.storage.close();
        }
        draws.clear();
    }

    @Override
    public void close() {
        endFrame();
    }

    static final class Draw {
        final ByteBufferBuilder storage;
        final BufferBuilder builder;
        MeshData mesh;

        Draw(VertexFormat format, VertexFormat.Mode mode, int capacity) {
            storage = new ByteBufferBuilder(capacity);
            builder = new BufferBuilder(storage, mode, format);
        }
    }
}
