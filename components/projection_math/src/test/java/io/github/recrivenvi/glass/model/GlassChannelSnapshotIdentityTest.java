package io.github.recrivenvi.glass.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

final class GlassChannelSnapshotIdentityTest {
    @Test
    void channelIdentityIsNestedButNeverLeaksToMainWorldWorkerThreads()
            throws InterruptedException {
        GlassChannelSnapshotIdentity identity = new GlassChannelSnapshotIdentity();
        AtomicBoolean workerSawChannel = new AtomicBoolean(true);

        assertFalse(identity.current());
        try (GlassChannelSnapshotIdentity.Scope outer = identity.open()) {
            assertTrue(identity.current());
            try (GlassChannelSnapshotIdentity.Scope inner = identity.open()) {
                assertTrue(identity.current());
                Thread worker = new Thread(() -> workerSawChannel.set(identity.current()));
                worker.start();
                worker.join();
            }
            assertTrue(identity.current());
        }

        assertFalse(identity.current());
        assertFalse(workerSawChannel.get());
    }
}
