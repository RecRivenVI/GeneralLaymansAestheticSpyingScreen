package io.github.recrivenvi.glass.network;

import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;

public record SetProjectorChannelPayload(GlobalPos projectorPos, UUID channelId)
        implements GlassPacket {
    public SetProjectorChannelPayload(FriendlyByteBuf buffer) {
        this(buffer.readGlobalPos(), buffer.readUUID());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeGlobalPos(projectorPos);
        buffer.writeUUID(channelId);
    }
}
