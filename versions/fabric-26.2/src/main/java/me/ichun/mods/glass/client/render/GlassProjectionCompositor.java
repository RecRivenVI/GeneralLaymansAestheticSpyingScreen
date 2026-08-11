package me.ichun.mods.glass.client.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Optional;
import java.util.OptionalDouble;
import me.ichun.mods.glass.content.GlassContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Owns the GPU pipeline, staging buffer, resource binding, and main-target composite pass. */
final class GlassProjectionCompositor implements AutoCloseable {
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    static final RenderPipeline SURFACE_PIPELINE =
            createPipeline("surface", PrimitiveTopology.QUADS);
    static final RenderPipeline WIRELESS_PIPELINE =
            createPipeline("wireless", PrimitiveTopology.TRIANGLES);

    private final StagedVertexBuffer buffer =
            new StagedVertexBuffer(() -> "GLASS channel composites", 16384);

    StagedVertexBuffer.Draw appendDraw(RenderPipeline pipeline) {
        VertexFormat format = pipeline.getVertexFormatBinding(0);
        return format == null ? null : buffer.appendDraw(format, pipeline.getPrimitiveTopology());
    }

    VertexConsumer vertices(StagedVertexBuffer.Draw draw) {
        return buffer.getVertexBuilder(draw);
    }

    void upload() {
        buffer.upload();
    }

    void draw(
            Minecraft client,
            RenderPipeline pipeline,
            TextureTarget inputTarget,
            StagedVertexBuffer.Draw draw) {
        StagedVertexBuffer.ExecuteInfo info = buffer.getExecuteInfo(draw);
        if (info == null) return;
        GpuTextureView input = inputTarget.getColorTextureView();
        RenderTarget output = client.gameRenderer.mainRenderTarget();
        if (input == null || output.getColorTextureView() == null) return;
        GpuBufferSlice transforms =
                RenderSystem.getDynamicUniforms()
                        .writeTransform(
                                RenderSystem.getModelViewMatrixCopy(),
                                COLOR_MODULATOR,
                                MODEL_OFFSET,
                                TEXTURE_MATRIX);
        try (RenderPass pass =
                RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                                () -> "GLASS channel composite",
                                output.getColorTextureView(),
                                Optional.empty(),
                                output.getDepthTextureView(),
                                OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.bindTexture(
                    "Sampler0",
                    input,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, info.vertexBuffer().slice());
            pass.setIndexBuffer(info.indexBuffer(), info.indexType());
            pass.drawIndexed(info.indexCount(), 1, info.firstIndex(), info.baseVertex(), 0);
        }
    }

    void endFrame() {
        buffer.endFrame();
    }

    @Override
    public void close() {
        buffer.close();
    }

    private static RenderPipeline createPipeline(String name, PrimitiveTopology topology) {
        return RenderPipelines.register(
                RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                        .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                        .withLocation(GlassContent.id("pipeline/" + name))
                        .withVertexShader(GlassContent.id("core/glass_projective"))
                        .withFragmentShader(GlassContent.id("core/glass_projective"))
                        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                        .withPrimitiveTopology(topology)
                        .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                        .withDepthStencilState(DepthStencilState.DEFAULT)
                        .withCull(false)
                        .build());
    }
}
