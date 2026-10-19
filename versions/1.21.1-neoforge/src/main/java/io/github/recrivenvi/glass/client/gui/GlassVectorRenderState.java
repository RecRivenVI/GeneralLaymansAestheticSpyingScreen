package io.github.recrivenvi.glass.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;

/** Draws the floating-point preview geometry through the native immediate GUI pipeline. */
final class GlassVectorRenderState {
    static Builder builder() {
        return new Builder();
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

        void submit(GuiGraphics graphics, int x, int y, int width, int height) {
            if (vertices.isEmpty()) return;
            graphics.flush();
            var builder =
                    Tesselator.getInstance()
                            .begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
            var pose = graphics.pose().last().pose();
            for (Vertex vertex : vertices)
                builder.addVertex(pose, vertex.x(), vertex.y(), 0).setColor(vertex.color());
            var previous = RenderSystem.getShader();
            boolean blend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            BufferUploader.drawWithShader(builder.buildOrThrow());
            RenderSystem.setShader(() -> previous);
            if (!blend) RenderSystem.disableBlend();
        }
    }

    record Point(float x, float y) {}

    private record Vertex(float x, float y, int color) {}
}
