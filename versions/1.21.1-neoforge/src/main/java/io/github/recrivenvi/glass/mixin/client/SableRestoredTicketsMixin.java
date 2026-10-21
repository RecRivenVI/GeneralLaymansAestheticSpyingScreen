package io.github.recrivenvi.glass.mixin.client;

@org.spongepowered.asm.mixin.Mixin(
        value = dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer.class,
        remap = false)
public abstract class SableRestoredTicketsMixin {
    @org.spongepowered.asm.mixin.injection.ModifyVariable(
            method = "loadTickets",
            at = @org.spongepowered.asm.mixin.injection.At("HEAD"),
            argsOnly = true)
    private it.unimi.dsi.fastutil.objects.Object2ObjectMap<
                    java.util.UUID, dev.ryanhcode.sable.api.sublevel.ticket.SubLevelTicketInfo>
            glass$sessionTickets(
                    it.unimi.dsi.fastutil.objects.Object2ObjectMap<
                                    java.util.UUID,
                                    dev.ryanhcode.sable.api.sublevel.ticket.SubLevelTicketInfo>
                            incoming) {
        return io.github.recrivenvi.glass.remote.GlassSablePersistence.restoredTickets(incoming);
    }
}
