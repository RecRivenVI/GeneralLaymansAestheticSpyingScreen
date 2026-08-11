package me.ichun.mods.glass.network;

import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import me.ichun.mods.glass.config.GlassConfig;
import me.ichun.mods.glass.content.GlassContent;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetWirelessOrderPayload(GlobalPos projectorPos, List<GlobalPos> positions)
        implements CustomPacketPayload {
    public static final Type<SetWirelessOrderPayload> TYPE =
            new Type<>(GlassContent.id("set_wireless_order"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetWirelessOrderPayload> CODEC =
            CustomPacketPayload.codec(SetWirelessOrderPayload::write, SetWirelessOrderPayload::new);

    private SetWirelessOrderPayload(RegistryFriendlyByteBuf buffer) {
        this(GlobalPos.STREAM_CODEC.decode(buffer), readPositions(buffer));
    }

    private static List<GlobalPos> readPositions(RegistryFriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > GlassConfig.MAX_WIRELESS_NODES) {
            throw new DecoderException("Invalid GLASS wireless position count: " + size);
        }
        ArrayList<GlobalPos> positions = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            positions.add(GlobalPos.STREAM_CODEC.decode(buffer));
        }
        return List.copyOf(positions);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        GlobalPos.STREAM_CODEC.encode(buffer, projectorPos);
        buffer.writeVarInt(positions.size());
        for (GlobalPos pos : positions) {
            GlobalPos.STREAM_CODEC.encode(buffer, pos);
        }
    }

    @Override
    public Type<SetWirelessOrderPayload> type() {
        return TYPE;
    }
}
