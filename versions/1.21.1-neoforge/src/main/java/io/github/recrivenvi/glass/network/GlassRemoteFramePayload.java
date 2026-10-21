package io.github.recrivenvi.glass.network;

import io.github.recrivenvi.glass.content.GlassContent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** A server-authorized view generation and native clientbound play packets. */
public record GlassRemoteFramePayload(UUID generation, CompoundTag header, List<byte[]> packets)
        implements CustomPacketPayload {
    public static final Type<GlassRemoteFramePayload> TYPE =
            new Type<>(GlassContent.id("remote_frame"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GlassRemoteFramePayload> CODEC =
            CustomPacketPayload.codec(GlassRemoteFramePayload::write, GlassRemoteFramePayload::new);

    private GlassRemoteFramePayload(RegistryFriendlyByteBuf b) {
        this(b.readUUID(), ByteBufCodecs.TRUSTED_COMPOUND_TAG.decode(b), readPackets(b));
    }

    private static List<byte[]> readPackets(RegistryFriendlyByteBuf b) {
        int count = b.readVarInt();
        List<byte[]> packets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) packets.add(b.readByteArray());
        return List.copyOf(packets);
    }

    private void write(RegistryFriendlyByteBuf b) {
        b.writeUUID(generation);
        ByteBufCodecs.TRUSTED_COMPOUND_TAG.encode(b, header);
        b.writeVarInt(packets.size());
        for (byte[] packet : packets) b.writeByteArray(packet);
    }

    @Override
    public Type<GlassRemoteFramePayload> type() {
        return TYPE;
    }
}
