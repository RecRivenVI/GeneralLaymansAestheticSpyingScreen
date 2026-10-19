package io.github.recrivenvi.glass.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.chat.Component;

final class GlassGuiTheme {
    static final int PANEL_WIDTH = 430;
    static final int PANEL_HEIGHT = 272;
    static final int LARGE_PANEL_WIDTH = 560;
    static final int LARGE_PANEL_HEIGHT = 300;
    static final int LARGE_PANEL_MAX_WIDTH = 840;
    static final int LARGE_PANEL_MAX_HEIGHT = 520;
    static final int PADDING = 16;
    static final int HEADER_HEIGHT = 44;
    static final int FOOTER_HEIGHT = 44;
    static final int ROW_HEIGHT = 32;

    static final int TEXT = 0xfff2fbff;
    static final int TEXT_MUTED = 0xffa8bcc4;
    static final int ACCENT = 0xff56d8e8;
    static final int ACCENT_DARK = 0xff16879a;
    static final int SUCCESS = 0xff72d89b;
    static final int DANGER = 0xffff8f8f;

    private static final int PANEL = 0xf21a2228;
    private static final int PANEL_EDGE = 0xff4a6670;
    private static final int PANEL_INNER = 0xff26343b;
    private static final int HEADER = 0xff202e35;
    private static final int LIST = 0xc910171b;
    private static final int ROW = 0x99233339;
    private static final int ROW_ALT = 0x992a3b42;
    private static final int ROW_HOVERED = 0xb53a5058;
    private static final int ROW_SELECTED = 0xd02b5c66;
    private static final int DIVIDER = 0xff3b5058;

    private GlassGuiTheme() {}

    static StringWidget label(Font font, Component text, int width) {
        return new StringWidget(
                width, 9, text.copy().withStyle(style -> style.withColor(TEXT_MUTED)), font);
    }

    static void form(Layout panel, AbstractWidget... controls) {
        GridLayout grid = new GridLayout().rowSpacing(6);
        for (int row = 0; row < controls.length; row++) grid.addChild(controls[row], row, 0);
        grid.arrangeElements();
        grid.setX(panel.left() + PADDING);
        grid.setY(panel.contentTop() + 4);
    }

    static void footer(Layout panel, AbstractWidget... buttons) {
        LinearLayout row = LinearLayout.horizontal().spacing(8);
        for (AbstractWidget button : buttons) row.addChild(button);
        row.arrangeElements();
        row.setX(panel.right() - PADDING - row.getWidth());
        row.setY(panel.footerTop() + 12);
    }

    static void button(GuiGraphicsExtractor graphics, Font font, AbstractWidget button) {
        int background =
                !button.active ? 0xff263238 : button.isHoveredOrFocused() ? 0xff326a74 : 0xff263f47;
        int edge = !button.active ? 0xff46535a : button.isHoveredOrFocused() ? ACCENT : ACCENT_DARK;
        graphics.fill(
                button.getX(), button.getY(), button.getRight(), button.getBottom(), background);
        graphics.outline(button.getX(), button.getY(), button.getWidth(), button.getHeight(), edge);
        graphics.centeredText(
                font,
                button.getMessage(),
                button.getX() + button.getWidth() / 2,
                button.getY() + (button.getHeight() - 8) / 2,
                button.active ? TEXT : TEXT_MUTED);
    }

    static Layout centered(int screenWidth, int screenHeight, boolean large) {
        int desiredWidth =
                large
                        ? Math.min(
                                LARGE_PANEL_MAX_WIDTH,
                                Math.max(LARGE_PANEL_WIDTH, Math.round(screenWidth * 0.82F)))
                        : PANEL_WIDTH;
        int desiredHeight =
                large
                        ? Math.min(
                                LARGE_PANEL_MAX_HEIGHT,
                                Math.max(LARGE_PANEL_HEIGHT, Math.round(screenHeight * 0.82F)))
                        : PANEL_HEIGHT;
        int panelWidth = Math.min(desiredWidth, screenWidth - 24);
        int panelHeight = Math.min(desiredHeight, screenHeight - 24);
        return new Layout(
                (screenWidth - panelWidth) / 2,
                (screenHeight - panelHeight) / 2,
                panelWidth,
                panelHeight);
    }

    static void panel(
            GuiGraphicsExtractor graphics,
            Font font,
            Layout layout,
            Component title,
            Component subtitle) {
        shadow(graphics, layout.left(), layout.top(), layout.width(), layout.height());
        graphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), PANEL);
        graphics.outline(layout.left(), layout.top(), layout.width(), layout.height(), PANEL_EDGE);
        graphics.outline(
                layout.left() + 1,
                layout.top() + 1,
                layout.width() - 2,
                layout.height() - 2,
                PANEL_INNER);
        graphics.fill(
                layout.left() + 2,
                layout.top() + 2,
                layout.right() - 2,
                layout.top() + HEADER_HEIGHT,
                HEADER);
        graphics.fill(
                layout.left() + 2,
                layout.top() + HEADER_HEIGHT - 1,
                layout.right() - 2,
                layout.top() + HEADER_HEIGHT,
                ACCENT_DARK);
        graphics.text(font, title, layout.left() + PADDING, layout.top() + 10, TEXT, true);
        graphics.text(
                font, subtitle, layout.left() + PADDING, layout.top() + 25, TEXT_MUTED, false);
    }

    static void sectionTitle(
            GuiGraphicsExtractor graphics, Font font, Component title, int x, int y) {
        graphics.text(font, title, x, y, TEXT_MUTED, false);
    }

    static void listBackground(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, LIST);
        graphics.outline(x, y, width, height, DIVIDER);
    }

    static void row(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            int width,
            int index,
            boolean selected,
            boolean hovered,
            Component primary,
            Component secondary,
            Component badge,
            boolean warning) {
        int color =
                selected ? ROW_SELECTED : hovered ? ROW_HOVERED : index % 2 == 0 ? ROW : ROW_ALT;
        graphics.fill(x, y, x + width, y + ROW_HEIGHT - 1, color);
        if (selected) {
            graphics.fill(x, y, x + 3, y + ROW_HEIGHT - 1, ACCENT);
        }
        int textLeft = x + 10;
        if (warning) {
            graphics.fill(x + 3, y, x + 6, y + ROW_HEIGHT - 1, DANGER);
            textLeft += 4;
        }
        graphics.text(font, trim(font, primary, width - 118), textLeft, y + 6, TEXT, false);
        graphics.text(
                font, trim(font, secondary, width - 118), textLeft, y + 18, TEXT_MUTED, false);
        if (badge != null) {
            String value = trim(font, badge, 74).getString();
            int badgeWidth = Math.min(80, font.width(value) + 12);
            int badgeLeft = x + width - badgeWidth - 8;
            graphics.fill(
                    badgeLeft,
                    y + 7,
                    badgeLeft + badgeWidth,
                    y + 23,
                    warning ? 0xff733b42 : selected ? 0xff1c7f8c : 0xff31474f);
            graphics.centeredText(
                    font,
                    value,
                    badgeLeft + badgeWidth / 2,
                    y + 11,
                    selected ? 0xffffffff : TEXT_MUTED);
        }
    }

    static void scrollBar(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int thumbY,
            int thumbHeight,
            boolean hovered) {
        graphics.fill(x, y, x + width, y + height, 0xff111a1f);
        graphics.outline(x, y, width, height, DIVIDER);
        graphics.fill(
                x + 1, thumbY, x + width - 1, thumbY + thumbHeight, hovered ? ACCENT : ACCENT_DARK);
    }

    static int clickedRow(double mouseX, double mouseY, int x, int y, int width, int visibleRows) {
        if (mouseX < x
                || mouseX >= x + width
                || mouseY < y
                || mouseY >= y + visibleRows * ROW_HEIGHT) return -1;
        return (int) ((mouseY - y) / ROW_HEIGHT);
    }

    static int scroll(int first, double amount, int size, int visibleRows) {
        int maximum = Math.max(0, size - visibleRows);
        return Math.max(0, Math.min(maximum, first - (int) Math.signum(amount)));
    }

    static boolean hovered(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    static Component coordinates(net.minecraft.core.BlockPos pos) {
        return Component.literal(pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
    }

    static Component trim(Font font, Component text, int width) {
        return Component.literal(font.plainSubstrByWidth(text.getString(), Math.max(0, width)));
    }

    private static void shadow(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x + 5, y + 5, x + width + 5, y + height + 5, 0x90000000);
    }

    record Layout(int left, int top, int width, int height) {
        int right() {
            return left + width;
        }

        int bottom() {
            return top + height;
        }

        int contentTop() {
            return top + HEADER_HEIGHT + 14;
        }

        int footerTop() {
            return bottom() - FOOTER_HEIGHT;
        }
    }
}
