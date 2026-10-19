package io.github.recrivenvi.glass.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class GlassChannelCameraStateTest {
    static {
        io.github.recrivenvi.glass.ForgeTestBootstrap.initialize();
    }

    @Test
    void derivesAnIndependentBlockPositionFromTheTransformedCameraPosition() {
        Vec3 transformedPosition = new Vec3(8.4999996, 1.6199999, -12.5);

        BlockPos blockPosition =
                GlassChannelCameraState.immutableBlockPosition(transformedPosition);

        assertEquals(new BlockPos(8, 1, -13), blockPosition);
    }
}
