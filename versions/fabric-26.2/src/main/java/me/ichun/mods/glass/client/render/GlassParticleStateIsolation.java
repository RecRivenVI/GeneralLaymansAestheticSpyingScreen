package me.ichun.mods.glass.client.render;

import java.util.function.Consumer;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;

final class GlassParticleStateIsolation {
    private GlassParticleStateIsolation() {}

    static void clear(ParticlesRenderState particles) {
        particles.reset();
    }

    static void restoreMain(
            ParticlesRenderState mainParticles, Consumer<ParticlesRenderState> extractor) {
        // QuadParticleGroup 在所有提取器之间复用同一个可变状态；通道提取后必须重建主视图快照。
        clear(mainParticles);
        extractor.accept(mainParticles);
    }
}
