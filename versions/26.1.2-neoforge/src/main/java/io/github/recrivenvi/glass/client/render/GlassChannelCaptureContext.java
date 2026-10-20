package io.github.recrivenvi.glass.client.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.recrivenvi.glass.GlassMod;
import io.github.recrivenvi.glass.core.GlassPortalTransform;
import io.github.recrivenvi.glass.mixin.client.CloudRendererAccessor;
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
    final EnvironmentAttributeProbe environmentProbe = new EnvironmentAttributeProbe();

    private ShaderManager.Configs transparencyConfigs;
    private PostChain transparencyChain;
    TextureTarget target;
    LevelRenderer renderer;
    final net.minecraft.client.renderer.state.GameRenderState gameState =
            new net.minecraft.client.renderer.state.GameRenderState();
    net.minecraft.client.renderer.feature.FeatureRenderDispatcher featureDispatcher;
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
        gameState.optionsRenderState.cloudRange =
                client.gameRenderer.getGameRenderState().optionsRenderState.cloudRange;
        gameState.optionsRenderState.cutoutLeaves =
                client.gameRenderer.getGameRenderState().optionsRenderState.cutoutLeaves;
        gameState.optionsRenderState.improvedTransparency =
                client.gameRenderer.getGameRenderState().optionsRenderState.improvedTransparency;
        gameState.optionsRenderState.ambientOcclusion =
                client.gameRenderer.getGameRenderState().optionsRenderState.ambientOcclusion;
        gameState.optionsRenderState.menuBackgroundBlurriness =
                client.gameRenderer.getGameRenderState()
                        .optionsRenderState
                        .menuBackgroundBlurriness;
        gameState.optionsRenderState.panoramaSpeed =
                client.gameRenderer.getGameRenderState().optionsRenderState.panoramaSpeed;
        gameState.optionsRenderState.maxAnisotropyValue =
                client.gameRenderer.getGameRenderState().optionsRenderState.maxAnisotropyValue;
        gameState.optionsRenderState.textureFiltering =
                client.gameRenderer.getGameRenderState().optionsRenderState.textureFiltering;
        gameState.optionsRenderState.bobView =
                client.gameRenderer.getGameRenderState().optionsRenderState.bobView;
        gameState.optionsRenderState.hideGui =
                client.gameRenderer.getGameRenderState().optionsRenderState.hideGui;
        gameState.optionsRenderState.screenEffectScale =
                client.gameRenderer.getGameRenderState().optionsRenderState.screenEffectScale;
        gameState.optionsRenderState.glintSpeed =
                client.gameRenderer.getGameRenderState().optionsRenderState.glintSpeed;
        gameState.optionsRenderState.glintStrength =
                client.gameRenderer.getGameRenderState().optionsRenderState.glintStrength;
        gameState.optionsRenderState.damageTiltStrength =
                client.gameRenderer.getGameRenderState().optionsRenderState.damageTiltStrength;
        gameState.optionsRenderState.backgroundForChatOnly =
                client.gameRenderer.getGameRenderState().optionsRenderState.backgroundForChatOnly;
        gameState.optionsRenderState.textBackgroundOpacity =
                client.gameRenderer.getGameRenderState().optionsRenderState.textBackgroundOpacity;
        gameState.optionsRenderState.cloudStatus =
                client.gameRenderer.getGameRenderState().optionsRenderState.cloudStatus;
        gameState.optionsRenderState.cameraType =
                client.gameRenderer.getGameRenderState().optionsRenderState.cameraType;
        gameState.optionsRenderState.renderDistance =
                client.gameRenderer.getGameRenderState().optionsRenderState.renderDistance;
        gameState.windowRenderState.width =
                client.gameRenderer.getGameRenderState().windowRenderState.width;
        gameState.windowRenderState.height =
                client.gameRenderer.getGameRenderState().windowRenderState.height;
        gameState.windowRenderState.guiScale =
                client.gameRenderer.getGameRenderState().windowRenderState.guiScale;
        gameState.windowRenderState.appropriateLineWidth =
                client.gameRenderer.getGameRenderState().windowRenderState.appropriateLineWidth;
        gameState.windowRenderState.isMinimized =
                client.gameRenderer.getGameRenderState().windowRenderState.isMinimized;
        gameState.windowRenderState.isResized =
                client.gameRenderer.getGameRenderState().windowRenderState.isResized;
        ClientLevel requestedLevel = client.level;
        LevelRenderer sourceRenderer = client.levelRenderer;
        if (requestedLevel == null) return false;
        boolean terminalLoadedInMain =
                terminalDimension.equals(requestedLevel.dimension())
                        && requestedLevel.hasChunk(
                                terminalPos.getX() >> 4, terminalPos.getZ() >> 4);
        if (!terminalLoadedInMain) return false;
        if (target == null) {
            target = new TextureTarget("GLASS channel", requestedWidth, requestedHeight, true);
        } else if (target.width != requestedWidth || target.height != requestedHeight) {
            target.resize(requestedWidth, requestedHeight);
        }
        logBackendOnce();
        if (renderer == null) {
            var buffers = client.renderBuffers();
            featureDispatcher =
                    new net.minecraft.client.renderer.feature.FeatureRenderDispatcher(
                            new net.minecraft.client.renderer.SubmitNodeStorage(),
                            client.getModelManager(),
                            buffers.bufferSource(),
                            client.getAtlasManager(),
                            buffers.outlineBufferSource(),
                            buffers.crumblingBufferSource(),
                            client.font,
                            gameState);
            renderer =
                    new LevelRenderer(
                            client,
                            client.getEntityRenderDispatcher(),
                            client.getBlockEntityRenderDispatcher(),
                            buffers,
                            gameState,
                            featureDispatcher);
            // This version constructs sky resources in the resource reload entry point.
            renderer.onResourceManagerReload(client.getResourceManager());
            renderState = gameState.levelRenderState;
            width = requestedWidth;
            height = requestedHeight;
        } else if (width != requestedWidth || height != requestedHeight) {
            renderer.resize(requestedWidth, requestedHeight);
            width = requestedWidth;
            height = requestedHeight;
        }
        if (level != requestedLevel) {
            level = requestedLevel;
            renderer.setLevel(level);
            environmentProbe.reset();
        }
        synchronizeCloudTexture(sourceRenderer, renderer);
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
                device.getBackendName(),
                device.getRenderer(),
                device.getVendor());
        backendLogged = true;
    }

    private static void synchronizeCloudTexture(LevelRenderer source, LevelRenderer target) {
        CloudRendererAccessor sourceClouds = (CloudRendererAccessor) source.getCloudRenderer();
        CloudRendererAccessor targetClouds = (CloudRendererAccessor) target.getCloudRenderer();
        if (targetClouds.glass$getTexture() == sourceClouds.glass$getTexture()) return;
        targetClouds.glass$setTexture(sourceClouds.glass$getTexture());
        target.getCloudRenderer().markForRebuild();
    }

    private void closeTransparencyChain() {
        if (transparencyChain != null) {
            transparencyChain.close();
            transparencyChain = null;
        }
    }

    @Override
    public void close() {
        if (renderer != null && level != null) renderer.setLevel(null);
        environmentProbe.reset();
        level = null;
        renderState = null;
        projectionBuffer.close();
        closeTransparencyChain();
        postChainProjectionBuffer.close();
        channelGlobals.close();
        mainGlobals.close();
        if (featureDispatcher != null) {
            featureDispatcher.close();
            featureDispatcher = null;
        }
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
