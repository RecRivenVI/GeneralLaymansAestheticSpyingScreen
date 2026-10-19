package io.github.recrivenvi.glass.network;

import io.github.recrivenvi.glass.config.GlassConfig;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.FriendlyByteBuf;

public record SetWirelessOrderPayload(GlobalPos projectorPos, List<GlobalPos> positions)
        implements GlassPacket {
    public SetWirelessOrderPayload(FriendlyByteBuf buffer) {
        this(buffer.readGlobalPos(), readPositions(buffer));
    }

    private static List<GlobalPos> readPositions(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > GlassConfig.MAX_WIRELESS_NODES) {
            throw new DecoderException("Invalid GLASS wireless position count: " + size);
        }
        ArrayList<GlobalPos> positions = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            positions.add(buffer.readGlobalPos());
        }
        return List.copyOf(positions);
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeGlobalPos(projectorPos);
        buffer.writeVarInt(positions.size());
        for (GlobalPos pos : positions) {
            buffer.writeGlobalPos(pos);
        }
    }
}
