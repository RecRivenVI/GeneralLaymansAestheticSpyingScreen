package io.github.recrivenvi.glass.probe;

import io.github.recrivenvi.glass.GlassMod;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 测试网格在保留侧精确平移一纹素；不随产品发布。 */
final class GlassRenderReviewBlocks {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(GlassMod.MOD_ID + "_probe");
    static final DeferredBlock<Block> INSET_RED =
            BLOCKS.registerSimpleBlock(
                    "inset_red",
                    () ->
                            BlockBehaviour.Properties.ofFullCopy(Blocks.CONCRETE.pick(DyeColor.RED))
                                    .noOcclusion());
    static final DeferredBlock<Block> INSET_GREEN =
            BLOCKS.registerSimpleBlock(
                    "inset_green",
                    () ->
                            BlockBehaviour.Properties.ofFullCopy(
                                            Blocks.CONCRETE.pick(DyeColor.LIME))
                                    .noOcclusion());

    private GlassRenderReviewBlocks() {}

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
