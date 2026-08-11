package me.ichun.mods.glass.client.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import me.ichun.mods.glass.GlassMod;
import me.ichun.mods.glass.core.GlassPortalTransform;
import me.ichun.mods.glass.mixin.client.CloudRendererAccessor;
import me.ichun.mods.glass.mixin.client.LevelRendererAccessor;
import me.ichun.mods.glass.mixin.client.SectionOcclusionGraphAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GlobalSettingsUniform;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostChainConfig;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Owns every mutable renderer resource used by one live GLASS channel capture. */
final class GlassChannelCaptureContext implements AutoCloseable {
    private static final Identifier TRANSPARENCY_POST_CHAIN_ID =
            Identifier.withDefaultNamespace("transparency");
    private static boolean backendLogged;

    // 每个缓冲只有一个矩阵切片；复用主渲染器的缓冲会把主世界投影一并覆写为出口斜切投影。
    final ProjectionMatrixBuffer projectionBuffer = new ProjectionMatrixBuffer("GLASS channel");
    private final Projection postChainProjection = new Projection();
    private final ProjectionMatrixBuffer postChainProjectionBuffer =
            new ProjectionMatrixBuffer("GLASS channel post");
    // 地形着色器的相机平移来自全局 UBO，而不是 CameraRenderState；通道与主世界必须使用不同缓冲。
    final GlobalSettingsUniform channelGlobals = new GlobalSettingsUniform();
    final GlobalSettingsUniform mainGlobals = new GlobalSettingsUniform();
    final GlassChunkLoadingStateIsolation chunkLoadingStateIsolation =
            new GlassChunkLoadingStateIsolation();
    final EnvironmentAttributeProbe environmentProbe = new EnvironmentAttributeProbe();

    private ShaderManager.Configs transparencyConfigs;
    private PostChain transparencyChain;
    TextureTarget target;
    LevelRenderer renderer;
    LevelExtractor extractor;
    LevelRenderState renderState;
    ClientLevel level;
    private long lastAmbientTick = Long.MIN_VALUE;
    private boolean hasUnwrappedYaw;
    private float unwrappedYaw;
    private int width;
    private int height;

    GlassChannelCaptureContext() {
        postChainProjection.setupOrtho(0.1F, 1000.0F, 1.0F, 1.0F, false);
    }

    PostChain transparencyChain(Minecraft client, boolean enabled) {
        if (!enabled) return null;
        ShaderManager shaderManager = client.getShaderManager();
        ShaderManager.Configs configs =
                ((ShaderManagerConfigAccess) shaderManager).glass$getConfigs();
        if (transparencyConfigs != configs) {
            closeTransparencyChain();
            transparencyConfigs = configs;
        }
        if (transparencyChain == null) {
            PostChainConfig config = configs.postChains().get(TRANSPARENCY_POST_CHAIN_ID);
            if (config == null) return null;
            try {
                transparencyChain =
                        PostChain.load(
                                config,
                                client.getTextureManager(),
                                LevelTargetBundle.SORTING_TARGETS,
                                TRANSPARENCY_POST_CHAIN_ID,
                                postChainProjection,
                                postChainProjectionBuffer);
            } catch (ShaderManager.CompilationException exception) {
                GlassMod.LOGGER.error(
                        "Failed to create an isolated GLASS transparency chain", exception);
            }
        }
        return transparencyChain;
    }

    boolean ensure(
            Minecraft client,
            int requestedWidth,
            int requestedHeight,
            ResourceKey<Level> terminalDimension,
            BlockPos terminalPos) {
        ClientLevel requestedLevel = client.level;
        LevelRenderer sourceRenderer = client.levelRenderer;
        if (requestedLevel == null) return false;
        boolean terminalLoadedInMain =
                terminalDimension.equals(requestedLevel.dimension())
                        && requestedLevel.hasChunk(
                                terminalPos.getX() >> 4, terminalPos.getZ() >> 4);
        if (!terminalLoadedInMain) return false;
        if (target == null) {
            target =
                    new TextureTarget(
                            "GLASS channel",
                            requestedWidth,
                            requestedHeight,
                            true,
                            com.mojang.blaze3d.GpuFormat.RGBA8_UNORM);
        } else if (target.width != requestedWidth || target.height != requestedHeight) {
            target.resize(requestedWidth, requestedHeight);
        }
        logBackendOnce();
        if (renderer == null) {
            renderer =
                    new LevelRenderer(
                            client.getEntityRenderDispatcher(),
                            client.getBlockEntityRenderDispatcher(),
                            client.getModelManager(),
                            client.getTextureManager(),
                            client.getAtlasManager(),
                            client.getShaderManager(),
                            client.gameRenderer,
                            requestedWidth,
                            requestedHeight);
            renderState = new LevelRenderState();
            ((LevelRendererAccessor) renderer).glass$setLevelRenderState(renderState);
            extractor = new LevelExtractor(client, renderState, renderer);
            width = requestedWidth;
            height = requestedHeight;
        } else if (width != requestedWidth || height != requestedHeight) {
            renderer.resize(requestedWidth, requestedHeight);
            width = requestedWidth;
            height = requestedHeight;
        }
        if (level != requestedLevel) {
            level = requestedLevel;
            extractor.setLevel(level);
            environmentProbe.reset();
        }
        synchronizeCloudTexture(sourceRenderer, renderer);
        SectionOcclusionGraphAccessor source =
                (SectionOcclusionGraphAccessor) sourceRenderer.sectionOcclusionGraph();
        SectionOcclusionGraphAccessor destination =
                (SectionOcclusionGraphAccessor) renderer.sectionOcclusionGraph();
        boolean changed =
                synchronizeSet(source.glass$getLoadedChunks(), destination.glass$getLoadedChunks());
        changed |=
                synchronizeSet(
                        source.glass$getEmptySections(), destination.glass$getEmptySections());
        if (changed) renderer.sectionOcclusionGraph().invalidate();
        return true;
    }

    void animateAt(Vec3 position) {
        long gameTime = level.getGameTime();
        if (lastAmbientTick == gameTime) return;
        lastAmbientTick = gameTime;
        level.animateTick(
                net.minecraft.util.Mth.floor(position.x),
                net.minecraft.util.Mth.floor(position.y),
                net.minecraft.util.Mth.floor(position.z));
    }

    float unwrapYaw(float yaw) {
        if (!hasUnwrappedYaw) {
            hasUnwrappedYaw = true;
            unwrappedYaw = yaw;
            return yaw;
        }
        unwrappedYaw = GlassPortalTransform.unwrapYaw(unwrappedYaw, yaw);
        return unwrappedYaw;
    }

    private static void logBackendOnce() {
        if (backendLogged) return;
        GpuDevice device = RenderSystem.getDevice();
        GlassMod.LOGGER.info(
                "GLASS render backend: {} on {} ({})",
                device.getDeviceInfo().backendName(),
                device.getDeviceInfo().name(),
                device.getDeviceInfo().vendorName());
        backendLogged = true;
    }

    private static boolean synchronizeSet(LongOpenHashSet source, LongOpenHashSet target) {
        if (target.equals(source)) return false;
        target.clear();
        target.addAll(source);
        return true;
    }

    private static void synchronizeCloudTexture(LevelRenderer source, LevelRenderer target) {
        CloudRendererAccessor sourceClouds = (CloudRendererAccessor) source.cloudRenderer();
        CloudRendererAccessor targetClouds = (CloudRendererAccessor) target.cloudRenderer();
        if (targetClouds.glass$getTexture() == sourceClouds.glass$getTexture()) return;
        targetClouds.glass$setTexture(sourceClouds.glass$getTexture());
        target.cloudRenderer().markForRebuild();
    }

    private void closeTransparencyChain() {
        if (transparencyChain != null) {
            transparencyChain.close();
            transparencyChain = null;
        }
    }

    @Override
    public void close() {
        if (extractor != null && level != null) extractor.setLevel(null);
        environmentProbe.reset();
        level = null;
        extractor = null;
        renderState = null;
        projectionBuffer.close();
        closeTransparencyChain();
        postChainProjectionBuffer.close();
        channelGlobals.close();
        mainGlobals.close();
        if (renderer != null) {
            renderer.close();
            renderer = null;
        }
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }
}
