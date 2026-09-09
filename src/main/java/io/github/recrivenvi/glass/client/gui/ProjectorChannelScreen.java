package io.github.recrivenvi.glass.client.gui;

import io.github.recrivenvi.glass.client.GlassClientState;
import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.content.block.GlassChannelBlock;
import io.github.recrivenvi.glass.network.SetProjectorChannelPayload;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class ProjectorChannelScreen extends Screen {
    private final GlobalPos projectorPos;
    private final List<Entry> channels;
    private final List<GlobalPos> wirelessPositions;
    private final Direction projectorFacing;
    private int selected = -1;
    private GlassNodeList<Entry> channelList;
    private Button confirmButton;

    public ProjectorChannelScreen(GlassStateSnapshot.Projector projector) {
        super(Component.translatable("block.glass.glass_projector"));
        projectorPos = projector.pos();
        wirelessPositions = projector.wirelessPositions();
        Minecraft client = Minecraft.getInstance();
        projectorFacing =
                client.level == null || !client.level.dimension().equals(projectorPos.dimension())
                        ? Direction.NORTH
                        : client.level
                                .getBlockState(projectorPos.pos())
                                .getValue(GlassChannelBlock.FACING);
        java.util.UUID playerId = client.player == null ? null : client.player.getUUID();
        channels =
                GlassClientState.snapshot().terminals().stream()
                        .filter(
                                terminal ->
                                        terminal.channel().publicChannel()
                                                || playerId != null
                                                        && terminal.channel()
                                                                .owner()
                                                                .filter(playerId::equals)
                                                                .isPresent())
                        .filter(terminal -> !terminal.channel().name().isEmpty())
                        .map(terminal -> new Entry(terminal.pos(), terminal.channel()))
                        .sorted(GlassNodeDistance.comparator(projectorPos, Entry::pos))
                        .toList();
        for (int index = 0; index < channels.size(); index++) {
            if (projector
                    .channelId()
                    .filter(channels.get(index).channel().id()::equals)
                    .isPresent()) {
                selected = index;
                break;
            }
        }
    }

    @Override
    protected void init() {
        GlassGuiTheme.Layout panel = panel();
        ListArea list = listArea();
        channelList =
                addRenderableWidget(
                        new GlassNodeList<>(
                                list.x(),
                                list.y(),
                                list.width(),
                                list.height(),
                                channels,
                                this::row,
                                index -> {
                                    selected = index;
                                    confirmButton.active = true;
                                },
                                (index, entry) -> applySelection(entry)));
        channelList.setMessage(Component.translatable("glass.gui.no_channels"));
        channelList.setSelectedIndex(selected);

        GlassButton wireless =
                addRenderableWidget(
                        GlassButton.create(
                                Component.translatable("glass.gui.manage_wireless"),
                                button ->
                                        minecraft.gui.setScreen(
                                                new WirelessOrderScreen(
                                                        projectorPos,
                                                        projectorFacing,
                                                        wirelessPositions,
                                                        this)),
                                panel.left() + GlassGuiTheme.PADDING,
                                panel.footerTop() + 12,
                                128,
                                20));
        wireless.setTooltip(
                Tooltip.create(Component.translatable("glass.gui.manage_wireless_tooltip")));
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
                                Component.translatable("glass.gui.confirm"),
                                button -> confirmSelection(),
                                panel.right() - 104,
                                panel.footerTop() + 12,
                                88,
                                20));
        confirmButton.active = selected >= 0;
    }

    private GlassNodeList.Row row(Entry entry) {
        boolean warning = GlassNodeDistance.possiblyUnloaded(entry.pos());
        Component badge =
                warning
                        ? Component.translatable("glass.gui.possibly_unloaded")
                        : Component.translatable(
                                entry.channel().publicChannel()
                                        ? "glass.gui.public"
                                        : "glass.gui.private");
        return new GlassNodeList.Row(
                Component.literal(entry.channel().name()),
                GlassNodeDistance.descriptionWithCoordinates(projectorPos, entry.pos()),
                badge,
                warning);
    }

    private void confirmSelection() {
        if (selected >= 0 && selected < channels.size()) applySelection(channels.get(selected));
    }

    private void applySelection(Entry entry) {
        ClientPlayNetworking.send(
                new SetProjectorChannelPayload(projectorPos, entry.channel().id()));
        onClose();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_DOWN) {
            channelList.moveSelection(event.key() == GLFW.GLFW_KEY_UP ? -1 : 1);
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
                Component.translatable("glass.gui.projector_subtitle", channels.size()));
        ListArea list = listArea();
        GlassGuiTheme.sectionTitle(
                graphics,
                font,
                Component.translatable("glass.gui.available_channels"),
                list.x(),
                list.y() - 15);
        graphics.item(new ItemStack(GlassContent.PROJECTOR), panel.right() - 34, panel.top() + 13);
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
