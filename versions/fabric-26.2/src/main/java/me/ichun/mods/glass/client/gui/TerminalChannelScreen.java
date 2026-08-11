package me.ichun.mods.glass.client.gui;

import me.ichun.mods.glass.network.SetTerminalChannelPayload;
import me.ichun.mods.glass.state.GlassStateSnapshot;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class TerminalChannelScreen extends Screen {
    private final GlobalPos terminalPos;
    private final String initialChannel;
    private boolean publicChannel;
    private EditBox channelName;
    private Button visibilityButton;

    public TerminalChannelScreen(GlassStateSnapshot.Terminal terminal) {
        super(Component.translatable("block.generallaymansaestheticspyingscreen.glass_terminal"));
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
                me.ichun.mods.glass.client.GlassClientState.snapshot()
                        .settings()
                        .channelNameLength());
        channelName.setValue(initialChannel);
        channelName.setHint(Component.translatable("glass.gui.channel_name_hint"));
        addRenderableWidget(channelName);

        visibilityButton =
                addRenderableWidget(
                        GlassButton.create(
                                visibilityText(),
                                button -> toggleVisibility(),
                                fieldLeft,
                                panel.contentTop() + 78,
                                fieldWidth,
                                20));
        addRenderableWidget(
                GlassButton.create(
                        Component.translatable("glass.gui.cancel"),
                        button -> onClose(),
                        panel.right() - 200,
                        panel.footerTop() + 12,
                        88,
                        20));
        addRenderableWidget(
                GlassButton.create(
                        Component.translatable("glass.gui.confirm"),
                        button -> confirm(),
                        panel.right() - 104,
                        panel.footerTop() + 12,
                        88,
                        20));
        setInitialFocus(channelName);
    }

    private void toggleVisibility() {
        publicChannel = !publicChannel;
        visibilityButton.setMessage(visibilityText());
    }

    private Component visibilityText() {
        return Component.translatable(
                publicChannel ? "glass.gui.visibility_public" : "glass.gui.visibility_private");
    }

    private void confirm() {
        ClientPlayNetworking.send(
                new SetTerminalChannelPayload(terminalPos, channelName.getValue(), publicChannel));
        onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (channelName.isFocused() && event.key() == GLFW.GLFW_KEY_TAB) {
            toggleVisibility();
            return true;
        }
        if (channelName.isFocused()
                && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)) {
            confirm();
            return true;
        }
        return super.keyPressed(event);
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
                Component.translatable("glass.gui.terminal_subtitle"));
        int left = panel.left() + GlassGuiTheme.PADDING;
        GlassGuiTheme.sectionTitle(
                graphics,
                font,
                Component.translatable("glass.gui.channel_name"),
                left,
                panel.contentTop() + 4);
        GlassGuiTheme.sectionTitle(
                graphics,
                font,
                Component.translatable("glass.gui.visibility"),
                left,
                panel.contentTop() + 60);
        graphics.text(
                font,
                Component.translatable("glass.gui.terminal_help"),
                left,
                panel.contentTop() + 111,
                GlassGuiTheme.TEXT_MUTED,
                false);
        graphics.text(
                font,
                Component.translatable("glass.gui.position")
                        .append(": ")
                        .append(GlassGuiTheme.coordinates(terminalPos.pos())),
                left,
                panel.footerTop() - 17,
                GlassGuiTheme.TEXT_MUTED,
                false);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private GlassGuiTheme.Layout panel() {
        return GlassGuiTheme.centered(width, height, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }
}
