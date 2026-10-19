package io.github.recrivenvi.glass.model;

/** 只把当前频道捕获调用栈创建的区块快照标记为频道世界。 */
public final class GlassChannelSnapshotIdentity {
    private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    public Scope open() {
        depth.set(depth.get() + 1);
        return new Scope(this);
    }

    public boolean current() {
        return depth.get() > 0;
    }

    private void closeScope() {
        int current = depth.get();
        if (current <= 0) {
            throw new IllegalStateException("GLASS channel snapshot scope is not active");
        }
        if (current == 1) {
            depth.remove();
        } else {
            depth.set(current - 1);
        }
    }

    public static final class Scope implements AutoCloseable {
        private GlassChannelSnapshotIdentity owner;

        private Scope(GlassChannelSnapshotIdentity owner) {
            this.owner = owner;
        }

        @Override
        public void close() {
            if (owner == null) return;
            owner.closeScope();
            owner = null;
        }
    }
}
