package io.github.recrivenvi.glass.content;

import io.github.recrivenvi.glass.GlassMod;
import io.github.recrivenvi.glass.content.block.GlassExtensionBlock;
import io.github.recrivenvi.glass.content.block.GlassProjectorBlock;
import io.github.recrivenvi.glass.content.block.GlassTerminalBlock;
import io.github.recrivenvi.glass.content.block.GlassWirelessExtensionBlock;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class GlassContent {
    public static final Block TERMINAL =
            registerBlock(
                    "glass_terminal",
                    GlassTerminalBlock::new,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN));

    public static final Block PROJECTOR =
            registerBlock(
                    "glass_projector",
                    GlassProjectorBlock::new,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));

    public static final Block EXTENSION =
            registerBlock(
                    "glass_extension",
                    GlassExtensionBlock::new,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));

    public static final Block WIRELESS_EXTENSION =
            registerBlock(
                    "glass_wireless_extension",
                    GlassWirelessExtensionBlock::new,
                    BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS));

    public static final SoundEvent AMBIENCE =
            Registry.register(
                    BuiltInRegistries.SOUND_EVENT,
                    id("ambience"),
                    SoundEvent.createVariableRangeEvent(id("ambience")));

    private GlassContent() {}

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.REDSTONE_BLOCKS)
                .register(
                        entries -> {
                            entries.accept(TERMINAL.asItem());
                            entries.accept(PROJECTOR.asItem());
                            entries.accept(EXTENSION.asItem());
                            entries.accept(WIRELESS_EXTENSION.asItem());
                        });
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GlassMod.MOD_ID, path);
    }

    public static boolean isSurface(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(PROJECTOR) || state.is(EXTENSION) || state.is(WIRELESS_EXTENSION);
    }

    private static Block registerBlock(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties) {
        Identifier id = id(name);
        BlockItemId blockItemId = BlockItemId.create(id, id);
        Block block = factory.apply(properties.setId(blockItemId.block()));
        Registry.register(BuiltInRegistries.BLOCK, blockItemId.block(), block);
        Registry.register(
                BuiltInRegistries.ITEM,
                blockItemId.item(),
                new BlockItem(
                        block,
                        new Item.Properties()
                                .useBlockDescriptionPrefix()
                                .setId(blockItemId.item())));
        return block;
    }
}
