package io.github.recrivenvi.glass.client.gui;

import io.github.recrivenvi.glass.network.GlassNetworking;
import io.github.recrivenvi.glass.network.SetTerminalChannelPayload;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class TerminalChannelScreen extends Screen {
    private final GlobalPos terminalPos;
    private final String initialChannel;
    private boolean publicChannel;
    private EditBox channelName;
    private CycleButton<Boolean> visibilityButton;

    public TerminalChannelScreen(GlassStateSnapshot.Terminal terminal) {
        super(Component.translatable("block.glass.glass_terminal"));
        terminalPos = terminal.pos();
        initialChannel = terminal.channel().name();
        publicChannel = terminal.channel().publicChannel();
    }

    @Override
    protected void init() {
        GlassGuiTheme.Layout panel = panel();
        int fieldLeft = panel.left() + GlassGuiTheme.PADDING;
        int fieldWidth = panel.width() - GlassGuiTheme.PADDING * 2;
        channelName =
                new EditBox(
                        font,
                        fieldLeft,
                        panel.contentTop() + 24,
                        fieldWidth,
                        22,
                        Component.translatable("glass.gui.channel_name"));
        channelName.setMaxLength(
                io.github.recrivenvi.glass.client.GlassClientState.snapshot()
                        .settings()
                        .channelNameLength());
        channelName.setValue(initialChannel);
        channelName.setHint(Component.translatable("glass.gui.channel_name_hint"));
        addRenderableWidget(channelName);

        visibilityButton =
                addRenderableWidget(
                        CycleButton.booleanBuilder(
                                        Component.translatable("glass.gui.visibility_public"),
                                        Component.translatable("glass.gui.visibility_private"))
                                .withInitialValue(publicChannel)
                                .displayOnlyValue()
                                .create(
                                        fieldLeft,
                                        panel.contentTop() + 78,
                                        fieldWidth,
                                        20,
                                        Component.translatable("glass.gui.visibility"),
                                        (button, value) -> publicChannel = value));
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
                                Component.translatable("glass.gui.confirm"),
                                button -> confirm(),
                                panel.right() - 104,
                                panel.footerTop() + 12,
                                88,
                                20));
        GlassGuiTheme.form(
                panel,
                addRenderableWidget(
                        GlassGuiTheme.label(
                                font,
                                Component.translatable("glass.gui.channel_name"),
                                fieldWidth)),
                channelName,
                addRenderableWidget(
                        GlassGuiTheme.label(
                                font, Component.translatable("glass.gui.visibility"), fieldWidth)),
                visibilityButton,
                addRenderableWidget(
                        GlassGuiTheme.label(
                                font,
                                Component.translatable("glass.gui.terminal_help"),
                                fieldWidth)),
                addRenderableWidget(
                        GlassGuiTheme.label(
                                font,
                                Component.translatable("glass.gui.position")
                                        .append(": ")
                                        .append(GlassGuiTheme.coordinates(terminalPos.pos())),
                                fieldWidth)));
        GlassGuiTheme.footer(panel, cancelButton, saveButton);
        setInitialFocus(channelName);
    }

    private void confirm() {
        GlassNetworking.sendToServer(
                new SetTerminalChannelPayload(terminalPos, channelName.getValue(), publicChannel));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (channelName.isFocused()
                && (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)) {
            confirm();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        GlassGuiTheme.Layout panel = panel();
        GlassGuiTheme.panel(
                graphics,
                font,
                panel,
                title,
                Component.translatable("glass.gui.terminal_subtitle"));
        super.render(graphics, mouseX, mouseY, partialTick);
        GlassGuiTheme.button(graphics, font, visibilityButton);
    }

    private GlassGuiTheme.Layout panel() {
        return GlassGuiTheme.centered(width, height, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
