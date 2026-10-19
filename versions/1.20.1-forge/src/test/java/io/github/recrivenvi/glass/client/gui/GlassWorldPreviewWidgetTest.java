package io.github.recrivenvi.glass.client.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.recrivenvi.glass.model.GlassPreviewCameraState;
import java.lang.reflect.Modifier;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class GlassWorldPreviewWidgetTest {
    static {
        io.github.recrivenvi.glass.ForgeTestBootstrap.initialize();
    }

    @BeforeAll
    static void bootstrapMinecraft() {
        io.github.recrivenvi.glass.ForgeTestBootstrap.initialize();
    }

    @Test
    void previewImplementationAndItsInternalControlsStayPackagePrivate()
            throws NoSuchMethodException {
        assertFalse(Modifier.isPublic(GlassWorldPreviewWidget.class.getModifiers()));
        assertFalse(
                Modifier.isPublic(
                        GlassWorldPreviewWidget.class
                                .getDeclaredConstructor(
                                        int.class,
                                        int.class,
                                        int.class,
                                        int.class,
                                        GlobalPos.class,
                                        Direction.class,
                                        List.class)
                                .getModifiers()));
        for (String method :
                new String[] {
                    "setNodes",
                    "setSelected",
                    "visibleNodeCount",
                    "previousNode",
                    "nextNode",
                    "recenter"
                }) {
            assertFalse(
                    Modifier.isPublic(
                            java.util.Arrays.stream(
                                            GlassWorldPreviewWidget.class.getDeclaredMethods())
                                    .filter(value -> value.getName().equals(method))
                                    .findFirst()
                                    .orElseThrow()
                                    .getModifiers()),
                    method);
        }
    }

    @Test
    void previousAndNextBlockClampAtTheBroadcastBounds() {
        GlobalPos origin = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        GlassWorldPreviewWidget preview =
                new GlassWorldPreviewWidget(
                        0,
                        0,
                        200,
                        160,
                        origin,
                        Direction.NORTH,
                        List.of(
                                GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 0, 0)),
                                GlobalPos.of(Level.OVERWORLD, new BlockPos(2, 0, 0))),
                        GlassPreviewCameraState.defaults(),
                        state -> {});

        assertEquals(2, preview.visibleNodeCount());
        preview.nextNode();
        assertEquals(2, preview.visibleNodeCount());
        preview.previousNode();
        assertEquals(1, preview.visibleNodeCount());
        preview.previousNode();
        assertEquals(0, preview.visibleNodeCount());
        preview.previousNode();
        assertEquals(0, preview.visibleNodeCount());
        preview.nextNode();
        assertEquals(1, preview.visibleNodeCount());
    }

    @Test
    void steppingTheBroadcastDoesNotChangeThePreviewFraming() {
        GlobalPos origin = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        GlassWorldPreviewWidget preview =
                new GlassWorldPreviewWidget(
                        0,
                        0,
                        200,
                        160,
                        origin,
                        Direction.NORTH,
                        List.of(
                                GlobalPos.of(Level.OVERWORLD, new BlockPos(4, 0, 0)),
                                GlobalPos.of(Level.OVERWORLD, new BlockPos(40, 0, 0))),
                        GlassPreviewCameraState.defaults(),
                        state -> {});

        double extent = preview.framingExtent();
        preview.previousNode();
        preview.previousNode();
        assertEquals(0, preview.visibleNodeCount());
        assertEquals(extent, preview.framingExtent());
    }

    @Test
    void changingThePropagationOrderKeepsTheCurrentBroadcastProgress() {
        GlobalPos origin = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        GlobalPos first = GlobalPos.of(Level.OVERWORLD, new BlockPos(1, 0, 0));
        GlobalPos second = GlobalPos.of(Level.OVERWORLD, new BlockPos(2, 0, 0));
        GlassWorldPreviewWidget preview =
                new GlassWorldPreviewWidget(
                        0,
                        0,
                        200,
                        160,
                        origin,
                        Direction.NORTH,
                        List.of(first, second),
                        GlassPreviewCameraState.defaults(),
                        state -> {});

        preview.previousNode();
        preview.setNodes(List.of(second, first));

        assertEquals(1, preview.visibleNodeCount());
    }

    @Test
    void recenterUsesTheProjectorFrontAsTheZeroOffsetView() {
        assertEquals(180.0F, GlassWorldPreviewWidget.defaultYaw(Direction.NORTH));
        assertEquals(0.0F, GlassWorldPreviewWidget.defaultYaw(Direction.SOUTH));
        assertEquals(-90.0F, GlassWorldPreviewWidget.defaultYaw(Direction.EAST));
        assertEquals(90.0F, GlassWorldPreviewWidget.defaultYaw(Direction.WEST));
        assertEquals(-90.0F, GlassWorldPreviewWidget.defaultPitch(Direction.UP));
        assertEquals(90.0F, GlassWorldPreviewWidget.defaultPitch(Direction.DOWN));
    }

    @Test
    void giantWirelessNetworksCannotReduceTheRequestedZoomScale() {
        GlobalPos origin = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        GlassWorldPreviewWidget compact =
                preview(origin, List.of(GlobalPos.of(Level.OVERWORLD, new BlockPos(4, 0, 0))));
        GlassWorldPreviewWidget giant =
                preview(origin, List.of(GlobalPos.of(Level.OVERWORLD, new BlockPos(16_000, 0, 0))));

        assertEquals(compact.pixelsPerBlock(), giant.pixelsPerBlock());
        assertEquals(
                (200.0F - 24.0F) / GlassPreviewCameraState.DEFAULT_VIEW_WIDTH,
                compact.pixelsPerBlock());
    }

    @Test
    void recenterFitsAndCentersTheCompleteTopologyWithPadding() {
        GlobalPos origin = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        GlassWorldPreviewWidget preview =
                new GlassWorldPreviewWidget(
                        0,
                        0,
                        400,
                        200,
                        origin,
                        Direction.SOUTH,
                        List.of(GlobalPos.of(Level.OVERWORLD, new BlockPos(9, 3, 0))),
                        new GlassPreviewCameraState(25.0F, 10.0F, 1.0F, 80.0F, -40.0F),
                        state -> {});

        preview.recenter();

        GlassPreviewCameraState state = preview.cameraState();
        assertEquals(0.0F, state.yawOffset());
        assertEquals(0.0F, state.pitchOffset());
        assertEquals(11.6F, state.zoom(), 0.001F);
        assertEquals(-145.862F, state.panX(), 0.001F);
        assertEquals(48.621F, state.panY(), 0.001F);
    }

    @Test
    void recenterUsesThePreviewAspectRatioToKeepTallTopologiesVisible() {
        GlobalPos origin = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        GlassWorldPreviewWidget preview =
                new GlassWorldPreviewWidget(
                        0,
                        0,
                        400,
                        200,
                        origin,
                        Direction.SOUTH,
                        List.of(GlobalPos.of(Level.OVERWORLD, new BlockPos(0, 9, 0))),
                        GlassPreviewCameraState.defaults(),
                        state -> {});

        preview.recenter();

        assertEquals(24.782F, preview.cameraState().zoom(), 0.001F);
    }

    @Test
    void projectorBlockCenterIsThePreviewCoordinateOrigin() {
        GlobalPos origin = GlobalPos.of(Level.OVERWORLD, BlockPos.ZERO);
        GlassWorldPreviewWidget preview = preview(origin, List.of());

        preview.recenter();

        assertEquals(0.0F, preview.cameraState().panX(), 0.001F);
        assertEquals(0.0F, preview.cameraState().panY(), 0.001F);
    }

    @Test
    void gridDensityTracksZoomUsingReadableOneTwoFiveSteps() {
        assertEquals(1, GlassWorldPreviewWidget.gridStepForScale(32.0F));
        assertEquals(1, GlassWorldPreviewWidget.gridStepForScale(16.0F));
        assertEquals(2, GlassWorldPreviewWidget.gridStepForScale(8.0F));
        assertEquals(5, GlassWorldPreviewWidget.gridStepForScale(4.0F));
        assertEquals(20, GlassWorldPreviewWidget.gridStepForScale(1.0F));
        assertEquals(2_000, GlassWorldPreviewWidget.gridStepForScale(0.01F));
    }

    private static GlassWorldPreviewWidget preview(GlobalPos origin, List<GlobalPos> nodes) {
        return new GlassWorldPreviewWidget(
                0,
                0,
                200,
                160,
                origin,
                Direction.SOUTH,
                nodes,
                GlassPreviewCameraState.defaults(),
                state -> {});
    }
}
