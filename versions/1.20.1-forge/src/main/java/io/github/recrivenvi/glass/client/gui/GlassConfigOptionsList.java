package io.github.recrivenvi.glass.client.gui;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

final class GlassConfigOptionsList
        extends ContainerObjectSelectionList<GlassConfigOptionsList.Row> {
    private final Minecraft client;

    GlassConfigOptionsList(Minecraft client, int width, int height, int top, int bottom) {
        super(client, width, height, top, bottom, 25);
        this.client = client;
    }

    void addHeader(Component title) {
        addEntry(new Row(List.of(new StringWidget(310, 20, title, client.font))));
    }

    void addBig(OptionInstance<?> option) {
        addEntry(new Row(List.of(option.createButton(client.options, 0, 0, 310))));
    }

    void addSmall(OptionInstance<?> first, OptionInstance<?> second) {
        addEntry(
                new Row(
                        List.of(
                                first.createButton(client.options, 0, 0, 150),
                                second.createButton(client.options, 0, 0, 150))));
    }

    @Override
    public int getRowWidth() {
        return 310;
    }

    @Override
    protected int getScrollbarPosition() {
        return width / 2 + 168;
    }

    static final class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final List<AbstractWidget> widgets;

        Row(List<AbstractWidget> widgets) {
            this.widgets = widgets;
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
                float delta) {
            for (int i = 0; i < widgets.size(); i++) {
                AbstractWidget widget = widgets.get(i);
                widget.setX(left + i * 160);
                widget.setY(top);
                widget.render(graphics, mouseX, mouseY, delta);
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return widgets;
        }
    }
}
