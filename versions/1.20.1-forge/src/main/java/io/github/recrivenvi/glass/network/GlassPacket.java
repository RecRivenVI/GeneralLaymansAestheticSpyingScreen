package io.github.recrivenvi.glass.network;

import net.minecraft.network.FriendlyByteBuf;

/** A GLASS message carried by Forge's native SimpleChannel transport. */
public interface GlassPacket {
    void write(FriendlyByteBuf buffer);
}
