package io.github.recrivenvi.glass.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** GLASS 面板使用的统一按钮。交互、键盘与鼠标语义仍由原版 Button 提供。 */
final class GlassButton extends Button {
    private GlassButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    static GlassButton create(
            Component message, OnPress onPress, int x, int y, int width, int height) {
        return new GlassButton(x, y, width, height, message, onPress);
    }

    @Override
    protected void extractContents(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        GlassGuiTheme.button(graphics, Minecraft.getInstance().font, this);
    }
}
