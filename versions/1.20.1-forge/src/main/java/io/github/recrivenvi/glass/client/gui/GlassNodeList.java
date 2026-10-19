package io.github.recrivenvi.glass.client.gui;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** 原版负责条目、选择、滚动与焦点，GLASS 只绘制行主题。 */
final class GlassNodeList<T> extends ObjectSelectionList<GlassNodeList<T>.NodeEntry> {
    private Component caption = Component.empty();
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
        super(Minecraft.getInstance(), width, height, y, y + height, GlassGuiTheme.ROW_HEIGHT);
        setLeftPos(x);
        setRenderBackground(false);
        setRenderTopAndBottom(false);
        centerListVertically = false;
        this.values = values;
        this.rowFactory = rowFactory;
        this.selectionChanged = selectionChanged;
        this.activated = activated;
        entriesChanged();
    }

    public void setMessage(Component message) {
        caption = message;
    }

    int getX() {
        return x0;
    }

    int getY() {
        return y0;
    }

    @Override
    public void updateNarration(net.minecraft.client.gui.narration.NarrationElementOutput output) {
        output.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, caption);
        super.updateNarration(output);
    }

    void setEmptyMessage(Component message) {
        emptyMessage = message;
    }

    void setSelectedIndex(int index) {
        super.setSelected(index >= 0 && index < children().size() ? children().get(index) : null);
        if (getSelected() != null) ensureVisible(getSelected());
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
        return Math.max(1, getWidth() - 10);
    }

    @Override
    protected int getScrollbarPosition() {
        return getRight() - 6;
    }

    @Override
    protected void renderSelection(
            GuiGraphics graphics, int y, int width, int height, int outer, int inner) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GlassGuiTheme.listBackground(graphics, getX(), getY(), getWidth(), getHeight());
        super.render(graphics, mouseX, mouseY, partialTick);
        if (getMaxScroll() > 0) {
            int thumbHeight =
                    net.minecraft.util.Mth.clamp(
                            getHeight() * getHeight() / getMaxPosition(), 32, getHeight() - 8);
            int thumbY =
                    getY()
                            + (int)
                                    (getScrollAmount()
                                            * (getHeight() - thumbHeight)
                                            / getMaxScroll());
            GlassGuiTheme.scrollBar(
                    graphics,
                    getScrollbarPosition(),
                    getY(),
                    6,
                    getHeight(),
                    thumbY,
                    thumbHeight,
                    mouseX >= getScrollbarPosition()
                            && mouseX < getRight()
                            && mouseY >= getY()
                            && mouseY < getBottom());
        }
        if (children().isEmpty()) {
            graphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    emptyMessage,
                    getX() + getWidth() / 2,
                    getY() + getHeight() / 2 - 4,
                    GlassGuiTheme.TEXT_MUTED);
        }
    }

    final class NodeEntry extends ObjectSelectionList.Entry<NodeEntry> {
        private final T value;
        private long lastClick;

        NodeEntry(T value) {
            this.value = value;
        }

        @Override
        public Component getNarration() {
            return rowFactory.apply(value).narration();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
            boolean selected = getSelected() == this;
            setSelected(this);
            long now = Util.getMillis();
            if (selected && now - lastClick < 250 && activated != null) {
                activated.accept(children().indexOf(this), value);
            }
            lastClick = now;
            return true;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (activated != null
                    && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
                activated.accept(children().indexOf(this), value);
                return true;
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        @Override
        public void render(
                GuiGraphics graphics,
                int index,
                int top,
                int left,
                int width,
                int height,
                int mouseX,
                int mouseY,
                boolean hovered,
                float partialTick) {
            Row row = rowFactory.apply(value);
            GlassGuiTheme.row(
                    graphics,
                    Minecraft.getInstance().font,
                    left,
                    top,
                    width,
                    index,
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
