package me.ichun.mods.glass.network;

import java.util.UUID;
import me.ichun.mods.glass.content.GlassContent;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetProjectorChannelPayload(GlobalPos projectorPos, UUID channelId)
        implements CustomPacketPayload {
    public static final Type<SetProjectorChannelPayload> TYPE =
            new Type<>(GlassContent.id("set_projector_channel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetProjectorChannelPayload> CODEC =
            CustomPacketPayload.codec(
                    SetProjectorChannelPayload::write, SetProjectorChannelPayload::new);

    private SetProjectorChannelPayload(RegistryFriendlyByteBuf buffer) {
        this(GlobalPos.STREAM_CODEC.decode(buffer), buffer.readUUID());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        GlobalPos.STREAM_CODEC.encode(buffer, projectorPos);
        buffer.writeUUID(channelId);
    }

    @Override
    public Type<SetProjectorChannelPayload> type() {
        return TYPE;
    }
}
