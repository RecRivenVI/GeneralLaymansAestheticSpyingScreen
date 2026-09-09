package io.github.recrivenvi.glass.client.gui;

import io.github.recrivenvi.glass.client.GlassClientState;
import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.network.AttachWirelessPayload;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class WirelessProjectorScreen extends Screen {
    private final GlobalPos wirelessPos;
    private final List<Entry> projectors;
    private int selected = -1;
    private GlassNodeList<Entry> projectorList;
    private Button confirmButton;

    public WirelessProjectorScreen(GlobalPos wireless) {
        super(Component.translatable("block.glass.glass_wireless_extension"));
        wirelessPos = wireless;
        java.util.UUID playerId =
                Minecraft.getInstance().player == null
                        ? null
                        : Minecraft.getInstance().player.getUUID();
        projectors =
                GlassClientState.snapshot().projectors().stream()
                        .filter(
                                projector ->
                                        projector.pos().dimension().equals(wireless.dimension()))
                        .filter(
                                projector ->
                                        projector.channelId().isEmpty()
                                                || projector
                                                        .channelId()
                                                        .flatMap(GlassClientState::terminal)
                                                        .map(
                                                                terminal ->
                                                                        terminal.channel()
                                                                                        .publicChannel()
                                                                                || playerId != null
                                                                                        && terminal.channel()
                                                                                                .owner()
                                                                                                .filter(
                                                                                                        playerId
                                                                                                                ::equals)
                                                                                                .isPresent())
                                                        .orElse(false))
                        .map(
                                projector ->
                                        new Entry(
                                                projector.pos(),
                                                projector
                                                        .channelId()
                                                        .flatMap(GlassClientState::terminal)
                                                        .map(GlassStateSnapshot.Terminal::channel)
                                                        .orElse(null)))
                        .sorted(GlassNodeDistance.comparator(wirelessPos, Entry::pos))
                        .toList();
    }

    @Override
    protected void init() {
        GlassGuiTheme.Layout panel = panel();
        ListArea list = listArea();
        projectorList =
                addRenderableWidget(
                        new GlassNodeList<>(
                                list.x(),
                                list.y(),
                                list.width(),
                                list.height(),
                                projectors,
                                this::row,
                                index -> {
                                    selected = index;
                                    confirmButton.active = true;
                                },
                                (index, entry) -> applySelection(entry)));
        projectorList.setMessage(Component.translatable("glass.gui.no_projectors"));
        addRenderableWidget(
                GlassButton.create(
                        Component.translatable("glass.gui.cancel"),
                        button -> onClose(),
                        panel.right() - 200,
                        panel.footerTop() + 12,
                        88,
                        20));
        confirmButton =
                addRenderableWidget(
                        GlassButton.create(
                                Component.translatable("glass.gui.attach"),
                                button -> confirmSelection(),
                                panel.right() - 104,
                                panel.footerTop() + 12,
                                88,
                                20));
        confirmButton.active = false;
    }

    private GlassNodeList.Row row(Entry entry) {
        boolean warning = GlassNodeDistance.possiblyUnloaded(entry.pos());
        Component channel =
                entry.channel() == null
                        ? Component.translatable("glass.gui.not_set")
                        : Component.literal(entry.channel().name());
        return new GlassNodeList.Row(
                channel,
                GlassNodeDistance.descriptionWithCoordinates(wirelessPos, entry.pos()),
                warning
                        ? Component.translatable("glass.gui.possibly_unloaded")
                        : Component.translatable("glass.gui.projector"),
                warning);
    }

    private void confirmSelection() {
        if (selected >= 0 && selected < projectors.size()) applySelection(projectors.get(selected));
    }

    private void applySelection(Entry entry) {
        ClientPlayNetworking.send(new AttachWirelessPayload(wirelessPos, entry.pos()));
        onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN) {
            projectorList.moveSelection(event.key() == GLFW.GLFW_KEY_UP ? -1 : 1);
            return true;
        }
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER)
                && selected >= 0) {
            confirmSelection();
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
                Component.translatable("glass.gui.wireless_subtitle", projectors.size()));
        ListArea list = listArea();
        GlassGuiTheme.sectionTitle(
                graphics,
                font,
                Component.translatable("glass.gui.available_projectors"),
                list.x(),
                list.y() - 15);
        graphics.item(
                new ItemStack(GlassContent.WIRELESS_EXTENSION),
                panel.right() - 34,
                panel.top() + 13);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private GlassGuiTheme.Layout panel() {
        return GlassGuiTheme.centered(width, height, false);
    }

    private ListArea listArea() {
        GlassGuiTheme.Layout panel = panel();
        int x = panel.left() + GlassGuiTheme.PADDING;
        int y = panel.contentTop() + 16;
        int height = panel.footerTop() - y - 12;
        return new ListArea(x, y, panel.width() - GlassGuiTheme.PADDING * 2, height);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    private record Entry(GlobalPos pos, GlassStateSnapshot.Channel channel) {}

    private record ListArea(int x, int y, int width, int height) {}
}
