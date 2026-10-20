package io.github.recrivenvi.glass.content.block;

import io.github.recrivenvi.glass.network.GlassNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class GlassWirelessExtensionBlock extends GlassStateBlock {
    public GlassWirelessExtensionBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            GlassNetworking.openScreen(serverPlayer, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
