package io.github.recrivenvi.glass.content.block;

import com.mojang.serialization.MapCodec;

public final class GlassExtensionBlock extends GlassConnectedBlock {
    public static final MapCodec<GlassExtensionBlock> CODEC = simpleCodec(GlassExtensionBlock::new);

    public GlassExtensionBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<GlassExtensionBlock> codec() {
        return CODEC;
    }
}
