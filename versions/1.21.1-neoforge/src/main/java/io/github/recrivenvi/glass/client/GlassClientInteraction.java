package io.github.recrivenvi.glass.client;

import io.github.recrivenvi.glass.client.gui.ProjectorChannelScreen;
import io.github.recrivenvi.glass.client.gui.TerminalChannelScreen;
import io.github.recrivenvi.glass.client.gui.WirelessProjectorScreen;
import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.network.GlassStatePayload;
import io.github.recrivenvi.glass.network.OpenGlassScreenPayload;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.block.state.BlockState;

public final class GlassClientInteraction {
    private GlassClientInteraction() {}

    public static void initialize() {
        io.github.recrivenvi.glass.network.GlassNetworking.setClientHandler(
                payload -> {
                    if (payload instanceof GlassStatePayload state)
                        GlassClientState.replace(GlassStateSnapshot.fromTag(state.state()));
                    else if (payload instanceof OpenGlassScreenPayload screen)
                        open(Minecraft.getInstance(), screen.pos());
                    else if (payload
                            instanceof
                            io.github.recrivenvi.glass.network.GlassRemoteFramePayload frame)
                        io.github.recrivenvi.glass.client.render.GlassRemoteViews.receive(frame);
                });
    }

    private static void open(Minecraft client, GlobalPos pos) {
        if (client.level == null || !client.level.dimension().equals(pos.dimension())) return;
        BlockState state = client.level.getBlockState(pos.pos());
        if (state.is(GlassContent.TERMINAL.get())) {
            GlassClientState.terminal(pos)
                    .ifPresent(terminal -> client.setScreen(new TerminalChannelScreen(terminal)));
        } else if (state.is(GlassContent.PROJECTOR.get())) {
            GlassClientState.projector(pos)
                    .ifPresent(
                            projector -> client.setScreen(new ProjectorChannelScreen(projector)));
        } else if (state.is(GlassContent.WIRELESS_EXTENSION.get())) {
            client.setScreen(new WirelessProjectorScreen(pos));
        }
    }
}
