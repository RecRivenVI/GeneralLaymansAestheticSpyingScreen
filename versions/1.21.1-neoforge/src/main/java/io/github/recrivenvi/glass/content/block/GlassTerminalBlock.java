package io.github.recrivenvi.glass.content.block;

public final class GlassTerminalBlock extends GlassChannelBlock {
    public static final com.mojang.serialization.MapCodec<GlassTerminalBlock> CODEC =
            simpleCodec(GlassTerminalBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<GlassTerminalBlock> codec() {
        return CODEC;
    }

    public GlassTerminalBlock(Properties properties) {
        super(properties);
    }
}
