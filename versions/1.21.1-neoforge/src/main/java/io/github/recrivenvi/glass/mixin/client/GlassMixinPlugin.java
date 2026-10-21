package io.github.recrivenvi.glass.mixin.client;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class GlassMixinPlugin implements IMixinConfigPlugin {
    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.endsWith("SableBlockAssemblyMixin")
                || mixin.endsWith("SableSavedDataMixin")
                || mixin.endsWith("SableRestoredTicketsMixin"))
            return net.neoforged.fml.loading.FMLLoader.getLoadingModList().getMods().stream()
                    .anyMatch(m -> m.getModId().equals("sable"));
        if (mixin.endsWith("VanillaCaptureSectionsMixin"))
            return net.neoforged.fml.loading.FMLLoader.getLoadingModList().getMods().stream()
                    .noneMatch(m -> m.getModId().equals("sodium"));
        return true;
    }

    @Override
    public void onLoad(String name) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> mine, Set<String> others) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}

    @Override
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (!mixin.endsWith("ClientPacketListenerWorldMixin")) return;
        // Mixin selectors exclude merged callbacks. Scope the dispatcher's native field reads
        // after merging, including Sodium light readiness and Sable entity interpolation.
        for (var method : node.methods) {
            for (var instruction : method.instructions.toArray()) {
                if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode field
                        && field.getOpcode() == org.objectweb.asm.Opcodes.GETFIELD
                        && field.owner.equals(
                                "net/minecraft/client/multiplayer/ClientPacketListener")
                        && field.name.equals("level")) {
                    method.instructions.insert(
                            field,
                            new org.objectweb.asm.tree.MethodInsnNode(
                                    org.objectweb.asm.Opcodes.INVOKESTATIC,
                                    "io/github/recrivenvi/glass/client/render/GlassCaptureWorld",
                                    "resolveLevel",
                                    "(Lnet/minecraft/client/multiplayer/ClientLevel;)Lnet/minecraft/client/multiplayer/ClientLevel;",
                                    false));
                }
            }
        }
    }
}
