package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.client.render.GlassCaptureWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.network.client.ClientSableInterpolationState", remap = false)
public abstract class SableInterpolationScopeMixin {
    @Redirect(
            method = "tick",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lnet/minecraft/client/Minecraft;level:Lnet/minecraft/client/multiplayer/ClientLevel;",
                            opcode = 180))
    private ClientLevel glass$sourceTickRate(Minecraft client) {
        return GlassCaptureWorld.resolveLevel(client.level);
    }
}
