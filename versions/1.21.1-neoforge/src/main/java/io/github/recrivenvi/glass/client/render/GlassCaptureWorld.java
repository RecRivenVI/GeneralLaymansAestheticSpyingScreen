package io.github.recrivenvi.glass.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.LevelRenderer;

/**
 * Scoped native reads for remote packet application and rendering; main world references never
 * change.
 */
public final class GlassCaptureWorld {
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> CREATING_PARTICLES =
            ThreadLocal.withInitial(() -> false);

    private GlassCaptureWorld() {}

    public record Context(
            ClientLevel level,
            LevelRenderer renderer,
            ParticleEngine particles,
            net.minecraft.client.renderer.LightTexture lightTexture) {}

    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }

    public static boolean creatingParticles() {
        return CREATING_PARTICLES.get();
    }

    public static ParticleEngine createParticles(ClientLevel level) {
        var client = net.minecraft.client.Minecraft.getInstance();
        CREATING_PARTICLES.set(true);
        try {
            ParticleEngine particles = new ParticleEngine(level, client.getTextureManager());
            var own = (io.github.recrivenvi.glass.mixin.client.ParticleEngineAccessor) particles;
            var main =
                    (io.github.recrivenvi.glass.mixin.client.ParticleEngineAccessor)
                            client.particleEngine;
            own.glass$providers().clear();
            own.glass$providers().putAll(main.glass$providers());
            return particles;
        } finally {
            CREATING_PARTICLES.remove();
        }
    }

    public static Context current() {
        return CURRENT.get();
    }

    public static ClientLevel resolveLevel(ClientLevel main) {
        Context scope = current();
        return scope == null ? main : scope.level();
    }

    public static Scope open(Context context) {
        Context previous = CURRENT.get();
        CURRENT.set(context);
        return () -> {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        };
    }
}
