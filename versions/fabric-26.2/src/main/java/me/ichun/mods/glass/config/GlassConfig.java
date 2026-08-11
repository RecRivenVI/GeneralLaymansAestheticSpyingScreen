package me.ichun.mods.glass.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import me.ichun.mods.glass.GlassMod;
import net.fabricmc.loader.api.FabricLoader;

/** 服务端权威的玩法与同步参数。 */
public final class GlassConfig {
    public static final int DEFAULT_PROPAGATION_LIMIT = 64;
    public static final int DEFAULT_PROPAGATION_DELAY_TICKS = 2;
    public static final int DEFAULT_SURFACE_ANIMATION_TICKS = 8;
    public static final int DEFAULT_OBSERVATION_DISTANCE = 256;
    public static final int DEFAULT_WIRELESS_MIN_NODES = 2;
    public static final int DEFAULT_WIRELESS_MAX_NODES = 256;
    public static final int DEFAULT_CHANNEL_NAME_LENGTH = 64;
    public static final int DEFAULT_CHANNEL_RENDER_DISTANCE = 8;

    public static final int MIN_PROPAGATION_LIMIT = 1;
    public static final int MAX_PROPAGATION_LIMIT = 256;
    public static final int MIN_PROPAGATION_DELAY_TICKS = 1;
    public static final int MAX_PROPAGATION_DELAY_TICKS = 20;
    public static final int MIN_SURFACE_ANIMATION_TICKS = 1;
    public static final int MAX_SURFACE_ANIMATION_TICKS = 40;
    public static final int MIN_OBSERVATION_DISTANCE = 32;
    public static final int MAX_OBSERVATION_DISTANCE = 1024;
    public static final int MIN_WIRELESS_NODES = 2;
    public static final int MAX_WIRELESS_NODES = 4096;
    public static final int MIN_CHANNEL_NAME_LENGTH = 1;
    public static final int MAX_CHANNEL_NAME_LENGTH = 128;
    public static final int MIN_CHANNEL_RENDER_DISTANCE = 2;
    public static final int MAX_CHANNEL_RENDER_DISTANCE = 32;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final GlassConfig INSTANCE = new GlassConfig();

    private int propagationLimit = DEFAULT_PROPAGATION_LIMIT;
    private int propagationDelayTicks = DEFAULT_PROPAGATION_DELAY_TICKS;
    private int surfaceAnimationTicks = DEFAULT_SURFACE_ANIMATION_TICKS;
    private int observationDistance = DEFAULT_OBSERVATION_DISTANCE;
    private int wirelessMinNodes = DEFAULT_WIRELESS_MIN_NODES;
    private int wirelessMaxNodes = DEFAULT_WIRELESS_MAX_NODES;
    private int channelNameLength = DEFAULT_CHANNEL_NAME_LENGTH;
    private int channelRenderDistance = DEFAULT_CHANNEL_RENDER_DISTANCE;
    private boolean legacyFadeAnimation;

    private GlassConfig() {}

    public static GlassConfig get() {
        return INSTANCE;
    }

    public void load() {
        Path path = path();
        if (!Files.exists(path)) {
            write(path);
            return;
        }
        try {
            JsonObject root =
                    JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8))
                            .getAsJsonObject();
            Data data = GSON.fromJson(root, Data.class);
            apply(data == null ? new Data() : data);
            if (!root.has("legacyFadeAnimation")) {
                root.addProperty("legacyFadeAnimation", false);
                Files.writeString(
                        path, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
            }
        } catch (IOException | RuntimeException exception) {
            apply(new Data());
            GlassMod.LOGGER.warn("Failed to read {}; using GLASS defaults", path, exception);
        }
    }

    private void write(Path path) {
        Values defaults = Values.defaults();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    GSON.toJson(new Data(defaults)) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
        } catch (IOException exception) {
            GlassMod.LOGGER.warn("Failed to create {}; using GLASS defaults", path, exception);
        }
        apply(defaults);
    }

    private void apply(Data data) {
        apply(
                new Values(
                        data.propagationLimit,
                        data.propagationDelayTicks,
                        data.surfaceAnimationTicks,
                        data.observationDistance,
                        data.wirelessMinNodes,
                        data.wirelessMaxNodes,
                        data.channelNameLength,
                        data.channelRenderDistance,
                        data.legacyFadeAnimation));
    }

    private void apply(Values requested) {
        Values values = requested.normalized();
        propagationLimit = values.propagationLimit();
        propagationDelayTicks = values.propagationDelayTicks();
        surfaceAnimationTicks = values.surfaceAnimationTicks();
        observationDistance = values.observationDistance();
        wirelessMinNodes = values.wirelessMinNodes();
        wirelessMaxNodes = values.wirelessMaxNodes();
        channelNameLength = values.channelNameLength();
        channelRenderDistance = values.channelRenderDistance();
        legacyFadeAnimation = values.legacyFadeAnimation();
        GlassMod.LOGGER.info(
                "GLASS config: propagation={} delay={} animation={} observation={} wireless={}..{} channelName={} channelRenderDistance={} legacyFadeAnimation={}",
                propagationLimit,
                propagationDelayTicks,
                surfaceAnimationTicks,
                observationDistance,
                wirelessMinNodes,
                wirelessMaxNodes,
                channelNameLength,
                channelRenderDistance,
                legacyFadeAnimation);
    }

    public Values values() {
        return new Values(
                propagationLimit,
                propagationDelayTicks,
                surfaceAnimationTicks,
                observationDistance,
                wirelessMinNodes,
                wirelessMaxNodes,
                channelNameLength,
                channelRenderDistance,
                legacyFadeAnimation);
    }

    public boolean save(Values requested) {
        Values values = requested.normalized();
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(
                    path,
                    GSON.toJson(new Data(values)) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            apply(values);
            return true;
        } catch (IOException exception) {
            GlassMod.LOGGER.warn(
                    "Failed to save {}; keeping the previous GLASS config", path, exception);
            return false;
        }
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(GlassMod.MOD_ID + ".json");
    }

    public int propagationLimit() {
        return propagationLimit;
    }

    public int propagationDelayTicks() {
        return propagationDelayTicks;
    }

    public int surfaceAnimationTicks() {
        return surfaceAnimationTicks;
    }

    public int observationDistance() {
        return observationDistance;
    }

    public int wirelessMinNodes() {
        return wirelessMinNodes;
    }

    public int wirelessMaxNodes() {
        return wirelessMaxNodes;
    }

    public int channelNameLength() {
        return channelNameLength;
    }

    public int channelRenderDistance() {
        return channelRenderDistance;
    }

    public boolean legacyFadeAnimation() {
        return legacyFadeAnimation;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public record Values(
            int propagationLimit,
            int propagationDelayTicks,
            int surfaceAnimationTicks,
            int observationDistance,
            int wirelessMinNodes,
            int wirelessMaxNodes,
            int channelNameLength,
            int channelRenderDistance,
            boolean legacyFadeAnimation) {
        public static Values defaults() {
            return new Values(
                    DEFAULT_PROPAGATION_LIMIT,
                    DEFAULT_PROPAGATION_DELAY_TICKS,
                    DEFAULT_SURFACE_ANIMATION_TICKS,
                    DEFAULT_OBSERVATION_DISTANCE,
                    DEFAULT_WIRELESS_MIN_NODES,
                    DEFAULT_WIRELESS_MAX_NODES,
                    DEFAULT_CHANNEL_NAME_LENGTH,
                    DEFAULT_CHANNEL_RENDER_DISTANCE,
                    false);
        }

        public Values normalized() {
            int minimumWirelessNodes =
                    clamp(wirelessMinNodes, MIN_WIRELESS_NODES, MAX_WIRELESS_NODES);
            return new Values(
                    clamp(propagationLimit, MIN_PROPAGATION_LIMIT, MAX_PROPAGATION_LIMIT),
                    clamp(
                            propagationDelayTicks,
                            MIN_PROPAGATION_DELAY_TICKS,
                            MAX_PROPAGATION_DELAY_TICKS),
                    clamp(
                            surfaceAnimationTicks,
                            MIN_SURFACE_ANIMATION_TICKS,
                            MAX_SURFACE_ANIMATION_TICKS),
                    clamp(observationDistance, MIN_OBSERVATION_DISTANCE, MAX_OBSERVATION_DISTANCE),
                    minimumWirelessNodes,
                    clamp(wirelessMaxNodes, minimumWirelessNodes, MAX_WIRELESS_NODES),
                    clamp(channelNameLength, MIN_CHANNEL_NAME_LENGTH, MAX_CHANNEL_NAME_LENGTH),
                    clamp(
                            channelRenderDistance,
                            MIN_CHANNEL_RENDER_DISTANCE,
                            MAX_CHANNEL_RENDER_DISTANCE),
                    legacyFadeAnimation);
        }
    }

    private static final class Data {
        private int propagationLimit = DEFAULT_PROPAGATION_LIMIT;
        private int propagationDelayTicks = DEFAULT_PROPAGATION_DELAY_TICKS;
        private int surfaceAnimationTicks = DEFAULT_SURFACE_ANIMATION_TICKS;
        private int observationDistance = DEFAULT_OBSERVATION_DISTANCE;
        private int wirelessMinNodes = DEFAULT_WIRELESS_MIN_NODES;
        private int wirelessMaxNodes = DEFAULT_WIRELESS_MAX_NODES;
        private int channelNameLength = DEFAULT_CHANNEL_NAME_LENGTH;
        private int channelRenderDistance = DEFAULT_CHANNEL_RENDER_DISTANCE;
        private boolean legacyFadeAnimation;

        private Data() {}

        private Data(Values values) {
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
    }
}
