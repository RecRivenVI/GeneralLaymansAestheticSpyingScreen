package me.ichun.mods.glass.client.render;

public final class GlassChannelEntityVisibility {
    private GlassChannelEntityVisibility() {}

    public static boolean isDetachedForExtraction(
            boolean cameraDetached, boolean channelExtraction) {
        // 频道相机借用主摄像机实体，但投影画面仍必须把本地玩家作为世界实体提取。
        return cameraDetached || channelExtraction;
    }

    public static boolean isVisibleForExtraction(
            boolean vanillaVisible, boolean localPlayer, boolean channelExtraction) {
        // 原版 RenderGlobalProxy 始终保留本地玩家，不让投影来源区段的可见性提前丢弃它。
        return vanillaVisible || localPlayer && channelExtraction;
    }
}
