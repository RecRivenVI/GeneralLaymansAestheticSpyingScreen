package io.github.recrivenvi.glass.client.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.recrivenvi.glass.GlassMod;
import io.github.recrivenvi.glass.model.GlassPreviewCameraState;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import net.fabricmc.loader.api.FabricLoader;

/** 客户端本地的空间预览视角。它不参与服务端玩法配置，也不会随频道同步。 */
final class GlassPreviewPreferences {
    private static final int FORMAT_VERSION = 2;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final GlassPreviewPreferences INSTANCE = new GlassPreviewPreferences();

    private final Path path;
    private GlassPreviewCameraState state;

    private GlassPreviewPreferences() {
        path = FabricLoader.getInstance().getConfigDir().resolve(GlassMod.MOD_ID + "-preview.json");
        state = load();
    }

    static GlassPreviewPreferences get() {
        return INSTANCE;
    }

    synchronized GlassPreviewCameraState state() {
        return state;
    }

    synchronized void save(GlassPreviewCameraState requested) {
        GlassPreviewCameraState normalized = requested.normalized();
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(
                    temporary,
                    GSON.toJson(new Data(normalized)) + System.lineSeparator(),
                    StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporary,
                        path,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailure) {
                if (!Files.exists(temporary)) throw atomicFailure;
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            state = normalized;
        } catch (IOException | RuntimeException exception) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            GlassMod.LOGGER.warn(
                    "Failed to save GLASS preview camera state to {}", path, exception);
        }
    }

    private GlassPreviewCameraState load() {
        if (!Files.exists(path)) return GlassPreviewCameraState.defaults();
        try {
            Data loaded = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), Data.class);
            return loaded == null || loaded.formatVersion != FORMAT_VERSION
                    ? GlassPreviewCameraState.defaults()
                    : loaded.state().normalized();
        } catch (IOException | RuntimeException exception) {
            GlassMod.LOGGER.warn(
                    "Failed to read GLASS preview camera state from {}; using defaults",
                    path,
                    exception);
            return GlassPreviewCameraState.defaults();
        }
    }

    private static final class Data {
        private int formatVersion = FORMAT_VERSION;
        private float yawOffset;
        private float pitchOffset;
        private float zoom = GlassPreviewCameraState.DEFAULT_VIEW_WIDTH;
        private float panX;
        private float panY;

        private Data() {}

        private Data(GlassPreviewCameraState state) {
            yawOffset = state.yawOffset();
            pitchOffset = state.pitchOffset();
            zoom = state.zoom();
            panX = state.panX();
            panY = state.panY();
        }

        private GlassPreviewCameraState state() {
            return new GlassPreviewCameraState(yawOffset, pitchOffset, zoom, panX, panY);
        }
    }
}
