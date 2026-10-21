package io.github.recrivenvi.glass.probe.performance;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class PerformanceMixinPlugin implements IMixinConfigPlugin {
    public void onLoad(String name) {}

    public String getRefMapperConfig() {
        return null;
    }

    public boolean shouldApplyMixin(String target, String mixin) {
        return Boolean.getBoolean("glass.performance");
    }

    public void acceptTargets(Set<String> mine, Set<String> others) {}

    public List<String> getMixins() {
        return null;
    }

    public void preApply(String name, ClassNode node, String mixin, IMixinInfo info) {}

    public void postApply(String name, ClassNode node, String mixin, IMixinInfo info) {}
}
