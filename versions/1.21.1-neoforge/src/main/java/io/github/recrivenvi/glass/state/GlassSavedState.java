package io.github.recrivenvi.glass.state;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/** 不依附方块实体的世界级持久状态。方块只提供位置与原版 BlockState；频道和无线关系 在这里独立维护。旧版方块实体 NBT 不再读取，这是现代化存档格式的明确断点。 */
public final class GlassSavedState extends SavedData {
    public static final String ID = "glass_state";
    private final Map<GlobalPos, CompoundTag> sableLinks = new LinkedHashMap<>();

    public Map<GlobalPos, CompoundTag> sableLinks() {
        return sableLinks;
    }

    public static final SavedData.Factory<GlassSavedState> TYPE =
            new SavedData.Factory<>(
                    GlassSavedState::new,
                    (tag, registries) -> fromTag(tag),
                    DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.merge(toTag());
        return tag;
    }

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
        changed |= sableLinks.remove(pos) != null;
        if (changed) setDirty();
        return changed;
    }

    private CompoundTag toTag() {
        CompoundTag tag =
                new GlassStateSnapshot(
                                terminals.values().stream()
                                        .sorted(
                                                Comparator.comparing(
                                                        value -> value.pos().toString()))
                                        .toList(),
                                projectors.values().stream()
                                        .sorted(
                                                Comparator.comparing(
                                                        value -> value.pos().toString()))
                                        .toList(),
                                List.of(),
                                new GlassStateSnapshot.Settings(2, 8, 256, 2, 256, 64, 8, false))
                        .toTag();
        net.minecraft.nbt.ListTag links = new net.minecraft.nbt.ListTag();
        sableLinks.forEach(
                (pos, value) -> {
                    CompoundTag link = value.copy();
                    link.put(
                            "pos",
                            GlobalPos.CODEC
                                    .encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, pos)
                                    .getOrThrow());
                    links.add(link);
                });
        tag.put("sableLinks", links);
        return tag;
    }

    private static GlassSavedState fromTag(CompoundTag tag) {
        GlassSavedState state = new GlassSavedState();
        GlassStateSnapshot snapshot = GlassStateSnapshot.fromTag(tag);
        snapshot.terminals().forEach(value -> state.terminals.put(value.pos(), value));
        snapshot.projectors().forEach(value -> state.projectors.put(value.pos(), value));
        for (var value : tag.getList("sableLinks", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            var link = (CompoundTag) value;
            var pos =
                    GlobalPos.CODEC
                            .parse(net.minecraft.nbt.NbtOps.INSTANCE, link.get("pos"))
                            .getOrThrow();
            state.sableLinks.put(pos, link.copy());
        }
        return state;
    }
}
