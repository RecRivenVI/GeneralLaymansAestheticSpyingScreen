package me.ichun.mods.glass.client.gui;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractScrollArea;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** 使用原版滚动条拖拽与鼠标样式、使用 GLASS 行样式的通用节点列表。 */
final class GlassNodeList<T> extends AbstractScrollArea {
    private final List<T> entries;
    private final Function<T, Row> rowFactory;
    private final IntConsumer selectionChanged;
    private final BiConsumer<Integer, T> activated;
    private int selected = -1;

    GlassNodeList(
            int x,
            int y,
            int width,
            int height,
            List<T> entries,
            Function<T, Row> rowFactory,
            IntConsumer selectionChanged,
            BiConsumer<Integer, T> activated) {
        super(
                x,
                y,
                width,
                height,
                Component.empty(),
                AbstractScrollArea.defaultSettings(GlassGuiTheme.ROW_HEIGHT));
        this.entries = entries;
        this.rowFactory = rowFactory;
        this.selectionChanged = selectionChanged;
        this.activated = activated;
    }

    int selectedIndex() {
        return selected;
    }

    void setSelectedIndex(int index) {
        selected = index >= 0 && index < entries.size() ? index : -1;
        ensureSelectionVisible();
    }

    void entriesChanged() {
        if (selected >= entries.size()) selected = entries.size() - 1;
        refreshScrollAmount();
        ensureSelectionVisible();
    }

    void moveSelection(int direction) {
        if (entries.isEmpty()) return;
        int next =
                selected < 0
                        ? direction < 0 ? entries.size() - 1 : 0
                        : Math.max(0, Math.min(entries.size() - 1, selected + direction));
        select(next);
    }

    private void select(int index) {
        if (index < 0 || index >= entries.size()) return;
        selected = index;
        ensureSelectionVisible();
        selectionChanged.accept(index);
    }

    private void ensureSelectionVisible() {
        if (selected < 0) return;
        double rowTop = selected * GlassGuiTheme.ROW_HEIGHT;
        double rowBottom = rowTop + GlassGuiTheme.ROW_HEIGHT;
        if (rowTop < scrollAmount()) setScrollAmount(rowTop);
        if (rowBottom > scrollAmount() + getHeight()) {
            setScrollAmount(rowBottom - getHeight());
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (updateScrolling(event)) return;
        int contentRight = getRight() - scrollbarWidth();
        if (event.x() < getX()
                || event.x() >= contentRight
                || event.y() < getY()
                || event.y() >= getBottom()) return;
        int contentY = (int) Math.floor(event.y() - getY() + scrollAmount());
        int index = contentY / GlassGuiTheme.ROW_HEIGHT;
        if (index < 0 || index >= entries.size()) return;
        boolean alreadySelected = selected == index;
        select(index);
        if (doubleClick && alreadySelected && activated != null) {
            activated.accept(index, entries.get(index));
        }
    }

    @Override
    protected void extractWidgetRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GlassGuiTheme.listBackground(graphics, getX(), getY(), getWidth(), getHeight());
        int contentRight = getRight() - scrollbarWidth();
        graphics.enableScissor(getX() + 1, getY() + 1, contentRight, getBottom() - 1);
        int first = Math.max(0, (int) (scrollAmount() / GlassGuiTheme.ROW_HEIGHT));
        int y = getY() + first * GlassGuiTheme.ROW_HEIGHT - (int) scrollAmount();
        for (int index = first; index < entries.size() && y < getBottom(); index++) {
            Row row = rowFactory.apply(entries.get(index));
            GlassGuiTheme.row(
                    graphics,
                    Minecraft.getInstance().font,
                    getX() + 1,
                    y,
                    contentRight - getX() - 2,
                    index,
                    index == selected,
                    isVisibleRowHovered(
                            mouseX,
                            mouseY,
                            getX(),
                            getY(),
                            contentRight,
                            getBottom(),
                            y,
                            GlassGuiTheme.ROW_HEIGHT),
                    row.primary(),
                    row.secondary(),
                    row.badge(),
                    row.warning());
            y += GlassGuiTheme.ROW_HEIGHT;
        }
        graphics.disableScissor();
        extractScrollbar(graphics, mouseX, mouseY);
        if (isHovered() && !isOverScrollbar(mouseX, mouseY)) {
            graphics.requestCursor(CursorTypes.POINTING_HAND);
        }
        if (entries.isEmpty()) {
            graphics.centeredText(
                    Minecraft.getInstance().font,
                    getMessage(),
                    getX() + (getWidth() - scrollbarWidth()) / 2,
                    getY() + getHeight() / 2 - 4,
                    GlassGuiTheme.TEXT_MUTED);
        }
    }

    @Override
    protected void extractScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!scrollable()) return;
        boolean hovered = isOverScrollbar(mouseX, mouseY);
        GlassGuiTheme.scrollBar(
                graphics,
                scrollBarX(),
                getY(),
                scrollbarWidth(),
                getHeight(),
                scrollBarY(),
                scrollerHeight(),
                hovered);
        if (hovered) graphics.requestCursor(CursorTypes.RESIZE_NS);
    }

    @Override
    protected int contentHeight() {
        return Math.max(getHeight(), entries.size() * GlassGuiTheme.ROW_HEIGHT);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    static boolean isVisibleRowHovered(
            double mouseX,
            double mouseY,
            int listLeft,
            int listTop,
            int contentRight,
            int listBottom,
            int rowTop,
            int rowHeight) {
        return mouseX >= listLeft
                && mouseX < contentRight
                && mouseY >= listTop
                && mouseY < listBottom
                && mouseY >= rowTop
                && mouseY < rowTop + rowHeight;
    }

    record Row(Component primary, Component secondary, Component badge, boolean warning) {}
}
