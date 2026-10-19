package io.github.recrivenvi.glass.network;

import io.github.recrivenvi.glass.content.GlassContent;
import io.github.recrivenvi.glass.state.GlassServerState;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class GlassNetworking {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL =
            NetworkRegistry.ChannelBuilder.named(GlassContent.id("main"))
                    .networkProtocolVersion(() -> PROTOCOL)
                    .clientAcceptedVersions(PROTOCOL::equals)
                    .serverAcceptedVersions(PROTOCOL::equals)
                    .simpleChannel();
    private static Consumer<GlassPacket> clientHandler;

    private GlassNetworking() {}

    public static void setClientHandler(Consumer<GlassPacket> handler) {
        clientHandler = handler;
    }

    public static void initialize() {
        client(0, OpenGlassScreenPayload.class, OpenGlassScreenPayload::new);
        client(1, GlassStatePayload.class, GlassStatePayload::new);
        server(
                2,
                SetTerminalChannelPayload.class,
                SetTerminalChannelPayload::new,
                (payload, player) ->
                        GlassServerState.setTerminal(
                                player, payload.pos(), payload.name(), payload.publicChannel()));
        server(
                3,
                SetProjectorChannelPayload.class,
                SetProjectorChannelPayload::new,
                (payload, player) ->
                        GlassServerState.setProjectorChannel(
                                player, payload.projectorPos(), payload.channelId()));
        server(
                4,
                AttachWirelessPayload.class,
                AttachWirelessPayload::new,
                (payload, player) ->
                        GlassServerState.attachWireless(
                                player, payload.wirelessPos(), payload.projectorPos()));
        server(
                5,
                SetWirelessOrderPayload.class,
                SetWirelessOrderPayload::new,
                (payload, player) ->
                        GlassServerState.setWirelessOrder(
                                player, payload.projectorPos(), payload.positions()));
    }

    private static <T extends GlassPacket> void client(
            int id, Class<T> type, Function<FriendlyByteBuf, T> decoder) {
        CHANNEL.messageBuilder(type, id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(GlassPacket::write)
                .decoder(decoder)
                .consumerMainThread(
                        (payload, context) -> {
                            if (clientHandler == null)
                                throw new IllegalStateException(
                                        "GLASS client handler is not initialized");
                            clientHandler.accept(payload);
                            context.get().setPacketHandled(true);
                        })
                .add();
    }

    private static <T extends GlassPacket> void server(
            int id,
            Class<T> type,
            Function<FriendlyByteBuf, T> decoder,
            BiConsumer<T, ServerPlayer> handler) {
        CHANNEL.messageBuilder(type, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(GlassPacket::write)
                .decoder(decoder)
                .consumerMainThread(
                        (payload, context) -> {
                            ServerPlayer player = context.get().getSender();
                            if (player == null)
                                throw new IllegalStateException(
                                        "GLASS server message has no player");
                            handler.accept(payload, player);
                            context.get().setPacketHandled(true);
                        })
                .add();
    }

    public static void sendToServer(GlassPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, GlassPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void openScreen(ServerPlayer player, BlockPos pos) {
        GlobalPos globalPos = GlobalPos.of(player.level().dimension(), pos.immutable());
        if (!GlassServerState.canInteract(player, globalPos)) return;
        GlassServerState.sendState(player);
        sendToPlayer(player, new OpenGlassScreenPayload(globalPos));
    }
}
