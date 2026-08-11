package me.ichun.mods.glass.mixin.client;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SectionOcclusionGraph.class)
public interface SectionOcclusionGraphAccessor {
    @Accessor("emptySections")
    LongOpenHashSet glass$getEmptySections();

    @Accessor("loadedChunks")
    LongOpenHashSet glass$getLoadedChunks();
}
