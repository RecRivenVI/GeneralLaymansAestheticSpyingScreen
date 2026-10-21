package io.github.recrivenvi.glass.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;

/** The plugin scopes native and merged world reads after all dispatcher callbacks are present. */
@Mixin(value = ClientPacketListener.class, priority = 500)
public abstract class ClientPacketListenerWorldMixin {}
