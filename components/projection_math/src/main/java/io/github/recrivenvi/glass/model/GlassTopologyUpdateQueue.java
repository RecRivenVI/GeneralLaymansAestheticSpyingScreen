package io.github.recrivenvi.glass.model;

/** 把同一服务端 tick 内的原版邻居更新合并为一次完整拓扑重算。 */
public final class GlassTopologyUpdateQueue {
    private boolean pending;

    public void mark() {
        pending = true;
    }

    public boolean consume() {
        boolean result = pending;
        pending = false;
        return result;
    }

    public void clear() {
        pending = false;
    }
}
