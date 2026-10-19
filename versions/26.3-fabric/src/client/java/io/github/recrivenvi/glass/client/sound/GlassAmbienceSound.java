package io.github.recrivenvi.glass.client.sound;

import io.github.recrivenvi.glass.content.GlassContent;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

public final class GlassAmbienceSound extends AbstractTickableSoundInstance {
    public GlassAmbienceSound(Vec3 position) {
        super(GlassContent.AMBIENCE, SoundSource.BLOCKS, RandomSource.create());
        looping = true;
        delay = 0;
        volume = 0.015F;
        pitch = 0.6F;
        moveTo(position);
    }

    public void moveTo(Vec3 position) {
        x = position.x;
        y = position.y;
        z = position.z;
    }

    public void stopNow() {
        stop();
    }

    @Override
    public void tick() {}
}
