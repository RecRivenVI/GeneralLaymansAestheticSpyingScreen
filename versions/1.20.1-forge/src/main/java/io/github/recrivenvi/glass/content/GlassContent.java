package io.github.recrivenvi.glass.content;

import io.github.recrivenvi.glass.GlassMod;
import io.github.recrivenvi.glass.content.block.GlassExtensionBlock;
import io.github.recrivenvi.glass.content.block.GlassProjectorBlock;
import io.github.recrivenvi.glass.content.block.GlassTerminalBlock;
import io.github.recrivenvi.glass.content.block.GlassWirelessExtensionBlock;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class GlassContent {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(
                    net.minecraft.core.registries.Registries.BLOCK, GlassMod.MOD_ID);
    private static final DeferredRegister<net.minecraft.world.item.Item> ITEMS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.ITEM, GlassMod.MOD_ID);
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(
                    net.minecraft.core.registries.Registries.SOUND_EVENT, GlassMod.MOD_ID);
    public static final RegistryObject<Block> TERMINAL =
            registerBlock(
                    "glass_terminal",
                    GlassTerminalBlock::new,
                    BlockBehaviour.Properties.copy(Blocks.OBSIDIAN));
    public static final RegistryObject<Block> PROJECTOR =
            registerBlock(
                    "glass_projector",
                    GlassProjectorBlock::new,
                    BlockBehaviour.Properties.copy(Blocks.BLUE_CONCRETE));
    public static final RegistryObject<Block> EXTENSION =
            registerBlock(
                    "glass_extension",
                    GlassExtensionBlock::new,
                    BlockBehaviour.Properties.copy(Blocks.WHITE_CONCRETE));
    public static final RegistryObject<Block> WIRELESS_EXTENSION =
            registerBlock(
                    "glass_wireless_extension",
                    GlassWirelessExtensionBlock::new,
                    BlockBehaviour.Properties.copy(Blocks.GREEN_CONCRETE));
    public static final RegistryObject<SoundEvent> AMBIENCE =
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

    public static ResourceLocation id(String path) {
        return new ResourceLocation(GlassMod.MOD_ID, path);
    }

    public static boolean isSurface(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(PROJECTOR.get())
                || state.is(EXTENSION.get())
                || state.is(WIRELESS_EXTENSION.get());
    }

    private static RegistryObject<Block> registerBlock(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties) {
        RegistryObject<Block> block = BLOCKS.register(name, () -> factory.apply(properties));
        ITEMS.register(
                name,
                () ->
                        new net.minecraft.world.item.BlockItem(
                                block.get(), new net.minecraft.world.item.Item.Properties()));
        return block;
    }
}
