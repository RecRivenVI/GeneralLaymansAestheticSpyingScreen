package io.github.recrivenvi.glass.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class GlassPreviewCameraStateTest {
    @Test
    void invalidPersistedValuesFallBackToSafeCameraLimits() {
        GlassPreviewCameraState state =
                new GlassPreviewCameraState(
                                Float.NaN,
                                Float.POSITIVE_INFINITY,
                                -4.0F,
                                Float.NEGATIVE_INFINITY,
                                100_000.0F)
                        .normalized();

        assertEquals(0.0F, state.yawOffset());
        assertEquals(0.0F, state.pitchOffset());
        assertEquals(0.25F, state.zoom());
        assertEquals(0.0F, state.panX());
        assertEquals(8192.0F, state.panY());
    }
}
