package io.github.recrivenvi.glass.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.function.Supplier;

/** Owns CPU and GPU batches for a frame on the 26.1 Blaze3D API. */
final class GlassStagedVertexBuffer implements AutoCloseable {
    private final Supplier<String> label;
    private final int capacity;
    private final ArrayList<Draw> draws = new ArrayList<>();

    GlassStagedVertexBuffer(Supplier<String> label, int capacity) {
        this.label = label;
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
        var counts = new java.util.EnumMap<VertexFormat.Mode, Integer>(VertexFormat.Mode.class);
        for (Draw draw : draws) {
            draw.mesh = draw.builder.build();
            if (draw.mesh == null) continue;
            draw.indexCount = draw.mesh.drawState().indexCount();
            if (draw.mesh.indexBuffer() == null)
                counts.merge(draw.mode, draw.indexCount, Math::max);
        }
        // Grow each shared sequential buffer before any draw keeps a reference to it.
        counts.forEach((mode, count) -> RenderSystem.getSequentialBuffer(mode).getBuffer(count));
        for (Draw draw : draws) {
            if (draw.mesh == null) continue;
            draw.vertex =
                    RenderSystem.getDevice()
                            .createBuffer(label, GpuBuffer.USAGE_VERTEX, draw.mesh.vertexBuffer());
            if (draw.mesh.indexBuffer() != null) {
                draw.index =
                        RenderSystem.getDevice()
                                .createBuffer(
                                        label, GpuBuffer.USAGE_INDEX, draw.mesh.indexBuffer());
                draw.indexType = draw.mesh.drawState().indexType();
                draw.ownsIndex = true;
            } else {
                var sequential = RenderSystem.getSequentialBuffer(draw.mode);
                draw.index = sequential.getBuffer(draw.indexCount);
                draw.indexType = sequential.type();
            }
        }
    }

    ExecuteInfo getExecuteInfo(Draw draw) {
        return draw.vertex == null
                ? null
                : new ExecuteInfo(draw.vertex, draw.index, draw.indexType, draw.indexCount);
    }

    void endFrame() {
        for (Draw draw : draws) {
            if (draw.vertex != null) draw.vertex.close();
            if (draw.ownsIndex && draw.index != null) draw.index.close();
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
        final VertexFormat.Mode mode;
        MeshData mesh;
        GpuBuffer vertex;
        GpuBuffer index;
        VertexFormat.IndexType indexType;
        int indexCount;
        boolean ownsIndex;

        Draw(VertexFormat format, VertexFormat.Mode mode, int capacity) {
            storage = new ByteBufferBuilder(capacity);
            builder = new BufferBuilder(storage, mode, format);
            this.mode = mode;
        }
    }

    record ExecuteInfo(
            GpuBuffer vertexBuffer,
            GpuBuffer indexBuffer,
            VertexFormat.IndexType indexType,
            int indexCount) {}
}
