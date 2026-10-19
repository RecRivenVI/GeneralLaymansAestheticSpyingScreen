package io.github.recrivenvi.glass.network;

import io.github.recrivenvi.glass.state.GlassServerState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;

public final class GlassNetworking {
    private GlassNetworking() {}

    public static void initialize(
            net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(OpenGlassScreenPayload.TYPE, OpenGlassScreenPayload.CODEC);
        registrar.playToClient(GlassStatePayload.TYPE, GlassStatePayload.CODEC);
        registrar.playToServer(
                SetTerminalChannelPayload.TYPE,
                SetTerminalChannelPayload.CODEC,
                (payload, context) ->
                        GlassServerState.setTerminal(
                                (ServerPlayer) context.player(),
                                payload.pos(),
                                payload.name(),
                                payload.publicChannel()));
        registrar.playToServer(
                SetProjectorChannelPayload.TYPE,
                SetProjectorChannelPayload.CODEC,
                (payload, context) ->
                        GlassServerState.setProjectorChannel(
                                (ServerPlayer) context.player(),
                                payload.projectorPos(),
                                payload.channelId()));
        registrar.playToServer(
                AttachWirelessPayload.TYPE,
                AttachWirelessPayload.CODEC,
                (payload, context) ->
                        GlassServerState.attachWireless(
                                (ServerPlayer) context.player(),
                                payload.wirelessPos(),
                                payload.projectorPos()));
        registrar.playToServer(
                SetWirelessOrderPayload.TYPE,
                SetWirelessOrderPayload.CODEC,
                (payload, context) ->
                        GlassServerState.setWirelessOrder(
                                (ServerPlayer) context.player(),
                                payload.projectorPos(),
                                payload.positions()));
    }

    public static void openScreen(ServerPlayer player, BlockPos pos) {
        GlobalPos globalPos = GlobalPos.of(player.level().dimension(), pos.immutable());
        if (!GlassServerState.canInteract(player, globalPos)) return;
        GlassServerState.sendState(player);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                player, new OpenGlassScreenPayload(globalPos));
    }
}
