package io.github.recrivenvi.glass.network;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;

public record AttachWirelessPayload(GlobalPos wirelessPos, GlobalPos projectorPos)
        implements GlassPacket {
    public AttachWirelessPayload(FriendlyByteBuf buffer) {
        this(buffer.readGlobalPos(), buffer.readGlobalPos());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeGlobalPos(wirelessPos);
        buffer.writeGlobalPos(projectorPos);
    }
}
