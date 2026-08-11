package me.ichun.mods.glass.client.gui;

import java.util.function.IntConsumer;
import me.ichun.mods.glass.config.GlassConfig;
import me.ichun.mods.glass.state.GlassServerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

/** 使用原版二级设置页布局编辑服务端权威配置。 */
public final class GlassConfigScreen extends OptionsSubScreen {
    private int propagationLimit;
    private int propagationDelayTicks;
    private int surfaceAnimationTicks;
    private int observationDistance;
    private int wirelessMinNodes;
    private int wirelessMaxNodes;
    private int channelNameLength;
    private int channelRenderDistance;
    private boolean legacyFadeAnimation;

    public GlassConfigScreen(Screen lastScreen) {
        this(lastScreen, Minecraft.getInstance().options, GlassConfig.get().values());
    }

    GlassConfigScreen(Screen lastScreen, Options options, GlassConfig.Values values) {
        super(lastScreen, options, Component.translatable("glass.config.title"));
        propagationLimit = values.propagationLimit();
        propagationDelayTicks = values.propagationDelayTicks();
        surfaceAnimationTicks = values.surfaceAnimationTicks();
        observationDistance = values.observationDistance();
        wirelessMinNodes = values.wirelessMinNodes();
        wirelessMaxNodes = values.wirelessMaxNodes();
        channelNameLength = values.channelNameLength();
        channelRenderDistance = values.channelRenderDistance();
        legacyFadeAnimation = values.legacyFadeAnimation();
    }

    @Override
    protected void addOptions() {
        list.addHeader(Component.translatable("glass.config.section.propagation"));
        list.addSmall(
                integerOption(
                        "propagation_limit",
                        GlassConfig.MIN_PROPAGATION_LIMIT,
                        GlassConfig.MAX_PROPAGATION_LIMIT,
                        propagationLimit,
                        value -> propagationLimit = value),
                integerOption(
                        "propagation_delay_ticks",
                        GlassConfig.MIN_PROPAGATION_DELAY_TICKS,
                        GlassConfig.MAX_PROPAGATION_DELAY_TICKS,
                        propagationDelayTicks,
                        value -> propagationDelayTicks = value));
        list.addBig(
                integerOption(
                        "surface_animation_ticks",
                        GlassConfig.MIN_SURFACE_ANIMATION_TICKS,
                        GlassConfig.MAX_SURFACE_ANIMATION_TICKS,
                        surfaceAnimationTicks,
                        value -> surfaceAnimationTicks = value));

        list.addHeader(Component.translatable("glass.config.section.observation"));
        list.addSmall(
                integerOption(
                        "observation_distance",
                        GlassConfig.MIN_OBSERVATION_DISTANCE,
                        GlassConfig.MAX_OBSERVATION_DISTANCE,
                        observationDistance,
                        value -> observationDistance = value),
                integerOption(
                        "channel_render_distance",
                        GlassConfig.MIN_CHANNEL_RENDER_DISTANCE,
                        GlassConfig.MAX_CHANNEL_RENDER_DISTANCE,
                        channelRenderDistance,
                        value -> channelRenderDistance = value));
        list.addBig(
                integerOption(
                        "channel_name_length",
                        GlassConfig.MIN_CHANNEL_NAME_LENGTH,
                        GlassConfig.MAX_CHANNEL_NAME_LENGTH,
                        channelNameLength,
                        value -> channelNameLength = value));

        list.addHeader(Component.translatable("glass.config.section.wireless"));
        list.addSmall(
                integerOption(
                        "wireless_min_nodes",
                        GlassConfig.MIN_WIRELESS_NODES,
                        GlassConfig.MAX_WIRELESS_NODES,
                        wirelessMinNodes,
                        value -> wirelessMinNodes = value),
                integerOption(
                        "wireless_max_nodes",
                        GlassConfig.MIN_WIRELESS_NODES,
                        GlassConfig.MAX_WIRELESS_NODES,
                        wirelessMaxNodes,
                        value -> wirelessMaxNodes = value));

        list.addHeader(Component.translatable("glass.config.section.visual"));
        list.addBig(
                OptionInstance.createBoolean(
                        "glass.config.legacy_fade_animation",
                        OptionInstance.cachedConstantTooltip(
                                Component.translatable(
                                        "glass.config.legacy_fade_animation.tooltip")),
                        legacyFadeAnimation,
                        value -> legacyFadeAnimation = value));
    }

    private static OptionInstance<Integer> integerOption(
            String name, int minimum, int maximum, int value, IntConsumer setter) {
        String key = "glass.config." + name;
        return new OptionInstance<>(
                key,
                OptionInstance.cachedConstantTooltip(Component.translatable(key + ".tooltip")),
                Options::genericValueLabel,
                new OptionInstance.IntRange(minimum, maximum),
                value,
                setter::accept);
    }

    @Override
    public void onClose() {
        if (list != null) {
            list.applyUnsavedChanges();
        }
        GlassConfig.Values values =
                new GlassConfig.Values(
                                propagationLimit,
                                propagationDelayTicks,
                                surfaceAnimationTicks,
                                observationDistance,
                                wirelessMinNodes,
                                wirelessMaxNodes,
                                channelNameLength,
                                channelRenderDistance,
                                legacyFadeAnimation)
                        .normalized();
        if (GlassConfig.get().save(values)) {
            IntegratedServer server = minecraft.getSingleplayerServer();
            if (server != null) {
                server.execute(() -> GlassServerState.rebuild(server));
            }
        }
        super.onClose();
    }
}
