package io.github.recrivenvi.glass.network;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;

public record SetTerminalChannelPayload(GlobalPos pos, String name, boolean publicChannel)
        implements GlassPacket {
    public SetTerminalChannelPayload(FriendlyByteBuf buffer) {
        this(buffer.readGlobalPos(), buffer.readUtf(512), buffer.readBoolean());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeGlobalPos(pos);
        buffer.writeUtf(name, 512);
        buffer.writeBoolean(publicChannel);
    }
}
