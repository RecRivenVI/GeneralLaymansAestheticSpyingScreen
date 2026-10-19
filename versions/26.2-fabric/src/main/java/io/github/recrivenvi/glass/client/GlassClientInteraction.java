package io.github.recrivenvi.glass.client;

import io.github.recrivenvi.glass.client.gui.ProjectorChannelScreen;
import io.github.recrivenvi.glass.client.gui.TerminalChannelScreen;
import io.github.recrivenvi.glass.client.gui.WirelessProjectorScreen;
import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.network.GlassStatePayload;
import io.github.recrivenvi.glass.network.OpenGlassScreenPayload;
import io.github.recrivenvi.glass.state.GlassStateSnapshot;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.block.state.BlockState;

public final class GlassClientInteraction {
    private GlassClientInteraction() {}

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(
                GlassStatePayload.TYPE,
                (payload, context) ->
                        context.client()
                                .execute(
                                        () -> {
                                            GlassStateSnapshot snapshot =
                                                    GlassStateSnapshot.fromTag(payload.state());
                                            GlassClientState.replace(snapshot);
                                        }));
        ClientPlayNetworking.registerGlobalReceiver(
                OpenGlassScreenPayload.TYPE,
                (payload, context) ->
                        context.client().execute(() -> open(context.client(), payload.pos())));
    }

    private static void open(Minecraft client, GlobalPos pos) {
        if (client.level == null || !client.level.dimension().equals(pos.dimension())) return;
        BlockState state = client.level.getBlockState(pos.pos());
        if (state.is(GlassContent.TERMINAL)) {
            GlassClientState.terminal(pos)
                    .ifPresent(
                            terminal -> client.gui.setScreen(new TerminalChannelScreen(terminal)));
        } else if (state.is(GlassContent.PROJECTOR)) {
            GlassClientState.projector(pos)
                    .ifPresent(
                            projector ->
                                    client.gui.setScreen(new ProjectorChannelScreen(projector)));
        } else if (state.is(GlassContent.WIRELESS_EXTENSION)) {
            client.gui.setScreen(new WirelessProjectorScreen(pos));
        }
    }
}
