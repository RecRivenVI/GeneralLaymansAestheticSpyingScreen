package io.github.recrivenvi.glass.client.gui;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** 原版负责条目、选择、滚动与焦点，GLASS 只绘制行主题。 */
final class GlassNodeList<T> extends ObjectSelectionList<GlassNodeList<T>.NodeEntry> {
    private boolean replacingEntries;
    private Component emptyMessage = Component.empty();
    private final List<T> values;
    private final Function<T, Row> rowFactory;
    private final IntConsumer selectionChanged;
    private final BiConsumer<Integer, T> activated;

    GlassNodeList(
            int x,
            int y,
            int width,
            int height,
            List<T> values,
            Function<T, Row> rowFactory,
            IntConsumer selectionChanged,
            BiConsumer<Integer, T> activated) {
        super(Minecraft.getInstance(), width, height, y, GlassGuiTheme.ROW_HEIGHT);
        setX(x);
        centerListVertically = false;
        this.values = values;
        this.rowFactory = rowFactory;
        this.selectionChanged = selectionChanged;
        this.activated = activated;
        entriesChanged();
    }

    void setEmptyMessage(Component message) {
        emptyMessage = message;
    }

    void setSelectedIndex(int index) {
        super.setSelected(index >= 0 && index < children().size() ? children().get(index) : null);
        if (getSelected() != null) scrollToEntry(getSelected());
    }

    void entriesChanged() {
        int selected = children().indexOf(getSelected());
        replacingEntries = true;
        replaceEntries(values.stream().map(NodeEntry::new).toList());
        replacingEntries = false;
        setSelectedIndex(Math.min(selected, values.size() - 1));
    }

    @Override
    public void setSelected(NodeEntry entry) {
        super.setSelected(entry);
        if (!replacingEntries && selectionChanged != null) {
            selectionChanged.accept(entry == null ? -1 : children().indexOf(entry));
        }
    }

    @Override
    public int getRowWidth() {
        return Math.max(1, getWidth() - scrollbarWidth() - 4);
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor graphics) {
        GlassGuiTheme.listBackground(graphics, getX(), getY(), getWidth(), getHeight());
    }

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor graphics) {}

    @Override
    protected void extractSelection(GuiGraphicsExtractor graphics, NodeEntry entry, int color) {}

    @Override
    protected void extractScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (scrollable()) {
            GlassGuiTheme.scrollBar(
                    graphics,
                    scrollBarX(),
                    getY(),
                    scrollbarWidth(),
                    getHeight(),
                    scrollBarY(),
                    scrollerHeight(),
                    isOverScrollbar(mouseX, mouseY));
        }
    }

    @Override
    public void extractWidgetRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        if (children().isEmpty()) {
            graphics.centeredText(
                    Minecraft.getInstance().font,
                    emptyMessage,
                    getX() + getWidth() / 2,
                    getY() + getHeight() / 2 - 4,
                    GlassGuiTheme.TEXT_MUTED);
        }
    }

    final class NodeEntry extends ObjectSelectionList.Entry<NodeEntry> {
        private final T value;

        NodeEntry(T value) {
            this.value = value;
        }

        @Override
        public Component getNarration() {
            return rowFactory.apply(value).narration();
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            setSelected(this);
            if (doubleClick && activated != null) {
                activated.accept(children().indexOf(this), value);
            }
            return true;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            if (activated != null
                    && (event.key() == GLFW.GLFW_KEY_ENTER
                            || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
                activated.accept(children().indexOf(this), value);
                return true;
            }
            return super.keyPressed(event);
        }

        @Override
        public void extractContent(
                GuiGraphicsExtractor graphics,
                int mouseX,
                int mouseY,
                boolean hovered,
                float partialTick) {
            Row row = rowFactory.apply(value);
            GlassGuiTheme.row(
                    graphics,
                    Minecraft.getInstance().font,
                    getX(),
                    getY(),
                    getWidth(),
                    children().indexOf(this),
                    getSelected() == this,
                    hovered,
                    row.primary(),
                    row.secondary(),
                    row.badge(),
                    row.warning());
        }
    }

    record Row(Component primary, Component secondary, Component badge, boolean warning) {
        Component narration() {
            var result = primary.copy().append(". ").append(secondary);
            if (badge != null) result.append(". ").append(badge);
            return result;
        }
    }
}
