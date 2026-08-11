package me.ichun.mods.glass.client.render;

import me.ichun.mods.glass.content.GlassContent;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 频道区块快照中的方块可见性规则；主世界快照不经过这层替换。 */
public final class GlassChannelBlockVisibility {
    private GlassChannelBlockVisibility() {}

    public static BlockState stateForSnapshot(BlockState state, boolean channelSnapshot) {
        return shouldHide(state.is(GlassContent.TERMINAL), channelSnapshot)
                ? Blocks.AIR.defaultBlockState()
                : state;
    }

    static boolean shouldHide(boolean terminal, boolean channelSnapshot) {
        return terminal && channelSnapshot;
    }
}
