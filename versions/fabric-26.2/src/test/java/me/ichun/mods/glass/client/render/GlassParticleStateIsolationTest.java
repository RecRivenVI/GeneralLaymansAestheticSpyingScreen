package me.ichun.mods.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import org.junit.jupiter.api.Test;

final class GlassParticleStateIsolationTest {
    @Test
    void clearsTheRemoteSnapshotBeforeReextractingTheMainView() {
        ParticlesRenderState particles = new ParticlesRenderState();
        AtomicInteger clears = new AtomicInteger();
        ParticleGroupRenderState remote = (collector, camera) -> {};
        ParticleGroupRenderState trackedRemote =
                new ParticleGroupRenderState() {
                    @Override
                    public void submit(
                            net.minecraft.client.renderer.SubmitNodeCollector collector,
                            net.minecraft.client.renderer.state.level.CameraRenderState camera) {}

                    @Override
                    public void clear() {
                        clears.incrementAndGet();
                    }
                };
        particles.add(remote);
        particles.add(trackedRemote);

        ParticleGroupRenderState main = (collector, camera) -> {};
        GlassParticleStateIsolation.restoreMain(particles, state -> state.add(main));

        assertEquals(1, clears.get());
        assertEquals(1, particles.particles.size());
        assertSame(main, particles.particles.getFirst());
    }
}
