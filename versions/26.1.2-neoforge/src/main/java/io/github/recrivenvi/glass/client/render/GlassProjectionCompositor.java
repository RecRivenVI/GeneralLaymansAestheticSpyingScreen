package io.github.recrivenvi.glass.client.render;

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
import io.github.recrivenvi.glass.content.GlassContent;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Owns the GPU pipeline, staging buffer, resource binding, and main-target composite pass. */
final class GlassProjectionCompositor implements AutoCloseable {
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    static final RenderPipeline SURFACE_PIPELINE =
            createPipeline("surface", VertexFormat.Mode.QUADS);
    static final RenderPipeline WIRELESS_PIPELINE =
            createPipeline("wireless", VertexFormat.Mode.TRIANGLES);

    static void registerPipelines(
            net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) {
        event.registerPipeline(SURFACE_PIPELINE);
        event.registerPipeline(WIRELESS_PIPELINE);
    }

    private final GlassStagedVertexBuffer buffer =
            new GlassStagedVertexBuffer(() -> "GLASS channel composites", 16384);

    GlassStagedVertexBuffer.Draw appendDraw(RenderPipeline pipeline) {
        VertexFormat format = pipeline.getVertexFormat();
        return format == null ? null : buffer.appendDraw(format, pipeline.getVertexFormatMode());
    }

    VertexConsumer vertices(GlassStagedVertexBuffer.Draw draw) {
        return buffer.getVertexBuilder(draw);
    }

    void upload() {
        buffer.upload();
    }

    void draw(
            Minecraft client,
            RenderPipeline pipeline,
            TextureTarget inputTarget,
            GlassStagedVertexBuffer.Draw draw) {
        GlassStagedVertexBuffer.ExecuteInfo info = buffer.getExecuteInfo(draw);
        if (info == null) return;
        GpuTextureView input = inputTarget.getColorTextureView();
        RenderTarget output = client.getMainRenderTarget();
        if (input == null || output.getColorTextureView() == null) return;
        GpuBufferSlice transforms =
                RenderSystem.getDynamicUniforms()
                        .writeTransform(
                                new Matrix4f(RenderSystem.getModelViewStack()),
                                COLOR_MODULATOR,
                                MODEL_OFFSET,
                                TEXTURE_MATRIX);
        try (RenderPass pass =
                RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                                () -> "GLASS channel composite",
                                output.getColorTextureView(),
                                OptionalInt.empty(),
                                output.getDepthTextureView(),
                                OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.bindTexture(
                    "Sampler0",
                    input,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            pass.setVertexBuffer(0, info.vertexBuffer());
            pass.setIndexBuffer(info.indexBuffer(), info.indexType());
            pass.drawIndexed(0, 0, info.indexCount(), 1);
        }
    }

    void endFrame() {
        buffer.endFrame();
    }

    @Override
    public void close() {
        buffer.close();
    }

    private static RenderPipeline createPipeline(String name, VertexFormat.Mode topology) {
        return RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                .withSampler("Sampler0")
                .withLocation(GlassContent.id("pipeline/" + name))
                .withVertexShader(GlassContent.id("core/glass_projective"))
                .withFragmentShader(GlassContent.id("core/glass_projective"))
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, topology)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(DepthStencilState.DEFAULT)
                .withCull(false)
                .build();
    }
}
