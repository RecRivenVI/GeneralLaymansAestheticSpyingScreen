package io.github.recrivenvi.glass.mixin.client;

@org.spongepowered.asm.mixin.Mixin(
        value = dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap.class,
        remap = false)
public abstract class SableSavedDataMixin {
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final
    private net.minecraft.server.level.ServerLevel level;

    @org.spongepowered.asm.mixin.injection.Inject(
            method = "saveAll",
            at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void glass$save(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        io.github.recrivenvi.glass.remote.GlassSablePersistence.flush(level);
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "saveAll",
            at =
                    @org.spongepowered.asm.mixin.injection.At(
                            value = "INVOKE",
                            target =
                                    "Ldev/ryanhcode/sable/sublevel/storage/HoldingSubLevel;setPointer(Ldev/ryanhcode/sable/sublevel/storage/holding/GlobalSavedSubLevelPointer;)V"))
    private void glass$holdingPointer(
            dev.ryanhcode.sable.sublevel.storage.HoldingSubLevel holding,
            dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer pointer) {
        holding.setPointer(pointer);
        io.github.recrivenvi.glass.remote.GlassSablePersistence.pointerChanged(
                level, holding.data().uuid(), pointer);
    }

    @org.spongepowered.asm.mixin.injection.Inject(
            method =
                    "moveAndSaveSubLevel(Ldev/ryanhcode/sable/sublevel/ServerSubLevel;Lnet/minecraft/world/level/ChunkPos;Ljava/util/List;)V",
            at = @org.spongepowered.asm.mixin.injection.At("TAIL"))
    private void glass$loadedPointer(
            dev.ryanhcode.sable.sublevel.ServerSubLevel ship,
            net.minecraft.world.level.ChunkPos pos,
            java.util.List<java.util.UUID> connected,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        io.github.recrivenvi.glass.remote.GlassSablePersistence.pointerChanged(
                level, ship.getUniqueId(), ship.getLastSerializationPointer());
    }
}
