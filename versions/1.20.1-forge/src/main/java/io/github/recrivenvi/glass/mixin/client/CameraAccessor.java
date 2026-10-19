package io.github.recrivenvi.glass.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setPosition")
    void glass$setPosition(Vec3 position);

    @Invoker("setRotation")
    void glass$setRotation(float yaw, float pitch);

    @Accessor("rotation")
    Quaternionf glass$getRotation();

    @Accessor("forwards")
    Vector3f glass$getForwards();

    @Accessor("up")
    Vector3f glass$getUp();

    @Accessor("left")
    Vector3f glass$getLeft();
}
