package me.ichun.mods.glass.client.gui;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import me.ichun.mods.glass.content.GlassContent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

/** 将预览中的浮点线段与三角形合并成一个 GUI 批次，避免先量化为 GUI 整数像素。 */
final class GlassVectorRenderState implements GuiElementRenderState {
    private static final RenderPipeline PIPELINE =
            RenderPipelines.register(
                    RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                            .withLocation(GlassContent.id("pipeline/gui_vector"))
                            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                            .withCull(false)
                            .build());

    private final Matrix3x2fc pose;
    private final List<Vertex> vertices;
    private final @Nullable ScreenRectangle scissorArea;
    private final @Nullable ScreenRectangle bounds;

    private GlassVectorRenderState(
            Matrix3x2fc pose,
            List<Vertex> vertices,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds) {
        this.pose = pose;
        this.vertices = vertices;
        this.scissorArea = scissorArea;
        this.bounds = bounds;
    }

    static Builder builder() {
        return new Builder();
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        for (Vertex vertex : vertices) {
            vertexConsumer
                    .addVertexWith2DPose(pose, vertex.x(), vertex.y())
                    .setColor(vertex.color());
        }
    }

    @Override
    public RenderPipeline pipeline() {
        return PIPELINE;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public @Nullable ScreenRectangle scissorArea() {
        return scissorArea;
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return bounds;
    }

    static final class Builder {
        private final ArrayList<Vertex> vertices = new ArrayList<>();

        void triangle(Point a, Point b, Point c, int color) {
            vertices.add(new Vertex(a.x(), a.y(), color));
            vertices.add(new Vertex(b.x(), b.y(), color));
            vertices.add(new Vertex(c.x(), c.y(), color));
        }

        void line(Point from, Point to, float width, int color) {
            float dx = to.x() - from.x();
            float dy = to.y() - from.y();
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length < 0.0001F) return;

            float halfWidth = width * 0.5F;
            float normalX = -dy / length * halfWidth;
            float normalY = dx / length * halfWidth;
            Point a = new Point(from.x() + normalX, from.y() + normalY);
            Point b = new Point(from.x() - normalX, from.y() - normalY);
            Point c = new Point(to.x() - normalX, to.y() - normalY);
            Point d = new Point(to.x() + normalX, to.y() + normalY);
            triangle(a, b, c, color);
            triangle(a, c, d, color);
        }

        void submit(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
            if (vertices.isEmpty()) return;
            Matrix3x2f pose = new Matrix3x2f(graphics.pose());
            ScreenRectangle widgetBounds =
                    new ScreenRectangle(x, y, width, height).transformMaxBounds(pose);
            ScreenRectangle scissor = graphics.scissorStack.peek();
            ScreenRectangle clippedBounds =
                    scissor == null ? widgetBounds : scissor.intersection(widgetBounds);
            if (clippedBounds == null) return;
            graphics.guiRenderState.addGuiElement(
                    new GlassVectorRenderState(
                            pose, List.copyOf(vertices), scissor, clippedBounds));
        }
    }

    record Point(float x, float y) {}

    private record Vertex(float x, float y, int color) {}
}
