package io.github.recrivenvi.glass.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.recrivenvi.glass.client.GlassClientState;
import io.github.recrivenvi.glass.content.block.GlassProjectorBlock;
import io.github.recrivenvi.glass.content.block.GlassTerminalBlock;
import io.github.recrivenvi.glass.content.block.GlassWirelessExtensionBlock;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EndCrystalRenderer;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;

/** 上游内饰经原版动态模型入口提交；世界中的完整方块仍由区块网格负责。 */
public final class GlassBlockInteriors {
    private static final Map<ClientLevel, Map<BlockPos, Interior>> LEVELS = new WeakHashMap<>();
    private static final Identifier CRYSTAL_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
    private static EndCrystalModel crystalModel;

    private GlassBlockInteriors() {}

    public static void chunkLoaded(ClientLevel level, LevelChunk chunk) {
        Map<BlockPos, Interior> blocks = LEVELS.computeIfAbsent(level, ignored -> new HashMap<>());
        int minY = chunk.getMinY();
        var sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            var section = sections[index];
            if (!section.maybeHas(GlassBlockInteriors::hasInterior)) continue;
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        BlockState state = section.getBlockState(x, y, z);
                        if (hasInterior(state)) {
                            BlockPos pos =
                                    new BlockPos(
                                            chunk.getPos().getMinBlockX() + x,
                                            minY + index * 16 + y,
                                            chunk.getPos().getMinBlockZ() + z);
                            blocks.computeIfAbsent(pos, ignored -> new Interior());
                        }
                    }
        }
    }

    public static void chunkUnloaded(ClientLevel level, ChunkPos chunk) {
        Map<BlockPos, Interior> blocks = LEVELS.get(level);
        if (blocks != null)
            blocks.keySet()
                    .removeIf(
                            pos ->
                                    (pos.getX() >> 4) == chunk.x()
                                            && (pos.getZ() >> 4) == chunk.z());
    }

    public static void blockChanged(ClientLevel level, BlockPos pos, BlockState state) {
        Map<BlockPos, Interior> blocks = LEVELS.computeIfAbsent(level, ignored -> new HashMap<>());
        if (hasInterior(state)) blocks.computeIfAbsent(pos.immutable(), ignored -> new Interior());
        else blocks.remove(pos);
    }

    private static boolean hasInterior(BlockState state) {
        return state.getBlock() instanceof GlassProjectorBlock
                || state.getBlock() instanceof GlassTerminalBlock
                || state.getBlock() instanceof GlassWirelessExtensionBlock;
    }

    public static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        Map<BlockPos, Interior> blocks = LEVELS.get(level);
        if (blocks == null) return;
        Map<BlockPos, Boolean> active = new HashMap<>();
        GlassClientState.snapshot()
                .networks()
                .forEach(
                        network -> {
                            if (network.projector().dimension().equals(level.dimension()))
                                active.put(network.projector().pos(), network.active());
                        });
        blocks.forEach(
                (pos, interior) -> {
                    interior.previousRotation = interior.rotation;
                    boolean powered = active.getOrDefault(pos, false);
                    if (powered != interior.active) {
                        interior.active = powered;
                        interior.fadeTicks = 12;
                    }
                    if (interior.fadeTicks > 0) interior.fadeTicks--;
                    // TileEntityGlassMaster uses the upstream twelve-tick speed ramp independently
                    // of the projection animation.
                    float factor = interior.fadeTicks / 12F;
                    if (powered) factor = 1F - factor;
                    interior.rotation = (interior.rotation + 20F * factor) % 360F;
                });
    }

    public static void submit(LevelRenderState state, SubmitNodeCollector collector) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        Map<BlockPos, Interior> blocks = LEVELS.get(level);
        if (blocks == null) return;
        var camera = state.cameraRenderState;
        Map<BlockPos, Integer> wirelessUsers = new HashMap<>();
        GlassClientState.snapshot()
                .projectors()
                .forEach(
                        projector ->
                                projector
                                        .wirelessPositions()
                                        .forEach(
                                                pos -> {
                                                    if (pos.dimension().equals(level.dimension()))
                                                        wirelessUsers.merge(
                                                                pos.pos(), 1, Integer::sum);
                                                }));
        for (var entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!camera.cullFrustum.isVisible(new AABB(pos))) continue;
            BlockState block =
                    GlassChannelBlockVisibility.stateForSnapshot(
                            level.getBlockState(pos),
                            io.github.recrivenvi.glass.GlassClient.VIEW_RENDERER
                                    .isChannelExtraction());
            int light =
                    LightCoordsUtil.pack(
                            level.getBrightness(LightLayer.BLOCK, pos),
                            level.getBrightness(LightLayer.SKY, pos));
            PoseStack pose = new PoseStack();
            pose.translate(
                    pos.getX() + .5 - camera.pos.x,
                    pos.getY() + .5 - camera.pos.y,
                    pos.getZ() + .5 - camera.pos.z);
            if (block.getBlock() instanceof GlassProjectorBlock) {
                Direction facing = block.getValue(GlassProjectorBlock.FACING);
                orientBeacon(pose, facing);
                Interior interior = entry.getValue();
                float step = (interior.rotation - interior.previousRotation + 360F) % 360F;
                pose.rotate(
                        Axis.YP.rotationDegrees(
                                interior.previousRotation + step * state.worldPartialTicks));
                pose.translate(-.25, -.35, -.25);
                pose.scale(.5F, .5F, .5F);
                submitBlock(client, Blocks.BEACON.defaultBlockState(), pose, collector, light);
            } else if (block.getBlock() instanceof GlassTerminalBlock) {
                Direction facing = block.getValue(GlassTerminalBlock.FACING);
                pose.rotate(
                        new Quaternionf()
                                .rotationAxis(
                                        (float) Math.PI / 4F,
                                        facing.getStepX(),
                                        facing.getStepY(),
                                        facing.getStepZ()));
                pose.translate(
                        -.125 - facing.getStepX() * .4,
                        -.125 - facing.getStepY() * .4,
                        -.125 - facing.getStepZ() * .4);
                pose.scale(.25F, .25F, .25F);
                submitBlock(client, Blocks.GLASS.defaultBlockState(), pose, collector, light);
            } else if (block.getBlock() instanceof GlassWirelessExtensionBlock) {
                if (crystalModel == null)
                    crystalModel =
                            new WirelessCrystalModel(
                                    client.getEntityModels().bakeLayer(ModelLayers.END_CRYSTAL));
                pose.translate(0, -.15, 0);
                pose.scale(.3F, .3F, .3F);
                EndCrystalRenderState crystal = new EndCrystalRenderState();
                crystal.ageInTicks =
                        (state.gameTime + state.worldPartialTicks)
                                * (1.2F + wirelessUsers.getOrDefault(pos, 0) * 10F)
                                / 3F;
                crystal.showsBottom = false;
                collector.submitModel(
                        crystalModel,
                        crystal,
                        pose,
                        RenderTypes.entityCutout(CRYSTAL_TEXTURE),
                        light,
                        OverlayTexture.NO_OVERLAY,
                        0);
            }
        }
    }

    private static void submitBlock(
            Minecraft client,
            BlockState state,
            PoseStack pose,
            SubmitNodeCollector collector,
            int light) {
        BlockModelRenderState model = new BlockModelRenderState();
        client.getBlockModelResolver().update(model, state, BlockDisplayContext.create());
        model.blockLightCoords = 0;
        model.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
    }

    private static void orientBeacon(PoseStack pose, Direction facing) {
        switch (facing) {
            case DOWN -> pose.rotate(Axis.XP.rotationDegrees(180F));
            case UP -> {}
            default -> {
                float yaw =
                        switch (facing) {
                            case SOUTH -> 0F;
                            case WEST -> -90F;
                            case NORTH -> -180F;
                            case EAST -> -270F;
                            default -> throw new IllegalStateException();
                        };
                pose.rotate(Axis.YP.rotationDegrees(yaw));
                pose.rotate(Axis.XP.rotationDegrees(90F));
            }
        }
    }

    private static final class Interior {
        float rotation;
        float previousRotation;
        boolean active;
        int fadeTicks;
    }

    /** 旧上游传入零浮动量，只保留水晶的旋转。 */
    private static final class WirelessCrystalModel extends EndCrystalModel {
        WirelessCrystalModel(ModelPart root) {
            super(root);
        }

        @Override
        public void setupAnim(EndCrystalRenderState state) {
            super.setupAnim(state);
            outerGlass.y -= EndCrystalRenderer.getY(state.ageInTicks) * 8F;
        }
    }
}
