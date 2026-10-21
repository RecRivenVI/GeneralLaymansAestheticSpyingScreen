package io.github.recrivenvi.glass.probe.performance.mixin;

import io.github.recrivenvi.glass.probe.performance.PerformanceCounters;
import io.github.recrivenvi.glass.probe.performance.PerformanceWire;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(
        targets = "io.github.recrivenvi.glass.remote.GlassRemoteSubscriptions$Runtime",
        remap = false)
public abstract class PerformanceRemoteSendMixin {
    @Unique private static final ThreadLocal<UUID> GLASSPERF_PLAYER = new ThreadLocal<>();
    @Unique private static final ThreadLocal<Integer> GLASSPERF_SIZE = new ThreadLocal<>();

    @ModifyVariable(method = "send", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private net.minecraft.server.level.ServerPlayer glassperf$player(
            net.minecraft.server.level.ServerPlayer player) {
        GLASSPERF_PLAYER.set(player.getUUID());
        return player;
    }

    @ModifyVariable(method = "send", at = @At("STORE"), ordinal = 0)
    private int glassperf$size(int size) {
        GLASSPERF_SIZE.set(size);
        return size;
    }

    @Inject(method = "send", at = @At("RETURN"))
    private void glassperf$sent(CallbackInfoReturnable<Boolean> callback) {
        if (PerformanceCounters.measuring && callback.getReturnValue())
            PerformanceWire.PAYLOAD
                    .computeIfAbsent(
                            GLASSPERF_PLAYER.get(),
                            k -> new java.util.concurrent.atomic.LongAdder())
                    .add(GLASSPERF_SIZE.get());
        GLASSPERF_PLAYER.remove();
        GLASSPERF_SIZE.remove();
    }
}
