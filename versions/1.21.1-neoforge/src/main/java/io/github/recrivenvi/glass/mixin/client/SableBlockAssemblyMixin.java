package io.github.recrivenvi.glass.mixin.client;

import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import io.github.recrivenvi.glass.remote.GlassSableRelocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(
        targets = {
            "io.github.recrivenvi.glass.content.block.GlassChannelBlock",
            "io.github.recrivenvi.glass.content.block.GlassStateBlock"
        },
        remap = false)
public abstract class SableBlockAssemblyMixin implements BlockSubLevelAssemblyListener {
    @Override
    public void beforeMove(
            ServerLevel from, ServerLevel to, BlockState state, BlockPos oldPos, BlockPos newPos) {
        GlassSableRelocation.before(from, to, oldPos, newPos);
    }

    @Override
    public void afterMove(
            ServerLevel from, ServerLevel to, BlockState state, BlockPos oldPos, BlockPos newPos) {
        GlassSableRelocation.after(from, to, oldPos, newPos);
    }
}
