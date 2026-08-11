package me.ichun.mods.glass.client.render;

import net.minecraft.client.renderer.PostChain;
import org.jspecify.annotations.Nullable;

public final class GlassChannelFramePolicy {
    private static final ThreadLocal<PostChain> CHANNEL_POST_CHAIN = new ThreadLocal<>();

    private GlassChannelFramePolicy() {}

    public static boolean mayUseSharedPostChains(boolean channelExtraction) {
        return !channelExtraction;
    }

    public static void setCurrentChannelPostChain(@Nullable PostChain postChain) {
        if (postChain == null) {
            CHANNEL_POST_CHAIN.remove();
        } else {
            CHANNEL_POST_CHAIN.set(postChain);
        }
    }

    public static @Nullable PostChain currentChannelPostChain() {
        return CHANNEL_POST_CHAIN.get();
    }

    public static void clearCurrentChannelPostChain() {
        CHANNEL_POST_CHAIN.remove();
    }
}
