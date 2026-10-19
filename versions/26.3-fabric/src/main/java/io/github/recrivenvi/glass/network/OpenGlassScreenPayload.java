package io.github.recrivenvi.glass.network;

import io.github.recrivenvi.glass.content.GlassContent;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenGlassScreenPayload(GlobalPos pos) implements CustomPacketPayload {
    public static final Type<OpenGlassScreenPayload> TYPE =
            new Type<>(GlassContent.id("open_screen"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenGlassScreenPayload> CODEC =
            CustomPacketPayload.codec(OpenGlassScreenPayload::write, OpenGlassScreenPayload::new);

    private OpenGlassScreenPayload(RegistryFriendlyByteBuf buffer) {
        this(GlobalPos.STREAM_CODEC.decode(buffer));
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        GlobalPos.STREAM_CODEC.encode(buffer, pos);
    }

    @Override
    public Type<OpenGlassScreenPayload> type() {
        return TYPE;
    }
}
