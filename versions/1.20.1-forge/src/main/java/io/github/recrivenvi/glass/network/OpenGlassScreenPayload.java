package io.github.recrivenvi.glass.network;

import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;

public record OpenGlassScreenPayload(GlobalPos pos) implements GlassPacket {
    public OpenGlassScreenPayload(FriendlyByteBuf buffer) {
        this(buffer.readGlobalPos());
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeGlobalPos(pos);
    }
}
