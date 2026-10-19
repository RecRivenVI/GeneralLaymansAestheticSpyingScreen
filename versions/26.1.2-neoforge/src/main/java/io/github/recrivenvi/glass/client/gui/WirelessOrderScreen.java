package io.github.recrivenvi.glass.client.gui;

import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.network.SetWirelessOrderPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

public final class WirelessOrderScreen extends Screen {
    private final GlobalPos projectorPos;

    @SuppressWarnings("unused")
    private final Direction projectorFacing;

    private final ArrayList<GlobalPos> positions;
    private final Screen parent;
    private int selected = -1;
    private GlassNodeList<GlobalPos> nodeList;
    private GlassWorldPreviewWidget preview;
    private Button upButton;
    private Button downButton;
    private Button removeButton;

    public WirelessOrderScreen(
            GlobalPos projectorPos,
            Direction projectorFacing,
            List<GlobalPos> positions,
            Screen parent) {
        super(Component.translatable("glass.gui.wireless_order"));
        this.projectorPos = projectorPos;
        this.projectorFacing = projectorFacing;
        this.positions = new ArrayList<>(positions);
        this.parent = parent;
    }

    @Override
    protected void init() {
        GlassGuiTheme.Layout panel = panel();
        ListArea list = listArea();
        nodeList =
                addRenderableWidget(
                        new GlassNodeList<>(
                                list.x(),
                                list.y(),
                                list.width(),
                                list.height(),
                                positions,
                                this::row,
                                index -> {
                                    selected = index;
                                    preview.setSelected(index >= 0 ? positions.get(index) : null);
                                    updateButtons();
                                },
                                null));
        nodeList.setMessage(Component.translatable("glass.gui.extension_order"));
        nodeList.setEmptyMessage(Component.translatable("glass.gui.no_extensions"));

        int actionsY = list.y() + list.height() + 8;
        upButton =
                addRenderableWidget(
                        GlassButton.create(
                                Component.translatable("glass.gui.move_up"),
                                button -> move(-1),
                                list.x(),
                                actionsY,
                                76,
                                20));
        downButton =
                addRenderableWidget(
                        GlassButton.create(
                                Component.translatable("glass.gui.move_down"),
                                button -> move(1),
                                list.x() + 82,
                                actionsY,
                                76,
                                20));
        removeButton =
                addRenderableWidget(
                        GlassButton.create(
                                Component.translatable("glass.gui.remove"),
                                button -> removeSelected(),
                                list.x() + 164,
                                actionsY,
                                76,
                                20));
        removeButton.setTooltip(Tooltip.create(Component.translatable("glass.gui.remove_tooltip")));

        PreviewArea previewArea = previewArea();
        preview =
                addRenderableWidget(
                        new GlassWorldPreviewWidget(
                                previewArea.x(),
                                previewArea.y(),
                                previewArea.width(),
                                previewArea.height(),
                                projectorPos,
                                projectorFacing,
                                positions));
        GlassButton previewHelp =
                addRenderableWidget(
                        GlassButton.create(
                                Component.literal("?"),
                                button -> {},
                                previewArea.x() + previewArea.width() - 14,
                                previewArea.y() - 18,
                                14,
                                14));
        previewHelp.setTooltip(Tooltip.create(Component.translatable("glass.gui.preview_hint")));
        int previewButtonsY = previewArea.y() + previewArea.height() + 6;
        int previewButtonWidth = (previewArea.width() - 12) / 3;
        addRenderableWidget(
                GlassButton.create(
                        Component.translatable("glass.gui.preview_previous"),
                        button -> preview.previousNode(),
                        previewArea.x(),
                        previewButtonsY,
                        previewButtonWidth,
                        20));
        addRenderableWidget(
                GlassButton.create(
                        Component.translatable("glass.gui.preview_next"),
                        button -> preview.nextNode(),
                        previewArea.x() + previewButtonWidth + 6,
                        previewButtonsY,
                        previewButtonWidth,
                        20));
        addRenderableWidget(
                GlassButton.create(
                        Component.translatable("glass.gui.preview_recenter"),
                        button -> preview.recenter(),
                        previewArea.x() + (previewButtonWidth + 6) * 2,
                        previewButtonsY,
                        previewButtonWidth,
                        20));

        Button backButton =
                addRenderableWidget(
                        GlassButton.create(
                                Component.translatable("glass.gui.back"),
                                button -> minecraft.setScreen(parent),
                                panel.left() + GlassGuiTheme.PADDING,
                                panel.footerTop() + 12,
                                88,
                                20));
        Button cancelButton =
                addRenderableWidget(
                        GlassButton.create(
                                Component.translatable("glass.gui.cancel"),
                                button -> onClose(),
                                panel.right() - 200,
                                panel.footerTop() + 12,
                                88,
                                20));
        Button saveButton =
                addRenderableWidget(
                        GlassButton.create(
                                Component.translatable("glass.gui.save"),
                                button -> confirm(),
                                panel.right() - 104,
                                panel.footerTop() + 12,
                                88,
                                20));
        updateButtons();
        GlassGuiTheme.footer(panel, backButton, cancelButton, saveButton);
        setInitialFocus(nodeList);
    }

    private GlassNodeList.Row row(GlobalPos pos) {
        int index = positions.indexOf(pos);
        boolean warning = GlassNodeDistance.possiblyUnloaded(pos);
        return new GlassNodeList.Row(
                Component.translatable("glass.gui.extension_number", index + 1),
                GlassNodeDistance.descriptionWithCoordinates(projectorPos, pos),
                warning
                        ? Component.translatable("glass.gui.possibly_unloaded")
                        : Component.literal("#" + (index + 1)),
                warning);
    }

    private void move(int direction) {
        if (selected < 0) return;
        int target = selected + direction;
        if (target < 0 || target >= positions.size()) return;
        GlobalPos value = positions.remove(selected);
        positions.add(target, value);
        selected = target;
        updateNodeWidgets();
    }

    private void removeSelected() {
        if (selected < 0 || selected >= positions.size()) return;
        positions.remove(selected);
        selected = Math.min(selected, positions.size() - 1);
        updateNodeWidgets();
    }

    private void updateNodeWidgets() {
        nodeList.entriesChanged();
        nodeList.setSelectedIndex(selected);
        preview.setNodes(positions);
        preview.setSelected(selected >= 0 ? positions.get(selected) : null);
        updateButtons();
    }

    private void updateButtons() {
        upButton.active = selected > 0;
        downButton.active = selected >= 0 && selected < positions.size() - 1;
        removeButton.active = selected >= 0;
    }

    private void confirm() {
        ClientPacketDistributor.sendToServer(
                new SetWirelessOrderPayload(projectorPos, List.copyOf(positions)));
        onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (nodeList.isFocused() && event.key() == GLFW.GLFW_KEY_DELETE && selected >= 0) {
            removeSelected();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && preview != null
                && preview.hasActiveDrag()
                && preview.mouseDragged(event, deltaX, deltaY)) return true;
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && preview != null
                && preview.hasActiveDrag()
                && preview.mouseReleased(event)) return true;
        return super.mouseReleased(event);
    }

    @Override
    public void removed() {
        if (preview != null) preview.persistCameraState();
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GlassGuiTheme.Layout panel = panel();
        GlassGuiTheme.panel(
                graphics,
                font,
                panel,
                title,
                Component.translatable("glass.gui.wireless_order_subtitle", positions.size()));
        ListArea list = listArea();
        GlassGuiTheme.sectionTitle(
                graphics,
                font,
                Component.translatable("glass.gui.extension_order"),
                list.x(),
                list.y() - 15);
        PreviewArea previewArea = previewArea();
        GlassGuiTheme.sectionTitle(
                graphics,
                font,
                Component.translatable("glass.gui.preview"),
                previewArea.x(),
                previewArea.y() - 15);
        graphics.item(
                new ItemStack(GlassContent.WIRELESS_EXTENSION.get()),
                panel.right() - 34,
                panel.top() + 13);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private GlassGuiTheme.Layout panel() {
        return GlassGuiTheme.centered(width, height, true);
    }

    private ListArea listArea() {
        GlassGuiTheme.Layout panel = panel();
        int x = panel.left() + GlassGuiTheme.PADDING;
        int y = panel.contentTop() + 16;
        int width = Math.max(220, Math.min(320, Math.round(panel.width() * 0.40F)));
        int height = panel.footerTop() - y - 40;
        return new ListArea(x, y, width, height);
    }

    private PreviewArea previewArea() {
        GlassGuiTheme.Layout panel = panel();
        ListArea list = listArea();
        int x = list.x() + list.width() + 16;
        int y = list.y();
        return new PreviewArea(
                x, y, panel.right() - GlassGuiTheme.PADDING - x, panel.footerTop() - y - 38);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private record ListArea(int x, int y, int width, int height) {}

    private record PreviewArea(int x, int y, int width, int height) {}
}
