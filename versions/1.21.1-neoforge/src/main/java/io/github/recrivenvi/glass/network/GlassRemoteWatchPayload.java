package io.github.recrivenvi.glass.network;

import io.github.recrivenvi.glass.content.GlassContent;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Requests an observed projector, never an arbitrary source position or dimension. */
public record GlassRemoteWatchPayload(Optional<GlobalPos> projector)
        implements CustomPacketPayload {
    public static final Type<GlassRemoteWatchPayload> TYPE =
            new Type<>(GlassContent.id("remote_watch"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GlassRemoteWatchPayload> CODEC =
            CustomPacketPayload.codec(GlassRemoteWatchPayload::write, GlassRemoteWatchPayload::new);

    private GlassRemoteWatchPayload(RegistryFriendlyByteBuf b) {
        this(b.readBoolean() ? Optional.of(GlobalPos.STREAM_CODEC.decode(b)) : Optional.empty());
    }

    private void write(RegistryFriendlyByteBuf b) {
        b.writeBoolean(projector.isPresent());
        projector.ifPresent(p -> GlobalPos.STREAM_CODEC.encode(b, p));
    }

    @Override
    public Type<GlassRemoteWatchPayload> type() {
        return TYPE;
    }
}
