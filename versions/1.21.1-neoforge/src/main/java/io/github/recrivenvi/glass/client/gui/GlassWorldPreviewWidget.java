package io.github.recrivenvi.glass.client.gui;

import io.github.recrivenvi.glass.model.GlassPreviewCameraState;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/** 可复用的示波器风格世界预览。它直接读取客户端当前拥有的方块状态和选择轮廓，不使用 截图或预制缩略图；未加载节点用单位立方体占位并由外部列表给出警告。 */
final class GlassWorldPreviewWidget extends AbstractWidget {
    private static final int GRID = 0x5533653f;
    private static final int WORLD = 0x99658f72;
    private static final int NODE = 0xff72f2ca;
    private static final int PROJECTOR = 0xff56d8e8;
    private static final int SELECTED = 0xffffe082;
    private static final int TRIANGLE = 0x995de6ff;
    private static final int MAX_CONTEXT_BLOCKS = 128;
    private static final float LINE_WIDTH = 0.65F;
    private static final float VIEW_PADDING_RATIO = 0.08F;
    private static final float MIN_VIEW_PADDING = 0.5F;
    private static final float VIEWPORT_PADDING = 12.0F;
    private static final float GRID_TARGET_PIXELS = 16.0F;
    private static final int MAX_GRID_RADIUS = 65_536;

    private final GlobalPos origin;
    private final Direction facing;
    private final Consumer<GlassPreviewCameraState> cameraStateSink;
    private final ArrayList<GlobalPos> nodes = new ArrayList<>();
    private GlobalPos selected;
    private float yawOffset;
    private float pitchOffset;
    private float zoom;
    private float panX;
    private float panY;
    private int dragButton = -1;
    private int visibleNodes;

    GlassWorldPreviewWidget(
            int x,
            int y,
            int width,
            int height,
            GlobalPos origin,
            Direction facing,
            List<GlobalPos> nodes) {
        this(x, y, width, height, origin, facing, nodes, GlassPreviewPreferences.get());
    }

    private GlassWorldPreviewWidget(
            int x,
            int y,
            int width,
            int height,
            GlobalPos origin,
            Direction facing,
            List<GlobalPos> nodes,
            GlassPreviewPreferences preferences) {
        this(x, y, width, height, origin, facing, nodes, preferences.state(), preferences::save);
    }

    GlassWorldPreviewWidget(
            int x,
            int y,
            int width,
            int height,
            GlobalPos origin,
            Direction facing,
            List<GlobalPos> nodes,
            GlassPreviewCameraState cameraState,
            Consumer<GlassPreviewCameraState> cameraStateSink) {
        super(x, y, width, height, Component.translatable("glass.gui.preview"));
        this.origin = origin;
        this.facing = facing;
        this.cameraStateSink = cameraStateSink;
        this.nodes.addAll(nodes);
        visibleNodes = nodes.size();
        applyCameraState(cameraState);
    }

    void setNodes(List<GlobalPos> values) {
        int progress = visibleNodes;
        nodes.clear();
        nodes.addAll(values);
        visibleNodes = Math.min(progress, nodes.size());
    }

    void setSelected(GlobalPos value) {
        selected = value;
    }

    int visibleNodeCount() {
        return visibleNodes;
    }

    void previousNode() {
        visibleNodes = Math.max(0, visibleNodes - 1);
    }

    void nextNode() {
        visibleNodes = Math.min(nodes.size(), visibleNodes + 1);
    }

    void recenter() {
        ViewFrame frame = topologyFrame();
        float paddedWidth = frame.width() + 2.0F * viewPadding(frame.width());
        float paddedHeight = frame.height() + 2.0F * viewPadding(frame.height());
        float viewportWidth = Math.max(1.0F, getWidth() - 2.0F * VIEWPORT_PADDING);
        float viewportHeight = Math.max(1.0F, getHeight() - 2.0F * VIEWPORT_PADDING);
        float viewWidth =
                Math.max(
                        GlassPreviewCameraState.DEFAULT_VIEW_WIDTH,
                        Math.max(paddedWidth, paddedHeight * viewportWidth / viewportHeight));
        float scale = viewportWidth / viewWidth;
        applyCameraState(
                new GlassPreviewCameraState(
                        0.0F, 0.0F, viewWidth, -frame.centerX() * scale, frame.centerY() * scale));
        persistCameraState();
    }

    boolean hasActiveDrag() {
        return dragButton >= 0;
    }

    private long lastClickTime;

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (!active || !visible || !isMouseOver(x, y) || !isValidClickButton(button)) return false;
        dragButton = button;
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (dragButton != button) return false;
        onDrag(x, y, dx, dy);
        return true;
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (dragButton != button) return false;
        onRelease(x, y);
        return true;
    }

    @Override
    protected boolean isValidClickButton(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
                || button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        long now = net.minecraft.Util.getMillis();
        boolean doubleClick = now - lastClickTime < 250;
        lastClickTime = now;
        if (doubleClick && dragButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            recenter();
        }
    }

    @Override
    public void onRelease(double mouseX, double mouseY) {
        if (dragButton >= 0) {
            dragButton = -1;
            persistCameraState();
        }
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (dragButton == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            yawOffset += (float) deltaX * 0.7F;
            float pitch =
                    Math.max(
                            -90.0F,
                            Math.min(
                                    90.0F,
                                    defaultPitch(facing) + pitchOffset + (float) deltaY * 0.7F));
            pitchOffset = pitch - defaultPitch(facing);
        } else {
            panX += (float) deltaX;
            panY += (float) deltaY;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        zoom =
                new GlassPreviewCameraState(
                                yawOffset,
                                pitchOffset,
                                zoom / (float) Math.pow(1.12, scrollY),
                                panX,
                                panY)
                        .normalized()
                        .zoom();
        persistCameraState();
        return true;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GlassGuiTheme.listBackground(graphics, getX(), getY(), getWidth(), getHeight());
        graphics.enableScissor(getX() + 1, getY() + 1, getRight() - 1, getBottom() - 1);
        Projection projection = projection();
        GlassVectorRenderState.Builder vectors = GlassVectorRenderState.builder();
        drawGrid(vectors, projection);
        List<GlobalPos> visible = nodes.subList(0, Math.min(visibleNodes, nodes.size()));
        drawContext(vectors, projection, visible);
        drawTriangles(vectors, projection, visible);
        drawNode(vectors, projection, origin, PROJECTOR);
        for (GlobalPos node : visible) {
            drawNode(vectors, projection, node, node.equals(selected) ? SELECTED : NODE);
        }
        vectors.submit(graphics, getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2);
        graphics.disableScissor();
        graphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("glass.gui.preview_progress", visible.size(), nodes.size()),
                getX() + 7,
                getBottom() - 13,
                GlassGuiTheme.TEXT_MUTED,
                false);
        if (isHovered()) {
            GlassCursor.request(org.lwjgl.glfw.GLFW.GLFW_CROSSHAIR_CURSOR);
        }
    }

    private Projection projection() {
        return new Projection(
                getX() + getWidth() / 2.0F + panX,
                getY() + getHeight() / 2.0F + panY,
                pixelsPerBlock(),
                (float) Math.toRadians(defaultYaw(facing) + yawOffset),
                (float) Math.toRadians(defaultPitch(facing) + pitchOffset));
    }

    float pixelsPerBlock() {
        return Math.max(1.0F, getWidth() - 2.0F * VIEWPORT_PADDING) / zoom;
    }

    void persistCameraState() {
        cameraStateSink.accept(cameraState());
    }

    GlassPreviewCameraState cameraState() {
        return new GlassPreviewCameraState(yawOffset, pitchOffset, zoom, panX, panY).normalized();
    }

    private void applyCameraState(GlassPreviewCameraState value) {
        GlassPreviewCameraState state = value.normalized();
        yawOffset = state.yawOffset();
        pitchOffset = state.pitchOffset();
        zoom = state.zoom();
        panX = state.panX();
        panY = state.panY();
    }

    static float defaultYaw(Direction direction) {
        return switch (direction) {
            case SOUTH, UP, DOWN -> 0.0F;
            case NORTH -> 180.0F;
            case EAST -> -90.0F;
            case WEST -> 90.0F;
        };
    }

    static float defaultPitch(Direction direction) {
        return switch (direction) {
            case UP -> -90.0F;
            case DOWN -> 90.0F;
            default -> 0.0F;
        };
    }

    double framingExtent() {
        double extent = 4.0;
        for (GlobalPos node : nodes) {
            BlockPos pos = node.pos();
            extent =
                    Math.max(
                            extent,
                            Math.max(
                                    Math.abs(pos.getX() - origin.pos().getX()),
                                    Math.max(
                                            Math.abs(pos.getY() - origin.pos().getY()),
                                            Math.abs(pos.getZ() - origin.pos().getZ()))));
        }
        return extent;
    }

    private ViewFrame topologyFrame() {
        float yaw = (float) Math.toRadians(defaultYaw(facing));
        float pitch = (float) Math.toRadians(defaultPitch(facing));
        ViewFrame.Builder frame = new ViewFrame.Builder();
        includeBlock(frame, origin.pos(), yaw, pitch);
        for (GlobalPos node : nodes) {
            if (node.dimension().equals(origin.dimension())) {
                includeBlock(frame, node.pos(), yaw, pitch);
            }
        }
        return frame.build();
    }

    private void includeBlock(ViewFrame.Builder frame, BlockPos pos, float yaw, float pitch) {
        float offsetX = pos.getX() - origin.pos().getX() - 0.5F;
        float offsetY = pos.getY() - origin.pos().getY() - 0.5F;
        float offsetZ = pos.getZ() - origin.pos().getZ() - 0.5F;
        for (int x = 0; x <= 1; x++) {
            for (int y = 0; y <= 1; y++) {
                for (int z = 0; z <= 1; z++) {
                    Vector3f point =
                            new Vector3f(offsetX + x, offsetY + y, offsetZ + z)
                                    .rotateY(yaw)
                                    .rotateX(pitch);
                    frame.include(point.x, point.y);
                }
            }
        }
    }

    private static float viewPadding(float extent) {
        return Math.max(MIN_VIEW_PADDING, extent * VIEW_PADDING_RATIO);
    }

    private void drawGrid(GlassVectorRenderState.Builder vectors, Projection projection) {
        int step = gridStepForScale(pixelsPerBlock());
        int radius = gridRadius(step);
        for (int coordinate = -radius; coordinate <= radius; coordinate += step) {
            drawWorldLine(
                    vectors,
                    projection,
                    new Vector3f(-radius, 0.0F, coordinate),
                    new Vector3f(radius, 0.0F, coordinate),
                    GRID);
            drawWorldLine(
                    vectors,
                    projection,
                    new Vector3f(coordinate, 0.0F, -radius),
                    new Vector3f(coordinate, 0.0F, radius),
                    GRID);
        }
    }

    static int gridStepForScale(float pixelsPerBlock) {
        float requiredBlocks = GRID_TARGET_PIXELS / Math.max(0.001F, pixelsPerBlock);
        if (requiredBlocks <= 1.0F) return 1;
        int magnitude = 1;
        while (requiredBlocks > 10.0F && magnitude <= MAX_GRID_RADIUS / 10) {
            requiredBlocks /= 10.0F;
            magnitude *= 10;
        }
        if (requiredBlocks <= 2.0F) return 2 * magnitude;
        if (requiredBlocks <= 5.0F) return 5 * magnitude;
        return 10 * magnitude;
    }

    private int gridRadius(int step) {
        float scale = pixelsPerBlock();
        float halfViewWidth = zoom * 0.5F;
        float halfViewHeight = getHeight() * 0.5F / scale;
        float panDistance = Math.max(Math.abs(panX), Math.abs(panY)) / scale;
        int required =
                Math.max(
                        8,
                        (int)
                                Math.ceil(
                                        Math.max(halfViewWidth, halfViewHeight) * 1.5F
                                                + panDistance
                                                + 2.0F));
        int maximum = Math.max(step, MAX_GRID_RADIUS / step * step);
        return Math.min(maximum, (required + step - 1) / step * step);
    }

    private void drawContext(
            GlassVectorRenderState.Builder vectors,
            Projection projection,
            List<GlobalPos> visible) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !client.level.dimension().equals(origin.dimension())) return;
        Set<BlockPos> network = new LinkedHashSet<>();
        network.add(origin.pos());
        visible.stream()
                .filter(pos -> pos.dimension().equals(origin.dimension()))
                .map(GlobalPos::pos)
                .forEach(network::add);
        LinkedHashSet<BlockPos> context = new LinkedHashSet<>();
        for (BlockPos center : network) {
            for (BlockPos candidate :
                    BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
                if (context.size() >= MAX_CONTEXT_BLOCKS) break;
                BlockPos immutable = candidate.immutable();
                if (network.contains(immutable)
                        || !client.level.hasChunk(immutable.getX() >> 4, immutable.getZ() >> 4)
                        || client.level.getBlockState(immutable).isAir()) continue;
                context.add(immutable);
            }
            if (context.size() >= MAX_CONTEXT_BLOCKS) break;
        }
        for (BlockPos pos : context) drawBlock(vectors, projection, pos, WORLD);
    }

    private void drawTriangles(
            GlassVectorRenderState.Builder vectors,
            Projection projection,
            List<GlobalPos> visible) {
        ArrayList<GlobalPos> points = new ArrayList<>(visible.size() + 1);
        points.add(origin);
        points.addAll(visible);
        for (int index = 2; index < points.size(); index++) {
            GlassVectorRenderState.Point a =
                    projectPosition(projection, points.get(index - 2).pos());
            GlassVectorRenderState.Point b =
                    projectPosition(projection, points.get(index - 1).pos());
            GlassVectorRenderState.Point c = projectPosition(projection, points.get(index).pos());
            vectors.triangle(a, b, c, TRIANGLE);
            vectors.line(a, b, LINE_WIDTH, GlassGuiTheme.ACCENT);
            vectors.line(b, c, LINE_WIDTH, GlassGuiTheme.ACCENT);
            vectors.line(c, a, LINE_WIDTH, GlassGuiTheme.ACCENT);
        }
    }

    private void drawNode(
            GlassVectorRenderState.Builder vectors,
            Projection projection,
            GlobalPos globalPos,
            int color) {
        if (!globalPos.dimension().equals(origin.dimension())) return;
        drawBlock(vectors, projection, globalPos.pos(), color);
    }

    private void drawBlock(
            GlassVectorRenderState.Builder vectors,
            Projection projection,
            BlockPos pos,
            int color) {
        Minecraft client = Minecraft.getInstance();
        List<AABB> boxes = List.of(new AABB(0, 0, 0, 1, 1, 1));
        if (client.level != null
                && client.level.dimension().equals(origin.dimension())
                && client.level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            BlockState state = client.level.getBlockState(pos);
            List<AABB> actual =
                    state.getShape(client.level, pos, CollisionContext.empty()).toAabbs();
            if (!actual.isEmpty()) boxes = actual;
        }
        float offsetX = pos.getX() - origin.pos().getX() - 0.5F;
        float offsetY = pos.getY() - origin.pos().getY() - 0.5F;
        float offsetZ = pos.getZ() - origin.pos().getZ() - 0.5F;
        for (AABB box : boxes) {
            drawBox(
                    vectors,
                    projection,
                    new AABB(
                            box.minX + offsetX,
                            box.minY + offsetY,
                            box.minZ + offsetZ,
                            box.maxX + offsetX,
                            box.maxY + offsetY,
                            box.maxZ + offsetZ),
                    color);
        }
    }

    private static void drawBox(
            GlassVectorRenderState.Builder vectors, Projection projection, AABB box, int color) {
        Vector3f[] corners = {
            new Vector3f((float) box.minX, (float) box.minY, (float) box.minZ),
            new Vector3f((float) box.maxX, (float) box.minY, (float) box.minZ),
            new Vector3f((float) box.maxX, (float) box.maxY, (float) box.minZ),
            new Vector3f((float) box.minX, (float) box.maxY, (float) box.minZ),
            new Vector3f((float) box.minX, (float) box.minY, (float) box.maxZ),
            new Vector3f((float) box.maxX, (float) box.minY, (float) box.maxZ),
            new Vector3f((float) box.maxX, (float) box.maxY, (float) box.maxZ),
            new Vector3f((float) box.minX, (float) box.maxY, (float) box.maxZ)
        };
        GlassVectorRenderState.Point[] projected = new GlassVectorRenderState.Point[corners.length];
        for (int index = 0; index < corners.length; index++) {
            projected[index] = projection.project(corners[index]);
        }
        int[][] edges = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6},
            {3, 7}
        };
        for (int[] edge : edges) {
            GlassVectorRenderState.Point a = projected[edge[0]];
            GlassVectorRenderState.Point b = projected[edge[1]];
            vectors.line(a, b, LINE_WIDTH, color);
        }
    }

    private GlassVectorRenderState.Point projectPosition(Projection projection, BlockPos pos) {
        return projection.project(
                new Vector3f(
                        pos.getX() - origin.pos().getX(),
                        pos.getY() - origin.pos().getY(),
                        pos.getZ() - origin.pos().getZ()));
    }

    private static void drawWorldLine(
            GlassVectorRenderState.Builder vectors,
            Projection projection,
            Vector3f from,
            Vector3f to,
            int color) {
        GlassVectorRenderState.Point a = projection.project(from);
        GlassVectorRenderState.Point b = projection.project(to);
        vectors.line(a, b, LINE_WIDTH, color);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    private record Projection(float centerX, float centerY, float scale, float yaw, float pitch) {
        GlassVectorRenderState.Point project(Vector3f input) {
            Vector3f point = new Vector3f(input).rotateY(yaw).rotateX(pitch).mul(scale);
            return new GlassVectorRenderState.Point(centerX + point.x, centerY - point.y);
        }
    }

    private record ViewFrame(float minX, float maxX, float minY, float maxY) {
        float width() {
            return Math.max(1.0F, maxX - minX);
        }

        float height() {
            return Math.max(1.0F, maxY - minY);
        }

        float centerX() {
            return (minX + maxX) * 0.5F;
        }

        float centerY() {
            return (minY + maxY) * 0.5F;
        }

        private static final class Builder {
            private float minX = Float.POSITIVE_INFINITY;
            private float maxX = Float.NEGATIVE_INFINITY;
            private float minY = Float.POSITIVE_INFINITY;
            private float maxY = Float.NEGATIVE_INFINITY;

            void include(float x, float y) {
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }

            ViewFrame build() {
                if (!Float.isFinite(minX)) return new ViewFrame(0.0F, 1.0F, 0.0F, 1.0F);
                return new ViewFrame(minX, maxX, minY, maxY);
            }
        }
    }
}
