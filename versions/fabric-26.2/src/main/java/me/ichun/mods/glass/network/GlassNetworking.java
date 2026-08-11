package me.ichun.mods.glass.network;

import me.ichun.mods.glass.state.GlassServerState;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;

public final class GlassNetworking {
    private GlassNetworking() {}

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay()
                .register(OpenGlassScreenPayload.TYPE, OpenGlassScreenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay()
                .register(GlassStatePayload.TYPE, GlassStatePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay()
                .register(SetTerminalChannelPayload.TYPE, SetTerminalChannelPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay()
                .register(SetProjectorChannelPayload.TYPE, SetProjectorChannelPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay()
                .register(AttachWirelessPayload.TYPE, AttachWirelessPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay()
                .register(SetWirelessOrderPayload.TYPE, SetWirelessOrderPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(
                SetTerminalChannelPayload.TYPE,
                (payload, context) ->
                        GlassServerState.setTerminal(
                                context.player(),
                                payload.pos(),
                                payload.name(),
                                payload.publicChannel()));
        ServerPlayNetworking.registerGlobalReceiver(
                SetProjectorChannelPayload.TYPE,
                (payload, context) ->
                        GlassServerState.setProjectorChannel(
                                context.player(), payload.projectorPos(), payload.channelId()));
        ServerPlayNetworking.registerGlobalReceiver(
                AttachWirelessPayload.TYPE,
                (payload, context) ->
                        GlassServerState.attachWireless(
                                context.player(), payload.wirelessPos(), payload.projectorPos()));
        ServerPlayNetworking.registerGlobalReceiver(
                SetWirelessOrderPayload.TYPE,
                (payload, context) ->
                        GlassServerState.setWirelessOrder(
                                context.player(), payload.projectorPos(), payload.positions()));
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> GlassServerState.sync(handler.getPlayer()));
    }

    public static void openScreen(ServerPlayer player, BlockPos pos) {
        GlobalPos globalPos = GlobalPos.of(player.level().dimension(), pos.immutable());
        if (!GlassServerState.canInteract(player, globalPos)) return;
        GlassServerState.sendState(player);
        ServerPlayNetworking.send(player, new OpenGlassScreenPayload(globalPos));
    }
}
