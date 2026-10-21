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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.joml.Quaternionf;

/** 上游内饰经原版动态模型入口提交；世界中的完整方块仍由区块网格负责。 */
public final class GlassBlockInteriors {
    private static final Map<ClientLevel, Map<BlockPos, Interior>> LEVELS = new WeakHashMap<>();
    private static final ResourceLocation CRYSTAL_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
    private static ModelPart crystalParts;

    private GlassBlockInteriors() {}

    public static void chunkLoaded(ClientLevel level, LevelChunk chunk) {
        Map<BlockPos, Interior> blocks = LEVELS.computeIfAbsent(level, ignored -> new HashMap<>());
        int minY = chunk.getMinBuildHeight();
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
                    .removeIf(pos -> (pos.getX() >> 4) == chunk.x && (pos.getZ() >> 4) == chunk.z);
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
        ClientLevel level =
                GlassCaptureWorld.current() == null
                        ? Minecraft.getInstance().level
                        : GlassCaptureWorld.current().level();
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

    public static void draw(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level =
                GlassCaptureWorld.current() == null
                        ? client.level
                        : GlassCaptureWorld.current().level();
        Map<BlockPos, Interior> blocks = LEVELS.get(level);
        if (blocks == null) return;
        var camera = event.getCamera().getPosition();
        var buffers = client.renderBuffers().bufferSource();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
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
            if (!event.getFrustum().isVisible(GlassSableClient.bounds(level, pos))) continue;
            BlockState block =
                    GlassChannelBlockVisibility.stateForSnapshot(
                            level.getBlockState(pos),
                            io.github.recrivenvi.glass.GlassClient.VIEW_RENDERER
                                    .isChannelExtraction());
            int light =
                    net.minecraft.client.renderer.LightTexture.pack(
                            level.getBrightness(LightLayer.BLOCK, pos),
                            level.getBrightness(LightLayer.SKY, pos));
            PoseStack pose = new PoseStack();
            pose.mulPose(
                    new org.joml.Matrix4f(
                            new org.joml.Matrix4d(event.getModelViewMatrix())
                                    .translate(-camera.x, -camera.y, -camera.z)
                                    .mul(GlassSableClient.pose(level, pos))
                                    .translate(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5)));
            if (block.getBlock() instanceof GlassProjectorBlock) {
                orientBeacon(pose, block.getValue(GlassProjectorBlock.FACING));
                Interior interior = entry.getValue();
                float step = (interior.rotation - interior.previousRotation + 360F) % 360F;
                pose.mulPose(Axis.YP.rotationDegrees(interior.previousRotation + step * partial));
                pose.translate(-.25, -.35, -.25);
                pose.scale(.5F, .5F, .5F);
                client.getBlockRenderer()
                        .renderSingleBlock(
                                Blocks.BEACON.defaultBlockState(),
                                pose,
                                buffers,
                                light,
                                OverlayTexture.NO_OVERLAY);
            } else if (block.getBlock() instanceof GlassTerminalBlock) {
                Direction facing = block.getValue(GlassTerminalBlock.FACING);
                pose.mulPose(
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
                client.getBlockRenderer()
                        .renderSingleBlock(
                                Blocks.GLASS.defaultBlockState(),
                                pose,
                                buffers,
                                light,
                                OverlayTexture.NO_OVERLAY);
            } else if (block.getBlock() instanceof GlassWirelessExtensionBlock) {
                if (crystalParts == null)
                    crystalParts = client.getEntityModels().bakeLayer(ModelLayers.END_CRYSTAL);
                pose.translate(0, -.15, 0);
                pose.scale(.6F, .6F, .6F);
                pose.translate(0, -.5, 0);
                float angle =
                        (level.getGameTime() + partial)
                                * (1.2F + wirelessUsers.getOrDefault(pos, 0) * 10F);
                pose.mulPose(Axis.YP.rotationDegrees(angle));
                pose.translate(0, 1.5, 0);
                var vertices =
                        buffers.getBuffer(
                                net.minecraft.client.renderer.RenderType.entityCutoutNoCull(
                                        CRYSTAL_TEXTURE));
                Quaternionf diagonal =
                        new Quaternionf()
                                .rotationAxis(
                                        (float) Math.PI / 3F,
                                        (float) Math.sqrt(.5),
                                        0,
                                        (float) Math.sqrt(.5));
                pose.mulPose(diagonal);
                crystalParts
                        .getChild("glass")
                        .render(pose, vertices, light, OverlayTexture.NO_OVERLAY);
                for (String part : new String[] {"glass", "cube"}) {
                    pose.scale(.875F, .875F, .875F);
                    pose.mulPose(diagonal);
                    pose.mulPose(Axis.YP.rotationDegrees(angle));
                    crystalParts
                            .getChild(part)
                            .render(pose, vertices, light, OverlayTexture.NO_OVERLAY);
                }
            }
        }
        buffers.endBatch();
        // Entity-layer cleanup disables lightmapping while terrain layers still need it.
        client.gameRenderer.lightTexture().turnOnLightLayer();
    }

    private static void orientBeacon(PoseStack pose, Direction facing) {
        switch (facing) {
            case DOWN -> pose.mulPose(Axis.XP.rotationDegrees(180F));
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
                pose.mulPose(Axis.YP.rotationDegrees(yaw));
                pose.mulPose(Axis.XP.rotationDegrees(90F));
            }
        }
    }

    private static final class Interior {
        float rotation;
        float previousRotation;
        boolean active;
        int fadeTicks;
    }
}
