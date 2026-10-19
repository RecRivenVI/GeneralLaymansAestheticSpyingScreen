# 旧 port 当前映射与 Target 接线审计

审计视角：当前实际入口、五个 Target、资源连通和反向来源。审计日期：2026-09-10；只读静态研究，未启动游戏、未构建、未执行产品测试。

## 证据边界

- 当前结论绑定 D:\Workspaces\Artifacts\GLASSPortAudit\20260910\SourceSnapshot 和 manifest-start.json SHA-256 81a3b04fab8f0e47572e376f09f59a90f53a0744605e13bd64ff428d1cfa5019。快照包含选定的 210 个现存文件；run/、instances/、validations/*/instance、缓存、本机配置和私密资料不在范围内。
- 当前仓库 HEAD 是 da59323c918c4bc2a928bce6d2dca860db26e881、分支 stonecutter；迁移工作树有大量删除/未跟踪变化。下文“当前”均指快照路径，不把这些路径或结论归属于 HEAD。
- 原始 Forge 基线使用独立上游 99d9b4170a64d3692b46100d9d3e63cb41c735ee；初始现代 port 使用本仓库 0f33e54b45b37692c1fbf1fbc127ff46f368feea。commit 后的当前身份/路径迁移来自 da59323c 及其未提交工作树。文件内容相似度只用于定位，不能证明行为等价。
- 状态词：配置已定位只表示发现配置；静态对应已确认只表示入口/代码/资源有静态连接；定位但接入未确认表示符号存在但没有足够接线证据；行为待动态验证不由本审计解除。

## 原始 Forge 1.12.2 基线

上游 99d9b417 的 build.gradle 使用 ForgeGradle 2.3-SNAPSHOT、Minecraft 1.12.2-14.23.0.2493、Java 8、版本 1.12.2-7.0.0，运行目录为 run/，并要求本地未锁定版本的 iChunUtil deobf JAR。src/main/java/me/ichun/mods/glass/common/GeneralLaymansAestheticSpyingScreen.java 是 @Mod/@SidedProxy 主类；ProxyCommon.preInit() 调用 WorldPortals.init()、注册四类 TileEntity 并创建 iChunUtil PacketChannel；EventHandlerServer 通过 Forge 注册事件创建两个 Block、两个 Item 和环境声。客户端由 ProxyClient/EventHandlerClient 注册模型、世界卸载、GUI、视锥和 WorldPortals 渲染。原始资源 namespace 是 generallaymansaestheticspyingscreen，旧 recipe 位于 assets/.../recipes/。

这条基线只有一个 Forge 1.12.2 产品入口，不等于当前五个 Target 的支持声明。原始代码中可直接追到的主能力是：BlockGlass/TileEntityGlassBase 的面传播，TileEntityGlassMaster 的 Projector/无线位置，TileEntityGlassTerminal 的频道锚点，三个 Packet*，八个 Gui*，两个 TileEntity renderer，以及 SoundGlassAmbience。

## 当前五个 Target 与构建产物链

当前 settings.gradle.kts 明确登记且只登记以下五个 <minecraft>-<loader> 节点：1.20.1-forge、1.21.1-neoforge、26.1.2-neoforge、26.2-fabric、26.2-neoforge；versions/current 内容是 26.2-fabric。Stonecutter 控制器固定 active 26.2-fabric，使用 0.9.7；constants 只匹配 fabric、forge、neoforge，本快照中没有 //? 条件源码，所有产品 Java 实际集中在 Fabric Target 自己的 src/。

| 目标 | Facts | 源码状态 | 接线证据 |
|---|---|---|---|
| 1.20.1-forge | planned；Java 17；Forge 1.20.1-47.4.23；legacyforge 2.0.140 | versions/1.20.1-forge/src/ 不存在；仅 build.gradle.kts、target.properties | legacyforge client/clientMultiplayer/server 已配置；目标描述、运行和产物细节见下方 |
| 1.21.1-neoforge | planned；Java 21；NeoForge 21.1.248；moddev 2.0.140 | versions/1.21.1-neoforge/src/ 不存在；仅 build/Facts | NeoForge 三类 run 已配置；目标描述、运行和产物细节见下方 |
| 26.1.2-neoforge | planned；Java 25；NeoForge 26.1.2.97 | versions/26.1.2-neoforge/src/ 不存在；仅 build/Facts | NeoForge 三类 run 已配置；目标描述、运行和产物细节见下方 |
| 26.2-fabric | implemented；Java 25；Fabric Loader 0.19.3、Fabric API 0.156.0+26.2、Mod Menu 20.0.1（modCompileOnly） | 85 main Java、43 resources、36 tests，共 164 个本 Target 源/测试/资源文件 | fabric.mod.json 三个 entrypoint、13 个 client Mixin、验证 profile 运行入口；目标描述、运行和产物细节见下方 |
| 26.2-neoforge | planned；Java 25；NeoForge 26.2.0.66 | versions/26.2-neoforge/src/ 不存在；仅 build/Facts | NeoForge 三类 run 已配置；目标描述、运行和产物细节见下方 |

五个 Target 的 planned 保护来自 settings.gradle.kts：除维护任务外，对 :version:<planned>:build/check/test/assemble/jar/sourcesJar/buildAndCollect/runClient/runClientMultiplayer/runServer 直接报错，发生在源码生成、编译、Loader 依赖解析、打包和运行前。目标 build 脚本的 run/产物定义是接线意图，不是已实现入口。

### 1.20.1-forge

build.gradle.kts 使用 net.neoforged.moddev.legacyforge 2.0.140，运行目录为 instances/1.20.1-forge/{client,client-multiplayer,server}，processResources 只展开 META-INF/mods.toml，buildAndCollect 依赖 reobfJar/sourcesJar。由于 target.status=planned，settings 在 Loader 解析前阻断业务任务；本 Target 没有 Java、通用资源或目标 mods.toml，不能视为 Forge 支持。

### 1.21.1-neoforge

build.gradle.kts 使用 net.neoforged.moddev 2.0.140，运行目录为 instances/1.21.1-neoforge/*，processResources 只展开 META-INF/neoforge.mods.toml，buildAndCollect 从 jar/sourcesJar 收集。由于 planned 保护，本 Target 没有源码和目标描述资源，运行/打包入口不可达。

### 26.1.2-neoforge

build.gradle.kts 使用 net.neoforged.moddev 2.0.140，运行目录为 instances/26.1.2-neoforge/*，只展开 NeoForge 描述，buildAndCollect 已定义但不可达。本 Target 没有源码、资源或入口描述。

### 26.2-fabric

build.gradle.kts 的 run profile 默认 S2；PRIMARY 取请求 profile，SECONDARY 在请求 M2 时取 M2，否则取 M1，SERVER 固定 M2。profile 要求 node、role、profile、gameDirectory、modsDirectory=gameDirectory/mods 和客户端 2560x1440；runClient/runClientMultiplayer 追加 --graphicsBackend VULKAN --width 2560 --height 1440，可选 --quickPlaySingleplayer <world>。buildAndCollect 收集 loomx.modJar 和 loomx.modSourcesJar 到 build/libs/8.0.0-alpha.1；根 testAll/checkAll/buildAll/collectArtifacts 只依赖该 Target。当前只确认静态入口，产品行为、视觉和跨 Target parity 仍待动态验证。

### 26.2-neoforge

build.gradle.kts 使用 net.neoforged.moddev 2.0.140，运行目录为 instances/26.2-neoforge/*，只展开 NeoForge 描述，buildAndCollect 已定义但 settings 阶段不可达。本 Target 没有源码和目标描述资源，不能视为 NeoForge 支持。

上述 Fabric profile/产物描述只证明配置入口，未启动运行。

## Fabric 26.2 的真实注册与资源入口

versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/GlassMod.java 实现 Fabric ModInitializer：加载 GlassConfig，调用 GlassContent.initialize()、GlassNetworking.initialize()，注册 server started/stopping、chunk load/unload、END_SERVER_TICK 到 GlassServerState。GlassClient.java 实现 ClientModInitializer：初始化 GlassClientInteraction，在 END_CLIENT_TICK 更新 GlassRenderSnapshots，在 LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN 调 VulkanBlaze3DViewRenderer.drawProjector，断开清理状态并在客户端停止时关闭 renderer。GlassContent 静态注册四个 Block/BlockItem（glass_terminal、glass_projector、glass_extension、glass_wireless_extension）和 glass:ambience SoundEvent；创造模式红石 tab 入口在 initialize()。

fabric.mod.json 的 entrypoint 是唯一被 Loader 发现的 main/client/modmenu 入口；glass.mixins.json 是 client-only Mixin 清单。快照虽在 Fabric 源资源中保留 META-INF/mods.toml 与 META-INF/neoforge.mods.toml，但 Fabric build 的 sourceSets.main.resources.exclude 会排除这两个描述。反之，Forge/NeoForge build 脚本只在它们各自的 src/main/resources 中展开对应描述；这些 planned Target 的 source 目录当前不存在，因此不存在可达的对应 Loader 入口。

### 资源连通静态检查

当前资源根是 versions/26.2-fabric/src/main/resources/assets/glass 与 data/glass，不是原始 assets/generallaymansaestheticspyingscreen。快照中发现：4 个 blockstate、4 个 item definition、8 个 block model、4 个 item model、2 个 shader、sounds.json+sounds/amb.ogg、4 个 recipe、4 个 loot table、2 个 Minecraft block tag、英文/简体中文 lang。逐个读取 JSON 并解析引用后，36 个 JSON/MCMeta 文件解析错误为 0，54 个 blockstate/item model 引用和 4 个 recipe result 均连通，所有 glass:block/*/glass:item/* 模型均有对应文件，recipe result 均落到 GlassContent 注册的四个 ID，sounds.json 的 glass:amb 有 amb.ogg，shader 由 GlassProjectionCompositor 的 GlassContent.id("core/glass_projective") 指向 assets/glass/shaders/core/glass_projective.{vsh,fsh}；fabric.mod.json 的 3 个 entrypoint 均有 Java 文件，glass.mixins.json 的 13 个 client Mixin 均有 Java 文件；固定 Component.translatable 字面量 46 个在 en_us.json 中均存在，资源引用错误数为 0。代码中的动态 glass.config. 前缀不应按完整字面量误报缺失 key。

资源和注册存在并不证明游戏能加载它们：Fabric 入口、Mixin 应用、GPU pipeline、声音播放和模型状态仍需真实客户端验证；planned Target 没有同等资源输入。

## 来源判定与历史接线

来源不是按类名相似度单独判断。对 99d9b417→0f33e54 的小范围历史检查显示：原始 common/client Forge 文件被删除，现代 port 的 GlassMod、GlassClient、GlassContent、GlassServerState、GlassStateSnapshot、GlassNetworking、连接方块、Vulkan renderer、Mixin、现代 Screen 和 data resources 作为新路径成组加入；这证明它们是初始 port 的实现输入，而非上游仓库遗漏的未移植文件。0f33e54→da59323 的 rename 记录又显示这些现代类从 versions/fabric-26.2/src 与旧资源 namespace 搬到新身份 root src/io.github.recrivenvi 和 assets/glass；当前快照把同一身份的产品源码落在 versions/26.2-fabric/src。相应地，本账本把“语义对应”与“实现来源”分开：上游给出行为锚点，Fabric API/Blaze3D 是 Loader/版本适配，State/Network/GUI/Vulkan/data 是 port 主动新增；没有独立提交或用户记录支撑的具体设计动机才标为未知。

## U01–U13 双向映射首批账本

上游路径均带 99d9b417；当前路径均带 26.2-fabric 快照。0f33e54 是初始现代 port 的直接来源定位，da59323c 是当前身份/路径迁移的历史定位。先给紧凑索引，再在编号下保留符号、触发、原因、风险和下一步证据。

| 编号 | 对应概要 | 当前变化与状态 | 证据节 |
|---|---|---|---|
| U01 | 注册、内容、Loader entrypoint | Forge event → Fabric initializer/Registry；静态对应已确认 | U01 |
| U02 | 频道、权限、serverbound/clientbound | TE/PacketChannel → SavedData/payload；部分实现，需复核规则分叉 | U02 |
| U03 | 终端、开屏、频道编辑 | TE/block GUI → BlockState/server-open Screen；静态对应已确认 | U03 |
| U04 | 投影面、拓扑、传播 | TE 面状态 → server BFS/snapshot；静态对应已确认 | U04 |
| U05 | 有线扩展、连接/支撑形状 | 现代六向 BlockState/model；静态对应已确认 | U05 |
| U06 | 无线扩展、顺序、三角带 | GlobalPos order + payload/mesh；静态对应已确认 | U06 |
| U07 | GUI、排序、空间预览 | Forge GUI → Fabric Screen/preview；静态对应已确认 | U07 |
| U08 | 主目标合成、远端捕获、裁切 | WorldPortals/OpenGL → Vulkan/Blaze3D/Mixin；静态对应已确认，行为待动态 | U08 |
| U09 | 远端世界、相机、跨维度边界 | 选择可保存、捕获静态不可达；当前能力边界不一致 | U09 |
| U10 | 持久化、同步、网络快照 | SavedData 新格式静态确认；旧 TE NBT 兼容确认缺失 | U10 |
| U11 | 配置、限制、生命周期 | iChunUtil/Forge hooks → JSON/clamp/Fabric lifecycle；静态对应已确认 | U11 |
| U12 | blockstate/model/item/sound/recipe/lang/shader | 现代 data/resource IDs；静态引用错误 0，行为待动态 | U12 |
| U13 | iChunUtil/WorldPortals 外部边界 | 外部依赖由 GLASS 自有替代实现承接；静态接入已确认，替代语义待动态 | U13 |

### U01 注册与内容

上游 99d9b417:src/main/java/me/ichun/mods/glass/common/GeneralLaymansAestheticSpyingScreen.java、common/core/ProxyCommon.java、common/core/EventHandlerServer.java 是 Forge @Mod/@SidedProxy、WorldPortals/init、TileEntity/PacketChannel 和 RegistryEvent 入口。当前快照的 versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/GlassMod.java:20-29 由 Fabric mod initializer 依次加载 Config/Content/Networking 并挂 server started/stopping、chunk load/unload、END_SERVER_TICK；GlassContent.java:24-52,56-90 注册四个 Block/BlockItem 与声音，fabric.mod.json main 指向 GlassMod。变化是 Forge event/外部代理到 Fabric initializer/现代 Registry；原因明确来自 0f33e54 的 upstream-behavior 记录和当前 AGENTS 的身份/版本约束。状态为静态对应已确认；其他四 Target 没有源码入口，Loader 实际加载待动态验证。

### U02 频道与权限

上游 99d9b417:src/main/java/me/ichun/mods/glass/common/tileentity/TileEntityGlassTerminal.java、TileEntityGlassMaster.java、common/packet/PacketSetChannel.java、PacketSetProjector.java 保存频道并通过 iChunUtil PacketChannel 传递。当前 GlassSavedState/GlassStateSnapshot 保存 UUID、name、owner、public；GlassNetworking.java:14-52 注册 2 个 clientbound/4 个 serverbound payload，GlassServerState.java:156-191,402-421 校验玩家距离、目标方块和 owner/public。变化是 TileEntity 字段/PacketChannel 到 SavedData、全局 payload 和服务端校验；原因明确：0f33e54 的 upstream-behavior 要求现代服务端重验权限、距离、角色、区块和列表。状态是部分实现，缺口要具体看：setProjectorChannel 目前有 canInteract/canAccess，但没有独立 active-state 分支；历史 hot-switch 记录使这成为待复核语义，不是已证缺陷。另有 GlassChannelRules.java:5-61 仅被对应测试引用，生产路径使用另一套 UUID/channelNameLength 规则，存在规则漂移风险；没有动态多人证据。

### U03 终端

上游 99d9b417:src/main/java/me/ichun/mods/glass/common/block/BlockGlassTerminal.java、common/tileentity/TileEntityGlassTerminal.java、client/gui/GuiChannelSetter.java 形成终端方块实体和 GUI。当前 GlassChannelBlock.java:71-76 的 useWithoutItem 在服务端调用 GlassNetworking.openScreen；GlassNetworking.java:55-60 先发 state 再发 OpenGlassScreenPayload；GlassClientInteraction.java:35-49 按 GlobalPos dimension 和 block state 打开 TerminalChannelScreen。变化是方块实体终端到 BlockState、global state 和 server-open Fabric Screen；原因由当前代码注释和上游对应路径直接支持。状态为静态对应已确认；输入、交互和屏幕往返待动态验证。

### U04 投影与拓扑

上游 99d9b417:src/main/java/me/ichun/mods/glass/common/block/BlockGlass.java、common/tileentity/TileEntityGlassBase.java、TileEntityGlassMaster.java 通过 activeFaces/fade/无线位置驱动传播。当前实际产品入口是 GlassTopologySolver.java:18-86 与 GlassServerState.java:287-312，再由 GlassRenderSnapshots.java:44-80 转成逐面 render state；solver 从 projector face 遍历同方块转角、共边和对角，并过滤不外露面。GlassPropagationRules.java 当前只被测试引用，不列为产品入口。变化是 TileEntity 面状态到服务端集中 BFS 和 client snapshot；原因明确来自 upstream-behavior 的“两 tick 一步/外露面图/实时投影”记录以及类注释。状态为静态对应已确认；行为待动态验证，主任务需复核 projector 被遮挡时虚拟根路径。

### U05 有线扩展

上游 99d9b417:src/main/java/me/ichun/mods/glass/common/block/BlockGlass.java 与 TileEntityGlassBase 的 activeFaces/fadePropagate 对应当前 GlassExtensionBlock.java、GlassConnectedBlock.java、GlassAttachmentSupport.java，以及 assets/glass/blockstates/glass_extension.json 的 multipart。GlassConnectedBlock.java:47-76,102-168 维护六向 wired/support 连接并预计算 64 个形状。变化是旧共享 Block/TE 到现代六向 BlockState 和支撑模型；原因明确：port 记录允许现代模型与外露面图，当前 block 注释也拆分连接/支撑职责。状态为静态对应已确认；真实方块状态、邻居更新和 64 状态 model parity 待动态验证。

### U06 无线扩展

上游 99d9b417:src/main/java/me/ichun/mods/glass/common/tileentity/TileEntityGlassWireless.java、TileEntityGlassMaster.java、common/packet/PacketWirelessOrder.java、client/gui/GuiWirelessOrder.java 对应当前 GlassWirelessExtensionBlock.java、AttachWirelessPayload.java、SetWirelessOrderPayload.java、GlassStateSnapshot.Projector.wirelessPositions 和 renderer wireless transitions。GlassServerState.java:194-231 校验同维度、方块、最大列表和重复；GlassRenderSnapshots.java:59-80 保留顺序；GlassProjectionCompositor.java:35-38 建 wireless triangle pipeline。变化是 TE 列表到 GlobalPos 有序列表、serverbound attach/order 和三角 mesh；原因明确来自 port 记录“顺序决定三角带且使用实时投影”。状态为静态对应已确认；多人、卸载、排序 GUI 和实时画面待动态验证。

### U07 GUI 与交互

上游 99d9b417:src/main/java/me/ichun/mods/glass/client/gui/GuiChannelList.java、GuiChannelSetter*.java、GuiProjector*.java、GuiWireless*.java 是旧 GUI 操作入口。当前 GlassClientInteraction.java:18-49 路由四个 Fabric Screen：ProjectorChannelScreen、TerminalChannelScreen、WirelessProjectorScreen、WirelessOrderScreen，并由 GlassConfigScreen/GlassWorldPreviewWidget 承担配置和空间预览；各 Screen 发送对应 payload。GlassNodeDistance.java:14-24 按直线距离/维度/坐标排序、43-53 标记可能未加载；GlassNodeList.java:87-165 处理点击/滚动条；GlassWorldPreviewWidget.java:140-219 处理左/右/中键拖动、缩放、复位、三角面和逐块 visible progress。变化是 Forge GUI 到 Fabric Screen/preview；原因明确：旧任务 JSONL 019ff1d0… log line 41322 记录用户要求排序、未加载提示、滚动条/鼠标行为、统一按钮和可拖拽缩放旋转的 3D 真实世界轮廓、三角广播预览、逐块播放；0f33e54 upstream-behavior 也允许独立 GUI。状态为静态对应已确认；视觉和输入行为待动态验证。

### U08 渲染

上游 99d9b417:src/main/java/me/ichun/mods/glass/client/render/TileEntityGlassRenderer.java、TileEntityGlassTerminalRenderer.java 直接调用 WorldPortals/OpenGL。当前 GlassClient.java:22 在 AFTER_TRANSLUCENT_TERRAIN 提交 VulkanBlaze3DViewRenderer；其 :54-67 维护每频道 view/context，GlassChannelCaptureContext.java:121-158 建独立 LevelRenderer/Extractor/renderState/targets 并同步 section sets，但 ensure:101-108 直接复用当前 client.level 和已加载 chunk，没有为每频道创建独立 ClientLevel/world；GlassProjectionCompositor.java:104-117 注册 RenderPipeline/shader，13 个 Mixin 由 glass.mixins.json 接线。变化是 TileEntitySpecialRenderer/WorldPortals 到 Blaze3D/Vulkan RenderTarget、extracted LevelRenderer 和 main-target composite；原因是初始 port 记录允许现代 Vulkan 资源管理且 renderer 类注释说明它是 iChunUtil stencil renderer 的 translation。历史用户约束另要求身份迁移不得再替换渲染技术栈；当前快照只是继承并搬运 0f33/da593 已存在的 Vulkan 实现。状态为静态对应已确认；GPU/视觉行为待动态验证，风险集中在 Mixin、SectionOcclusion、共享 client.level/加载区块边界和主目标状态隔离。

### U09 跨维度与远端世界

上游 99d9b417:src/main/java/me/ichun/mods/glass/common/block/TerminalPlacement.java 和 TileEntityGlassRenderer.drawScene 在同一个 World 中保存 master/terminal BlockPos，并交给 WorldPortals。当前 GlassStateSnapshot.Network 的 terminal/projector 都是 GlobalPos，GlassServerState.setProjectorChannel 可从全局 terminals 找频道并保存不同 dimension 的 pair；GUI/server 因此能静态保存跨维度选择。渲染链却有确定边界：GlassRenderSnapshots.java:35-37 只为当前 client level dimension 的 projector 生成 views，GlassChannelCaptureContext.java:95-108 要求 terminalDimension 等于 requestedLevel.dimension 且 terminal chunk 可用；跨维度 projector/terminal pair 同时不能满足这两个条件，capture 分支直接返回 false，显示不可达。变化是同 World BlockPos 到可保存 GlobalPos，但新增选择能力与渲染能力不一致；原因来自当前 GlobalPos 数据设计，未发现把跨维度显示声明为已支持的独立记录。状态为选择可保存、捕获静态不可达；当前能力边界不一致。若后续用户选择跨维度显示范围，再单独明确限制或捕获方案；本记录不把该边界归为原版跨维度遗漏，也不以动态待验证掩盖静态不可达分支。

### U10 状态、持久化与网络

上游 99d9b417 的四类 TileEntity readFromNBT/writeToNBT 与 iChunUtil PacketChannel 对应当前 GlassSavedState.java、GlassStateSnapshot.java、GlassStatePayload.java 和 JOIN sync。GlassSavedState.java:17-26,79-97 使用 SavedDataType glass:state 并序列化 terminals/projectors；GlassNetworking.java:51-59 在玩家加入时发快照，GlassClientState 只读替换并在断开清空。变化是方块实体 NBT 到世界级 SavedData/snapshot payload；原因明确：GlassSavedState:17 写明旧方块实体 NBT 不再读取。状态要拆开记录：新格式的静态对应已确认；旧 TE NBT 迁移兼容是确认缺失，不能把新格式存在误写为旧存档兼容；JOIN 同步行为待动态验证。

### U11 配置、限制与生命周期

上游 99d9b417 的 iChunUtil GUI_CONFIG_FACTORY、Forge preInit/update、TileEntity tick/world unload 对应当前 GlassConfig.java、GlassConfigScreen.java、GlassMod.java server lifecycle/chunk/tick 和 GlassClient.java disconnect/stop。GlassConfig.java:16-38,59-167,232-253 提供默认值、范围、clamp 和 JSON 持久化；GlassMod.java:23-29 接入 server lifecycle/chunk/tick；GlassClient.java:23-28 清理 renderer。变化是外部配置/Forge hooks 到本模组 JSON、限制和 Fabric lifecycle；原因由 port 记录允许配置与现代生命周期、当前代码“服务端权威”注释支持。状态为静态对应已确认；配置界面保存/重载和生命周期动态行为待验证。

### U12 资源、配方与声音

上游 99d9b417 的 assets/generallaymansaestheticspyingscreen/*、旧 recipes/*、sounds.json/amb.ogg 对应当前 assets/glass/*、data/glass/recipe/*、loot/tag/lang/shader、GlassContent.AMBIENCE 和 GlassAmbienceSound。快照静态计数为 4 blockstates、4 item definitions、8 block models、4 item models、2 shaders、4 recipes、4 loot tables、2 Minecraft tags、双语 lang 和 sounds.json+amb.ogg；逐个解析 blockstate/item model、recipe result、sound file 和 shader ID 后引用错误为 0。变化是 Forge 1.12 asset/recipe/registry 到现代 data pack/resource IDs；原因明确是当前身份约束和 da59323 resource rename，port 记录允许现代模型/GUI。状态为静态对应已确认；模型状态、recipe 可取、声音播放和 shader 编译待动态验证。

### U13 iChunUtil/WorldPortals 外部边界

上游 99d9b417 的 ProxyCommon、EventHandlerClient、TileEntityGlassRenderer 直接 import iChunUtil PacketChannel、WorldPortals 和 EntityTransformationStack。当前 main Java 没有这些外部 import；compat/package-info.java 只声明 GLASS 自有可选兼容层，GlassPortalTransform、GlassObliqueProjection、Vulkan renderer 是本仓库的替代实现，且 VulkanBlaze3DViewRenderer:695-702/890 实际调用它们。变化是外部强依赖到 GLASS 自有替代实现和 Loader API 适配；原因明确：当前 compat 约束禁止外部重编译/重分发，GlassPortalTransform 注释说明其参考 iChunUtil QuaternionFormula。这里不能把“替代”写成“等价”：iChunUtil 5cf 与当前数值/渲染链差异已由主审 M06/M07 记录，仍需按动态行为验收。状态为静态接入已确认；行为等价、外部兼容待动态验证。

## 反向当前新增 N01 起

以下是从原始 99d9 与初始 port 代码/历史反向看到的当前内容。N 表示当前设计中无法归入单一原始类的新增或现代化接线；“主动新增”表示当前源码/开发记录明确选择了该设计，不表示用户已经动态验收。

| N | 当前新增概要 | 分类与状态 | 直接证据、原因、风险与下一步 |
|---|---|---|---|
| N01 | 身份/命名空间：GlassMod.MOD_ID="glass"、io.github.recrivenvi.glass、assets/glass/data/glass、8.0.0-alpha.1 | 主动新增 + Loader/路径迁移；静态对应已确认 | gradle.properties、GlassMod:14-16、fabric.mod.json、资源路径；git diff 0f33..da593 显示 namespace/resource rename。当前 AGENTS 固定身份；旧任务 JSONL 019ff1d0… 约 line 45800 明确允许 package/namespace/group/artifact identity 迁移，同时要求不替换渲染技术栈/引入产品架构。旧资源/旧存档 ID 兼容缺证；身份迁移授权是历史原因证据，不是本轮发布授权 |
| N02 | 五节点 Stonecutter/Gradle、target Facts/build scripts、planned 阻断、root aggregate | 主动新增 + 版本/Loader 适配；静态对应已确认 | settings 目标清单、versions/*/target.properties、五 build 文件、root build.gradle.kts；当前 AGENTS 要求五 Target、只实现 Fabric、Stonecutter 0.9.7。planned 节点无源码/资源，不能宣称四版本支持 |
| N03 | GlassSavedState/GlassStateSnapshot 世界级状态、UUID channel、GlobalPos、wireless order、revision、Settings | 主动新增；新格式静态对应已确认，旧格式确认缺失 | SavedDataType glass:state、NBT to/from、server snapshot；GlassSavedState:17 说明现代格式与旧 TE NBT 断点。迁移/旧存档风险高 |
| N04 | 服务端权限/距离/方块/区块/列表校验与 JOIN 全量同步 | 主动新增；部分实现 | GlassServerState:156-231,247-341,402-437 与 GlassNetworking:51-59；port upstream-behavior 要求 authoritative revalidation。需复核 setProjectorChannel active/owner 语义与 U09 跨维度边界 |
| N05 | 现代 Block/BlockState 连接体系、GlassAttachmentSupport、64 缓存 shape、Projector POWERED/FACING | 主动新增 + Minecraft API 适配；静态对应已确认 | block classes 与四 blockstate multipart；上游只有 BlockGlass/TE 状态，当前类注释拆分物理/连接/状态。形状、可附着性、红石更新待动态验证 |
| N06 | 独立 renderer/extractor/renderState/targets 的 Vulkan 管线、13 Mixin、section/cloud/particle isolation | 初始 port 主动新增 + 版本/Loader 适配；当前迁移沿用；静态接入已确认 | 0f33 直接新增约 2,800 行，da59323 高相似度 rename，快照只改变落点/namespace；AFTER_TRANSLUCENT_TERRAIN、每频道 renderer/extractor/renderState/targets、RenderPipelines/shader 直接接线，但 ensure 复用当前 client.level/已加载 chunk，并非每频道独立 ClientLevel。初始 port 文档允许 Vulkan，但旧任务约 line 45800 随后禁止身份迁移再替换渲染栈；行为待动态验证，属于最大风险簇 |
| N07 | GUI/空间预览/配置面板、排序/未加载提示/鼠标滚动/3D 三角预览 | 主动新增；静态对应已确认 | GlassNodeDistance:14-24,43-53、GlassNodeList:87-165、GlassWorldPreviewWidget:140-219、WirelessOrderScreen:98-218。旧任务 log line 41322 明确用户需求，故不是原版遗漏/未知来源；视觉与输入待动态验证 |
| N08 | data recipe/loot/tags、blockstate multipart、support models、glass.mixins.json、shader pair | 主动新增 + 资源迁移；静态对应已确认 | 资源连通检查错误 0，GlassContent IDs 与 recipes 对齐；当前身份/迁移历史与原始 recipes/namespace 不同。真实加载待动态验证 |
| N09 | 渲染状态/动画分层、closing pairs、channel switch/wireless transitions | 主动新增；静态对应已确认 | renderer replaceViews 与 Settings.legacyFadeAnimation；port 文档允许可配置动画但要求实时捕获。动画时序/热切换待动态验证 |
| N10 | 源码从 HEAD root src/ 落到 versions/26.2-fabric/src/，planned Target 未复制通用源码 | 主动新增 + 结构迁移；静态对应已确认 | 快照只有 Fabric src，四 planned src 全缺，manifest 绑定未提交工作树；AGENTS 要求 target 自有 src/并只实现 Fabric。结束时需与 manifest 复比 |
| N11 | 并行但未接入的 GlassChannelRules（MAX_NAME_LENGTH=15、owner:name）和 GlassPropagationRules | 来源未知 / 生产不可达的旧辅助实现（测试辅助仍可执行） | rg 显示生产调用为 0，GlassServerState 使用另一套 sanitizeName/BFS；两类由 0f33 加入但动机未知、无记录解释保留原因。测试通过不能证明生产使用同一规则；后续范围由用户选择 |

## 主审闭合与后续

- 已闭合的静态范围：五 Target 的 settings/Facts/build/source presence、Fabric main/client/Mixin 入口、Loader 描述选择、资源打包排除/展开、artifact collect chain、U01–U13 双向路径和 N01–N11 反向新增。统一闭合索引见主文 M01–M17 及其风险表，本文件保留本视角的路径与接线证据。
- 仍待实际动态：真实客户端/服务器、模型与 shader 实际加载、GUI 输入、无线排序运行、区块卸载、旧存档迁移和跨 Loader parity；本审计按边界未启动或构建，静态闭合不替代动态结果。
- 高风险已在主文风险表登记：U02 setProjectorChannel 的 active/权限语义与 GlassChannelRules/GlassServerState 规则漂移；U04/U08 的投影拓扑、遮挡和 SectionOcclusionGraph；U09 选择可保存而捕获不可达的边界；U10 旧 NBT 断点；U12 实际资源加载；U13 替代实现相对 iChunUtil/WorldPortals 的语义满足度。后续处理范围由用户选择。
