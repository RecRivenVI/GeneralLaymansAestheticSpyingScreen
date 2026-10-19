package io.github.recrivenvi.glass.network;

import io.github.recrivenvi.glass.content.GlassContent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record GlassStatePayload(CompoundTag state) implements CustomPacketPayload {
    public static final Type<GlassStatePayload> TYPE = new Type<>(GlassContent.id("state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GlassStatePayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.TRUSTED_COMPOUND_TAG,
                    GlassStatePayload::state,
                    GlassStatePayload::new);

    @Override
    public Type<GlassStatePayload> type() {
        return TYPE;
    }
}
