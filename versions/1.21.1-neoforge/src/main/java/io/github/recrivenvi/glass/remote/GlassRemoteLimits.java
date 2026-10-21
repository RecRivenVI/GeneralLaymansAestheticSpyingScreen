package io.github.recrivenvi.glass.remote;

import com.google.gson.GsonBuilder;
import io.github.recrivenvi.glass.GlassMod;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import net.neoforged.fml.loading.FMLPaths;

/** Server budgets for the bounded remote-view trial. */
public record GlassRemoteLimits(
        int radius,
        int globalChunks,
        int bytesPerSecond,
        int initialChunksPerSecond,
        int idleTicks,
        int refreshTicks,
        int maxEntities,
        int maxSourceChunks) {
    private static final GlassRemoteLimits DEFAULT =
            new GlassRemoteLimits(2, 100, 512 * 1024, 4, 60, 100, 256, 25);

    public GlassRemoteLimits {
        if (radius < 0
                || radius > 2
                || globalChunks < 1
                || bytesPerSecond < 1024
                || initialChunksPerSecond < 1
                || initialChunksPerSecond > 20
                || idleTicks < 20
                || refreshTicks < 20
                || maxEntities < 1
                || maxEntities > 256
                || maxSourceChunks < 1
                || maxSourceChunks > 25)
            throw new IllegalArgumentException("Invalid GLASS remote-view budget");
    }

    public static GlassRemoteLimits load() {
        var path = FMLPaths.CONFIGDIR.get().resolve(GlassMod.MOD_ID + "_remote.json");
        var gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.writeString(path, gson.toJson(DEFAULT) + "\n");
                return DEFAULT;
            }
            var limits = gson.fromJson(Files.readString(path), GlassRemoteLimits.class);
            if (limits == null)
                throw new IllegalArgumentException("GLASS remote-view budget is empty");
            return limits;
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
    }
}
