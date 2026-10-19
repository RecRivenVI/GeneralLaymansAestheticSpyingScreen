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
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

public final class GlassClientInteraction {
    private GlassClientInteraction() {}

    public static void initialize(RegisterClientPayloadHandlersEvent event) {
        event.register(
                GlassStatePayload.TYPE,
                (payload, context) ->
                        Minecraft.getInstance()
                                .execute(
                                        () -> {
                                            GlassStateSnapshot snapshot =
                                                    GlassStateSnapshot.fromTag(payload.state());
                                            GlassClientState.replace(snapshot);
                                        }));
        event.register(
                OpenGlassScreenPayload.TYPE,
                (payload, context) ->
                        Minecraft.getInstance()
                                .execute(() -> open(Minecraft.getInstance(), payload.pos())));
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
