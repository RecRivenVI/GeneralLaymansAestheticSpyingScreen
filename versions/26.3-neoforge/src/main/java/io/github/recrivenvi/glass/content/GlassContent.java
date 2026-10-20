package io.github.recrivenvi.glass.content;

import io.github.recrivenvi.glass.GlassMod;
import io.github.recrivenvi.glass.content.block.GlassExtensionBlock;
import io.github.recrivenvi.glass.content.block.GlassProjectorBlock;
import io.github.recrivenvi.glass.content.block.GlassTerminalBlock;
import io.github.recrivenvi.glass.content.block.GlassWirelessExtensionBlock;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GlassContent {
    private static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(GlassMod.MOD_ID);
    private static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(GlassMod.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, GlassMod.MOD_ID);
    public static final DeferredBlock<Block> TERMINAL =
            registerBlock(
                    "glass_terminal",
                    GlassTerminalBlock::new,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN));
    public static final DeferredBlock<Block> PROJECTOR =
            registerBlock(
                    "glass_projector",
                    GlassProjectorBlock::new,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    public static final DeferredBlock<Block> EXTENSION =
            registerBlock(
                    "glass_extension",
                    GlassExtensionBlock::new,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    public static final DeferredBlock<Block> WIRELESS_EXTENSION =
            registerBlock(
                    "glass_wireless_extension",
                    GlassWirelessExtensionBlock::new,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENCE =
            SOUNDS.register("ambience", () -> SoundEvent.createVariableRangeEvent(id("ambience")));

    private GlassContent() {}

    public static void initialize(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        SOUNDS.register(bus);
        bus.addListener(
                (BuildCreativeModeTabContentsEvent event) -> {
                    if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
                        event.accept(TERMINAL.get().asItem());
                        event.accept(PROJECTOR.get().asItem());
                        event.accept(EXTENSION.get().asItem());
                        event.accept(WIRELESS_EXTENSION.get().asItem());
                    }
                });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GlassMod.MOD_ID, path);
    }

    public static boolean isSurface(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(PROJECTOR.get())
                || state.is(EXTENSION.get())
                || state.is(WIRELESS_EXTENSION.get());
    }

    private static DeferredBlock<Block> registerBlock(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties) {
        DeferredBlock<Block> block = BLOCKS.registerBlock(name, factory, () -> properties);
        ITEMS.registerSimpleBlockItem(block);
        return block;
    }
}
