package io.github.recrivenvi.glass.mixin.client;

import io.github.recrivenvi.glass.GlassClient;
import io.github.recrivenvi.glass.client.render.GlassChannelBlockVisibility;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCopy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderSectionRegion.class)
public abstract class RenderSectionRegionMixin {
    @Unique private boolean glass$channelSnapshot;

    // NeoForge 的缓存直接调用带 ModelData 快照的六参数构造器；五参数入口仅作委托。
    @Inject(
            method =
                    "<init>(Lnet/minecraft/client/multiplayer/ClientLevel;III[Lnet/minecraft/client/renderer/chunk/SectionCopy;Lit/unimi/dsi/fastutil/longs/Long2ObjectFunction;)V",
            at = @At("RETURN"))
    private void glass$rememberChannelSnapshot(
            ClientLevel level,
            int minSectionX,
            int minSectionY,
            int minSectionZ,
            SectionCopy[] sections,
            it.unimi.dsi.fastutil.longs.Long2ObjectFunction<
                            net.neoforged.neoforge.model.data.ModelData>
                    modelDataSnapshot,
            CallbackInfo callback) {
        // 区块网格可能在后台线程稍后编译，必须把频道身份固化在快照本身，不能在
        // getBlockState 时读取一帧即逝的 extractingChannel 标志。
        glass$channelSnapshot = GlassClient.VIEW_RENDERER.isChannelExtraction();
    }

    @Inject(method = "getBlockState", at = @At("RETURN"), cancellable = true)
    private void glass$hideTerminalFromProjectedWorld(
            BlockPos pos, CallbackInfoReturnable<BlockState> callback) {
        callback.setReturnValue(
                GlassChannelBlockVisibility.stateForSnapshot(
                        callback.getReturnValue(), glass$channelSnapshot));
    }
}
