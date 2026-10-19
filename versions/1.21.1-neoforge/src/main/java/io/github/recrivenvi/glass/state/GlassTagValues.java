package io.github.recrivenvi.glass.state;

import com.mojang.serialization.Codec;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/** Reads the GLASS snapshot schema using the native 1.21.1 NBT API. */
final class GlassTagValues {
    private GlassTagValues() {}

    static String string(CompoundTag tag, String key, String fallback) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback;
    }

    static int integer(CompoundTag tag, String key, int fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
    }

    static long longValue(CompoundTag tag, String key, long fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getLong(key) : fallback;
    }

    static boolean bool(CompoundTag tag, String key, boolean fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : fallback;
    }

    static <T> void store(CompoundTag tag, String key, Codec<T> codec, T value) {
        tag.put(key, codec.encodeStart(NbtOps.INSTANCE, value).getOrThrow());
    }

    static <T> Optional<T> read(CompoundTag tag, String key, Codec<T> codec) {
        Tag value = tag.get(key);
        return value == null ? Optional.empty() : codec.parse(NbtOps.INSTANCE, value).result();
    }
}
