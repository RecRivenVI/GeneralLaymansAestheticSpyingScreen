package me.ichun.mods.glass.state;

import com.mojang.serialization.Codec;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import me.ichun.mods.glass.content.GlassContent;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** 不依附方块实体的世界级持久状态。方块只提供位置与原版 BlockState；频道和无线关系 在这里独立维护。旧版方块实体 NBT 不再读取，这是现代化存档格式的明确断点。 */
public final class GlassSavedState extends SavedData {
    private static final Codec<GlassSavedState> CODEC =
            CompoundTag.CODEC.xmap(GlassSavedState::fromTag, GlassSavedState::toTag);
    public static final SavedDataType<GlassSavedState> TYPE =
            new SavedDataType<>(
                    GlassContent.id("state"),
                    GlassSavedState::new,
                    CODEC,
                    DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<GlobalPos, GlassStateSnapshot.Terminal> terminals = new LinkedHashMap<>();
    private final Map<GlobalPos, GlassStateSnapshot.Projector> projectors = new LinkedHashMap<>();

    public Map<GlobalPos, GlassStateSnapshot.Terminal> terminals() {
        return terminals;
    }

    public Map<GlobalPos, GlassStateSnapshot.Projector> projectors() {
        return projectors;
    }

    public GlassStateSnapshot.Terminal ensureTerminal(GlobalPos pos) {
        GlassStateSnapshot.Terminal terminal =
                terminals.computeIfAbsent(
                        pos,
                        ignored ->
                                new GlassStateSnapshot.Terminal(
                                        pos,
                                        new GlassStateSnapshot.Channel(
                                                UUID.randomUUID(), "", Optional.empty(), false)));
        setDirty();
        return terminal;
    }

    public GlassStateSnapshot.Projector ensureProjector(GlobalPos pos) {
        GlassStateSnapshot.Projector projector =
                projectors.computeIfAbsent(
                        pos,
                        ignored ->
                                new GlassStateSnapshot.Projector(pos, Optional.empty(), List.of()));
        setDirty();
        return projector;
    }

    public void putTerminal(GlassStateSnapshot.Terminal terminal) {
        terminals.put(terminal.pos(), terminal);
        setDirty();
    }

    public void putProjector(GlassStateSnapshot.Projector projector) {
        projectors.put(projector.pos(), projector);
        setDirty();
    }

    public boolean remove(GlobalPos pos) {
        boolean changed = terminals.remove(pos) != null;
        changed |= projectors.remove(pos) != null;
        if (changed) setDirty();
        return changed;
    }

    private CompoundTag toTag() {
        return new GlassStateSnapshot(
                        terminals.values().stream()
                                .sorted(Comparator.comparing(value -> value.pos().toString()))
                                .toList(),
                        projectors.values().stream()
                                .sorted(Comparator.comparing(value -> value.pos().toString()))
                                .toList(),
                        List.of(),
                        new GlassStateSnapshot.Settings(2, 8, 256, 2, 256, 64, 8, false))
                .toTag();
    }

    private static GlassSavedState fromTag(CompoundTag tag) {
        GlassSavedState state = new GlassSavedState();
        GlassStateSnapshot snapshot = GlassStateSnapshot.fromTag(tag);
        snapshot.terminals().forEach(value -> state.terminals.put(value.pos(), value));
        snapshot.projectors().forEach(value -> state.projectors.put(value.pos(), value));
        return state;
    }
}
