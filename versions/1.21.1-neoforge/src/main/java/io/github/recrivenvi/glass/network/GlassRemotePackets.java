package io.github.recrivenvi.glass.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GameProtocols;

/** Uses the registered vanilla play codec, including modded registry entries. */
public final class GlassRemotePackets {
    private GlassRemotePackets() {}

    public static byte[] encode(
            RegistryAccess registries, Packet<? super ClientGamePacketListener> packet) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            GameProtocols.CLIENTBOUND_TEMPLATE
                    .bind(RegistryFriendlyByteBuf.decorator(registries))
                    .codec()
                    .encode(buffer, packet);
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            return data;
        } finally {
            buffer.release();
        }
    }

    public static Packet<? super ClientGamePacketListener> apply(
            RegistryAccess registries, byte[] bytes, ClientGamePacketListener listener) {
        ByteBuf buffer = Unpooled.wrappedBuffer(bytes);
        try {
            var packet =
                    GameProtocols.CLIENTBOUND_TEMPLATE
                            .bind(RegistryFriendlyByteBuf.decorator(registries))
                            .codec()
                            .decode(buffer);
            packet.handle(listener);
            return packet;
        } finally {
            buffer.release();
        }
    }
}
