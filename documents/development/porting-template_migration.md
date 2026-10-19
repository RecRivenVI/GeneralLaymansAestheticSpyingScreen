# GLASS 构建与 API 迁移

2026-10-19 的迁移以签名检查点 `2dd51af4d4b41756d9a89312d7a16d9f7afa8af4` 为基线，在 `develop` 工作树进行。版本保持 `8.0.0-alpha.1`；迁移改动不提交、不推送。五端已补齐产品实现，参考端的本轮基本运行证据已保存。构建门禁结果以迁移验证报告为准；用户视觉验收仍待完成。

## 目录对照

| 检查点位置 | 当前位置或处置 |
| --- | --- |
| `versions/26.2-fabric/src/main` | `versions/26.3-fabric/src/main` 与 `src/client` |
| 原计划中的 NeoForge 26.2 | `versions/26.3-neoforge`，迁入产品并适配 NeoForge |
| `buildSrc`、`eng`、根构建与 Stonecutter 控制器 | 完整备份后送回收站；构建由模板插件接管 |
| `eng/runtime` 的 Profile 运行管理 | `components/runtime_validation` 与模板实例配置 |
| 四个 kebab-case 验证项目 | 四个 snake_case 主题验证项目，原配置保存于各自 `result/migration_20261019` |
| `instances/26.2-fabric/client` | `instances/26.3-fabric/client`，逐文件迁移，不打开旧世界 |
| 验证实例中的 `26.2-fabric` | 相应验证的 `instance/26.3-fabric`，历史数据保留 |
| `run` 中剩余历史诊断日志 | `validations/migration-template_layout/result/legacy_runtime` |
| `documents/development/audit-port-*.md` | `documents/research/audit-port_*.md`，历史结论保留 |
| 首次布局迁移说明 | [历史布局迁移](porting-repository_layout.md) |
| `build/migration-20260909` | 原位保护，不作为普通缓存清理 |

备份包含 `git bundle --all` 和整个工作树的忽略文件副本；Git 图、refs、本机 Git 配置与逐文件哈希均已保存。

## API 适配

| 26.2 | 26.3 |
| --- | --- |
| 方块 `MapCodec` 和 `simpleCodec` | 原版移除了方块 codec 入口，删除对应覆盖 |
| Blaze3D GPU 类型 | 对应 RenderPearl API 包与 `CompiledRenderPipeline` |
| GLFW 键码 | `InputConstants` 的 SDL 逻辑键码与鼠标按键常量 |
| 布尔深度附件的 `TextureTarget` | 显式颜色 `RGBA8_UNORM` 与深度 `D32_FLOAT` |
| 透明排序 PostChain | 原版改为 OIT；频道渲染器继续独立持有 GPU 目标与状态 |
| Fabric 透明地形回调内另建 pass | 在 `LevelRenderer.render` 返回后、3D HUD 之前合成，避免在活动 pass 内上传或创建嵌套 pass |
| 摄像机与实体可见性旧参数 | 传入 `DeltaTracker`、实体 partial tick 与区块淡入时长 |
| NeoForge 内容与网络 | DeferredRegister、载荷注册事件和客户端独立 handler |

这些是构建或 API 适配，频道、权限、拓扑、无线传播和持久化规则保持。合成时点移动后与天气及透明物的组合顺序仍待运行和视觉确认，不能声明显示等价。

1.20.1 Forge 使用 SimpleChannel、FriendlyByteBuf、旧 SavedData 序列化、复数 datapack 目录、原生 BufferBuilder 与带 JSON 描述的 ShaderInstance。1.21.1 NeoForge 使用原生 payload 注册、ResourceLocation、Factory SavedData 与单数 datapack 目录。两端的列表、预览交互和配置页按各自 GuiGraphics 和输入回调实现。

26.1.2 使用原生 `LevelRenderer.extractLevel`、自有 GPU 批次和 forward-Z 投影；26.3 使用 `LevelExtractor`、RenderPearl 与 reversed-Z。两个深度约定均有数学回归断言。用户已明确三个旧端也需本轮补齐完整功能。

## 审计缺陷的当前位置

1.20.1 Forge 与 1.21.1 NeoForge 使用 `Blaze3DViewRenderer`、原生独立 `LevelRenderer` 和经典 ShaderInstance/FBO 合成；26.1.2 与 26.3 使用 `VulkanBlaze3DViewRenderer`。旧版没有原生 Vulkan，不能把五端构建称作五端 Vulkan 运行证据。

以下编号沿用[历史审计](../research/audit-port_status.md)。当前 Fabric 客户端源码根为 `versions/26.3-fabric/src/client/java/io/github/recrivenvi/glass`；Fabric 服务端为同 Target 的 `src/main/java`，NeoForge 均在对应 Target 的 `src/main/java`。

| 审计编号 | 当前文件与机制 | 本轮处理 |
| --- | --- | --- |
| R01 | `state/GlassServerState.java` 全量状态广播 | 保留，私有频道可见性合同待决 |
| R02 | `GlassRenderSnapshots.java` 与 `VulkanBlaze3DViewRenderer.java` 多频道同面提交 | 未修复 |
| R03 | `GlassChannelCaptureContext.java` 同维度和区块前提 | 未修复 |
| R04 | `components/projection_math` 中 `model/GlassProjectionAnimation.java` 频道切换 | 保留差异 |
| R05 | `GlassConfig.java` 与 `GlassConnectedBlock.java` WIRED 含义 | 待决 |
| R06 | 交互和动画链，无左键暂时白屏路径 | 未修复 |
| R07 | `GlassSavedState.java`，无旧身份 NBT 转换 | 未修复 |
| R08 | `assets/glass/models/item` 与 block 资源 | 保留差异 |
| R09 | `GlassPortalTransform.java`、摄像机状态与四元数 | 视觉等价性待验 |
| R10 | `GlassMod.java`，无上游 UpdateChecker 接线 | 未补齐 |
| R11 | `GlassRenderSnapshots.java` 无线节点不足时回退 | 未修复 |
| R12 | `GlassServerState.java` 删除无线节点后的反向引用 | 未修复 |
| R13 | `VulkanBlaze3DViewRenderer.java` ambience 配对集合 | 保留，实际可听性待验 |
| R14 | `GlassChannelCaptureContext.java` 失败时保留上一帧 | 未修复 |

## 当前验证

检查点签名、完整备份及 1,623 个实例和历史日志文件的迁移哈希校验通过。当前五端单元测试分别为 1.20.1 Forge 75 项、1.21.1 NeoForge 75 项、26.1.2 NeoForge 77 项、26.3 Fabric 和 NeoForge 各 79 项，均通过。Forge 的普通 JUnit 初始化独立注册表与服务端 side fixture，并为原生网络事件准备监听列表；它没有启动 FML，不能视为加载或网络集成验收。纯数学组件另有 31 项测试。

参考端已在 `smoke-reference_projection` 的独立新世界启动并完成频道配置网络往返、有线和无线画面捕获与合成。记录为 Vulkan、1920×1080、全局声音零、一个活动网络、17 个表面、三个无线节点。频道与主画面的红绿场景标记均有非零像素计数，截图可见合成区域。日志、四张截图与机器检查报告在该验证的 `result/`；这只证明基本捕获和合成，不是用户视觉验收。游戏已正常关闭。

初次启动发现并清理了 26.3 不再使用的 ShaderManager 配置注入，着色器已按 RenderPearl 的 include、location 与 DynamicTransforms 绑定接口适配。五个 Target 的注入成员与调用目标均对照本机实际游戏字节码核对。1.20.1、1.21.1、26.1.2 的游戏运行、其他四个历史场景及多端 parity 本轮未验收，不能由单元测试推断。历史 PASS 仍仅是历史证据。

全套 `spotlessApply`、`check`、严格合规、五端 `build` 与 `verifyRelease` 的本轮结果保存在 `validations/migration-template_layout/result/build_verification.json`；发行产物由模板归集到 `build/release/8.0.0-alpha.1/`。本轮没有发布。

完成迁移门禁后，按用户追加要求接入固定 MCP alpha 快照。在正常 NeoForge 发行安装的单 MCP JAR 实例中完成端点、在线身份与 USER READ；随后使用 GLASS 发行包进入项目独立世界，完成玩家移动、视角、1920×1080 截图、输入释放、保存返回标题及正常退出。完整流程复跑通过，结果位于 [MCP 接入验证](../../validations/integration-mcp_alpha/validation.md)；首次视角捕获情况记录在 [MCP 兼容性清单](debugging-mcp_alpha.md)。这些操作不替代前述 GLASS 内部探针或人工视觉确认。

用户验收预设为 2560×1440、全局声音 50%、音乐零、玩家名 RecRivenVI。Custom Skin Loader 15.1 从 Modrinth 精确匹配；Zume 无 26.3 NeoForge 对应版本，按用户规则跳过。验收状态由同项验证的 `result/acceptance.json` 记录，视觉确认保留为 `PENDING_USER`。

## 后续审阅

迁移前 `Update` 检查点位于 `stonecutter`，迁移工作位于 `develop`。本轮没有改变远程分支策略，也没有推送或为迁移创建提交。以后如何保留上游历史、怎样提交或合并 `develop`，留给用户决定。MCP 接入按用户追加授权完成；九月审计问题按上表保留到下一轮。
