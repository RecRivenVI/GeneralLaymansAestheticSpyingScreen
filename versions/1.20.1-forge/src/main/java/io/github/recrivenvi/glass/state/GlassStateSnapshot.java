package io.github.recrivenvi.glass.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

/** 服务端权威状态的不可变网络快照。 */
public record GlassStateSnapshot(
        List<Terminal> terminals,
        List<Projector> projectors,
        List<Network> networks,
        Settings settings) {
    public GlassStateSnapshot {
        terminals = List.copyOf(terminals);
        projectors = List.copyOf(projectors);
        networks = List.copyOf(networks);
    }

    public CompoundTag toTag() {
        CompoundTag root = new CompoundTag();
        ListTag terminalTags = new ListTag();
        terminals.forEach(value -> terminalTags.add(value.toTag()));
        root.put("terminals", terminalTags);
        ListTag projectorTags = new ListTag();
        projectors.forEach(value -> projectorTags.add(value.toTag()));
        root.put("projectors", projectorTags);
        ListTag networkTags = new ListTag();
        networks.forEach(value -> networkTags.add(value.toTag()));
        root.put("networks", networkTags);
        root.put("settings", settings.toTag());
        return root;
    }

    public static GlassStateSnapshot fromTag(CompoundTag root) {
        ArrayList<Terminal> terminals = new ArrayList<>();
        for (int index = 0;
                index < root.getList("terminals", net.minecraft.nbt.Tag.TAG_COMPOUND).size();
                index++) {
            terminals.add(
                    Terminal.fromTag(
                            root.getList("terminals", net.minecraft.nbt.Tag.TAG_COMPOUND)
                                    .getCompound(index)));
        }
        ArrayList<Projector> projectors = new ArrayList<>();
        for (int index = 0;
                index < root.getList("projectors", net.minecraft.nbt.Tag.TAG_COMPOUND).size();
                index++) {
            projectors.add(
                    Projector.fromTag(
                            root.getList("projectors", net.minecraft.nbt.Tag.TAG_COMPOUND)
                                    .getCompound(index)));
        }
        ArrayList<Network> networks = new ArrayList<>();
        for (int index = 0;
                index < root.getList("networks", net.minecraft.nbt.Tag.TAG_COMPOUND).size();
                index++) {
            networks.add(
                    Network.fromTag(
                            root.getList("networks", net.minecraft.nbt.Tag.TAG_COMPOUND)
                                    .getCompound(index)));
        }
        return new GlassStateSnapshot(
                terminals, projectors, networks, Settings.fromTag(root.getCompound("settings")));
    }

    public record Channel(UUID id, String name, Optional<UUID> owner, boolean publicChannel) {
        public Channel {
            name = name == null ? "" : name;
            owner = owner == null ? Optional.empty() : owner;
        }

        private CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("id", id.toString());
            tag.putString("name", name);
            owner.ifPresent(value -> tag.putString("owner", value.toString()));
            tag.putBoolean("public", publicChannel);
            return tag;
        }

        private static Channel fromTag(CompoundTag tag) {
            String owner = GlassTagValues.string(tag, "owner", "");
            return new Channel(
                    parseUuid(GlassTagValues.string(tag, "id", ""), UUID.randomUUID()),
                    GlassTagValues.string(tag, "name", ""),
                    owner.isEmpty()
                            ? Optional.empty()
                            : Optional.of(parseUuid(owner, UUID.randomUUID())),
                    GlassTagValues.bool(tag, "public", false));
        }
    }

    public record Terminal(GlobalPos pos, Channel channel) {
        private CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            GlassTagValues.store(tag, "pos", GlobalPos.CODEC, pos);
            tag.put("channel", channel.toTag());
            return tag;
        }

        private static Terminal fromTag(CompoundTag tag) {
            return new Terminal(
                    GlassTagValues.read(tag, "pos", GlobalPos.CODEC).orElseThrow(),
                    Channel.fromTag(tag.getCompound("channel")));
        }
    }

    public record Projector(
            GlobalPos pos, Optional<UUID> channelId, List<GlobalPos> wirelessPositions) {
        public Projector {
            channelId = channelId == null ? Optional.empty() : channelId;
            wirelessPositions = List.copyOf(wirelessPositions);
        }

        private CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            GlassTagValues.store(tag, "pos", GlobalPos.CODEC, pos);
            channelId.ifPresent(value -> tag.putString("channel", value.toString()));
            ListTag wireless = new ListTag();
            for (GlobalPos value : wirelessPositions) {
                CompoundTag entry = new CompoundTag();
                GlassTagValues.store(entry, "pos", GlobalPos.CODEC, value);
                wireless.add(entry);
            }
            tag.put("wireless", wireless);
            return tag;
        }

        private static Projector fromTag(CompoundTag tag) {
            String channel = GlassTagValues.string(tag, "channel", "");
            ArrayList<GlobalPos> wireless = new ArrayList<>();
            ListTag list = tag.getList("wireless", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int index = 0; index < list.size(); index++) {
                GlassTagValues.read(list.getCompound(index), "pos", GlobalPos.CODEC)
                        .ifPresent(wireless::add);
            }
            return new Projector(
                    GlassTagValues.read(tag, "pos", GlobalPos.CODEC).orElseThrow(),
                    channel.isEmpty()
                            ? Optional.empty()
                            : Optional.of(parseUuid(channel, UUID.randomUUID())),
                    wireless);
        }
    }

    public record Network(
            GlobalPos projector,
            UUID channelId,
            GlobalPos terminal,
            Direction terminalFacing,
            Direction facing,
            boolean active,
            long revision,
            List<Surface> surfaces,
            List<GlobalPos> wirelessPositions) {
        public Network {
            surfaces = List.copyOf(surfaces);
            wirelessPositions = List.copyOf(wirelessPositions);
        }

        private CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            GlassTagValues.store(tag, "projector", GlobalPos.CODEC, projector);
            tag.putString("channel", channelId.toString());
            GlassTagValues.store(tag, "terminal", GlobalPos.CODEC, terminal);
            tag.putInt("terminalFacing", terminalFacing.get3DDataValue());
            tag.putInt("facing", facing.get3DDataValue());
            tag.putBoolean("active", active);
            tag.putLong("revision", revision);
            ListTag surfaceTags = new ListTag();
            surfaces.forEach(value -> surfaceTags.add(value.toTag()));
            tag.put("surfaces", surfaceTags);
            ListTag wirelessTags = new ListTag();
            for (GlobalPos value : wirelessPositions) {
                CompoundTag entry = new CompoundTag();
                GlassTagValues.store(entry, "pos", GlobalPos.CODEC, value);
                wirelessTags.add(entry);
            }
            tag.put("wireless", wirelessTags);
            return tag;
        }

        private static Network fromTag(CompoundTag tag) {
            ArrayList<Surface> surfaces = new ArrayList<>();
            ListTag surfaceTags = tag.getList("surfaces", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int index = 0; index < surfaceTags.size(); index++) {
                surfaces.add(Surface.fromTag(surfaceTags.getCompound(index)));
            }
            ArrayList<GlobalPos> wireless = new ArrayList<>();
            ListTag wirelessTags = tag.getList("wireless", net.minecraft.nbt.Tag.TAG_COMPOUND);
            for (int index = 0; index < wirelessTags.size(); index++) {
                GlassTagValues.read(wirelessTags.getCompound(index), "pos", GlobalPos.CODEC)
                        .ifPresent(wireless::add);
            }
            return new Network(
                    GlassTagValues.read(tag, "projector", GlobalPos.CODEC).orElseThrow(),
                    parseUuid(GlassTagValues.string(tag, "channel", ""), UUID.randomUUID()),
                    GlassTagValues.read(tag, "terminal", GlobalPos.CODEC).orElseThrow(),
                    Direction.from3DDataValue(
                            GlassTagValues.integer(
                                    tag, "terminalFacing", Direction.NORTH.get3DDataValue())),
                    Direction.from3DDataValue(
                            GlassTagValues.integer(
                                    tag, "facing", Direction.NORTH.get3DDataValue())),
                    GlassTagValues.bool(tag, "active", false),
                    GlassTagValues.longValue(tag, "revision", 0L),
                    surfaces,
                    wireless);
        }
    }

    public record Surface(GlobalPos pos, Direction face, int distance) {
        private CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            GlassTagValues.store(tag, "pos", GlobalPos.CODEC, pos);
            tag.putInt("face", face.get3DDataValue());
            tag.putInt("distance", distance);
            return tag;
        }

        private static Surface fromTag(CompoundTag tag) {
            return new Surface(
                    GlassTagValues.read(tag, "pos", GlobalPos.CODEC).orElseThrow(),
                    Direction.from3DDataValue(
                            GlassTagValues.integer(tag, "face", Direction.NORTH.get3DDataValue())),
                    GlassTagValues.integer(tag, "distance", 0));
        }
    }

    public record Settings(
            int propagationDelayTicks,
            int surfaceAnimationTicks,
            int observationDistance,
            int wirelessMinNodes,
            int wirelessMaxNodes,
            int channelNameLength,
            int channelRenderDistance,
            boolean legacyFadeAnimation) {
        private CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("propagationDelayTicks", propagationDelayTicks);
            tag.putInt("surfaceAnimationTicks", surfaceAnimationTicks);
            tag.putInt("observationDistance", observationDistance);
            tag.putInt("wirelessMinNodes", wirelessMinNodes);
            tag.putInt("wirelessMaxNodes", wirelessMaxNodes);
            tag.putInt("channelNameLength", channelNameLength);
            tag.putInt("channelRenderDistance", channelRenderDistance);
            tag.putBoolean("legacyFadeAnimation", legacyFadeAnimation);
            return tag;
        }

        private static Settings fromTag(CompoundTag tag) {
            return new Settings(
                    GlassTagValues.integer(tag, "propagationDelayTicks", 2),
                    GlassTagValues.integer(tag, "surfaceAnimationTicks", 8),
                    GlassTagValues.integer(tag, "observationDistance", 256),
                    GlassTagValues.integer(tag, "wirelessMinNodes", 2),
                    GlassTagValues.integer(tag, "wirelessMaxNodes", 256),
                    GlassTagValues.integer(tag, "channelNameLength", 64),
                    GlassTagValues.integer(tag, "channelRenderDistance", 8),
                    GlassTagValues.bool(tag, "legacyFadeAnimation", false));
        }
    }

    private static UUID parseUuid(String value, UUID fallback) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
