package io.github.recrivenvi.glass.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record GlassStatePayload(CompoundTag state) implements GlassPacket {
    public GlassStatePayload(FriendlyByteBuf buffer) {
        this(java.util.Objects.requireNonNull(buffer.readNbt(), "GLASS state"));
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
        buffer.writeNbt(state);
    }
}
