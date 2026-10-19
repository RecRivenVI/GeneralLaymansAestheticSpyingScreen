package io.github.recrivenvi.glass.network;

import io.github.recrivenvi.glass.content.GlassContent;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetTerminalChannelPayload(GlobalPos pos, String name, boolean publicChannel)
        implements CustomPacketPayload {
    public static final Type<SetTerminalChannelPayload> TYPE =
            new Type<>(GlassContent.id("set_terminal_channel"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetTerminalChannelPayload> CODEC =
            CustomPacketPayload.codec(
                    SetTerminalChannelPayload::write, SetTerminalChannelPayload::new);

    private SetTerminalChannelPayload(RegistryFriendlyByteBuf buffer) {
        this(GlobalPos.STREAM_CODEC.decode(buffer), buffer.readUtf(512), buffer.readBoolean());
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        GlobalPos.STREAM_CODEC.encode(buffer, pos);
        buffer.writeUtf(name, 512);
        buffer.writeBoolean(publicChannel);
    }

    @Override
    public Type<SetTerminalChannelPayload> type() {
        return TYPE;
    }
}
