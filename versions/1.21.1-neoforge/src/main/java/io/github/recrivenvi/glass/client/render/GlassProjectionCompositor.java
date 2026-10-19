package io.github.recrivenvi.glass.client.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.recrivenvi.glass.content.GlassContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Native framebuffer composition, using the actual wired quads and wireless triangles as masks. */
final class GlassProjectionCompositor implements AutoCloseable {
    enum Pipeline {
        SURFACE(VertexFormat.Mode.QUADS),
        WIRELESS(VertexFormat.Mode.TRIANGLES);
        final VertexFormat.Mode mode;

        Pipeline(VertexFormat.Mode mode) {
            this.mode = mode;
        }

        VertexFormat getVertexFormat() {
            return DefaultVertexFormat.POSITION_COLOR;
        }
    }

    static final Pipeline SURFACE_PIPELINE = Pipeline.SURFACE;
    static final Pipeline WIRELESS_PIPELINE = Pipeline.WIRELESS;
    private static ShaderInstance shader;
    private final GlassStagedVertexBuffer buffer =
            new GlassStagedVertexBuffer(() -> "GLASS composites", 16384);

    static void registerShader(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            GlassContent.id("glass_projective"),
                            DefaultVertexFormat.POSITION_COLOR),
                    value -> shader = value);
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }

    GlassStagedVertexBuffer.Draw appendDraw(Pipeline pipeline) {
        return buffer.appendDraw(pipeline.getVertexFormat(), pipeline.mode);
    }

    VertexConsumer vertices(GlassStagedVertexBuffer.Draw draw) {
        return buffer.getVertexBuilder(draw);
    }

    void upload() {
        buffer.upload();
    }

    void draw(
            Minecraft client,
            Pipeline pipeline,
            TextureTarget input,
            GlassStagedVertexBuffer.Draw draw) {
        if (draw.mesh == null) return;
        if (shader == null)
            throw new IllegalStateException("GLASS projective shader has not loaded");
        var previousShader = RenderSystem.getShader();
        int previousTexture = RenderSystem.getShaderTexture(0);
        float[] previousColor = RenderSystem.getShaderColor().clone();
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        int previousDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        try {
            client.getMainRenderTarget().bindWrite(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableCull();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, input.getColorTextureId());
            BufferUploader.drawWithShader(draw.mesh);
            draw.mesh = null;
        } finally {
            RenderSystem.setShaderColor(
                    previousColor[0], previousColor[1], previousColor[2], previousColor[3]);
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.setShaderTexture(0, previousTexture);
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            RenderSystem.depthFunc(previousDepthFunc);
            RenderSystem.depthMask(depthMask);
            if (!blend) RenderSystem.disableBlend();
            if (!depth) RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull();
        }
    }

    void endFrame() {
        buffer.endFrame();
    }

    @Override
    public void close() {
        buffer.close();
    }
}
