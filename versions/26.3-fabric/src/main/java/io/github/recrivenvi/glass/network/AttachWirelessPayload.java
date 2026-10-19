package io.github.recrivenvi.glass.network;

import io.github.recrivenvi.glass.content.GlassContent;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AttachWirelessPayload(GlobalPos wirelessPos, GlobalPos projectorPos)
        implements CustomPacketPayload {
    public static final Type<AttachWirelessPayload> TYPE =
            new Type<>(GlassContent.id("attach_wireless"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AttachWirelessPayload> CODEC =
            CustomPacketPayload.codec(AttachWirelessPayload::write, AttachWirelessPayload::new);

    private AttachWirelessPayload(RegistryFriendlyByteBuf buffer) {
        this(GlobalPos.STREAM_CODEC.decode(buffer), GlobalPos.STREAM_CODEC.decode(buffer));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        GlobalPos.STREAM_CODEC.encode(buffer, wirelessPos);
        GlobalPos.STREAM_CODEC.encode(buffer, projectorPos);
    }

    @Override
    public Type<AttachWirelessPayload> type() {
        return TYPE;
    }
}
