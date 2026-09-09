package io.github.recrivenvi.glass.state;

/** 把同一服务端 tick 内的原版邻居更新合并为一次完整拓扑重算。 */
final class GlassTopologyUpdateQueue {
    private boolean pending;

    void mark() {
        pending = true;
    }

    boolean consume() {
        boolean result = pending;
        pending = false;
        return result;
    }

    void clear() {
        pending = false;
    }
}
