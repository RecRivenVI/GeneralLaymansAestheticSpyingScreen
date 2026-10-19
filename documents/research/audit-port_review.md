# 旧 port 独立反例复核

调查日期：2026-09-10。以下为迁移前审计的原始发现；2026-10-19 仅增加文档格式小节并迁移位置，未重新判定历史结论。

## 问题

核对旧 port GLASS 的移植状态和证据边界。

## 方法

按下列原始记录中的固定基线与只读方法调查。

## 发现

审计单元：C 视角（遗漏、空壳、不可达、资源失联和关键等价反例）。

范围绑定 `D:\Workspaces\Artifacts\GLASSPortAudit\20260910\manifest-start.json`，SHA-256 为 `81a3b04fab8f0e47572e376f09f59a90f53a0744605e13bd64ff428d1cfa5019`；当前工作树中的新路径按 `26.2-fabric` Target 记录，旧根路径删除按迁移差异处理。上游比较锁定 `99d9b4170a64d3692b46100d9d3e63cb41c735ee`，不以最新上游判定遗漏。本文件只记录研究，不改变实现、资源、依赖、构建、运行配置或模块边界。

状态使用既有审计约定：`确认缺失` 只用于已知应承接的能力没有迁移替代或当前入口；实现缺陷、行为差异和合同未知单独标明，不把“存在反例”自动写成缺失。`部分实现` 表示入口存在但关键分支或副作用未闭合；`生产不可达` 表示源码或数据结构存在而没有 main 入口；`静态机制确认；行为待动态验证` 表示只能证明入口和分支对应，不能证明客户端结果。

## 首批具体反例

### R01 / U02、U08：私有频道的已实现权限范围与全量广播

状态：`静态机制确认；保密范围未提供，合同待决`。Target：`26.2-fabric`。

- 上游基线 `99d9b4170a64d3692b46100d9d3e63cb41c735ee` 的 `PacketSetChannel.execute`（`src/main/java/me/ichun/mods/glass/common/packet/PacketSetChannel.java:45-64`）只更新服务器方块实体；基线没有当前新增的 public/private 读侧合同，不能把旧版行为误写成隐私保证。
- 当前 `GlassServerState.rebuild` 将同一个 `GlassStateSnapshot` 发给 `PlayerLookup.all(server)`（`versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/state/GlassServerState.java:247-251`）。`rebuildSnapshot` 把所有终端、投影仪和网络写入快照（`.../GlassServerState.java:260-341`），其中包含频道名称、owner UUID、位置和网络几何；没有按接收玩家生成过滤快照。
- 当前服务端的 `canAccess` 只出现在设置投影频道、添加无线节点和排序无线节点的写入路径（`.../GlassServerState.java:175-231、402-420`）。它没有包围 `sendState`、`rebuild` 或网络构建。
- 客户端 GUI 的列表有 owner/public 过滤（`.../client/gui/ProjectorChannelScreen.java:42-56`、`.../client/gui/WirelessProjectorScreen.java:33-66`），但这是展示过滤，不是数据传输边界；`GlassClientInteraction.open` 对终端位置直接从全量镜像打开 `TerminalChannelScreen`（`.../client/GlassClientInteraction.java:35-49`），未在客户端再次检查 owner。
- 渲染路径从全量 `snapshot.networks()` 生成视图（`.../client/GlassRenderSnapshots.java:23-83`），`findPairs` 和 `drawProjector` 没有 public/private 条件（`.../client/render/VulkanBlaze3DViewRenderer.java:1311-1350、946-995`）。静态事实是读侧接收与渲染都使用全量状态；如果产品合同要求私有元数据或物理画面按 owner 隔离，当前没有承接该合同的过滤机制。当前资料不足以替产品决定这一范围。

直接影响：全量快照事实包含频道名称、owner UUID、终端/投影仪位置和 `Network` 结构；当前已确定的权限范围只覆盖设置投影频道、添加/排序无线节点等写入校验。是否把全量广播视为私有读侧缺陷，取决于尚未明确的产品合同，不能仅凭 GUI 过滤下结论。变化类型为当前 port 新增访问模型与广播粒度不同，原因来源为当前路径直接证据；不是上游遗漏推断。

意图与接受证据：当前代码能证明写入校验和全量广播为什么产生上述状态，但没有找到“私有元数据/物理画面必须按 owner 隔离”的明确接受记录；`docs/reference/upstream-behavior.md` 只记录公开/私有范围和服务端重新校验要求。保密结论保持合同待决。

排查过的替代路径：检索了 `publicChannel`、`owner`、`canAccess` 的所有生产调用，未找到每玩家快照、渲染前授权或网络级过滤；`GlassChannelAccessTest`（`.../src/test/java/io/github/recrivenvi/glass/state/GlassChannelAccessTest.java:22-50`）只覆盖写入辅助函数，不能证明 payload 或 renderer 的隔离。本轮未启动客户端或读取网络包；读侧合同需先由产品确认，再决定动态验收断言。

### R02 / U04、U05、U08：不同频道共享有线面时没有排他占用，同面可重复提交，最终画面待验证

状态：`确认缺失`。Target：`26.2-fabric`。

- 上游基线 `99d9b4170a64d3692b46100d9d3e63cb41c735ee` 的 `TileEntityGlassBase.bePropagatedTo`（`src/main/java/me/ichun/mods/glass/common/tileentity/TileEntityGlassBase.java:186-228`）只有“目标未激活”或“目标已激活且频道相同”时才接受激活；目标已有另一个频道时不会覆盖其 `channel`/`active`。
- 当前 `GlassServerState.rebuildSnapshot` 对每个投影仪独立调用 `GlassTopologySolver.solve`（`versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/state/GlassServerState.java:260-323`）。求解器的 `isSurface` 输入来自 `GlassContent.isSurface`（`.../GlassContent.java:71-73`），只按方块种类判断，不读取频道、owner 或其他网络的占用状态。
- 当前 `GlassTopologySolver.solve`（`.../core/GlassTopologySolver.java:18-86`）没有全局 claim/排他 predicate；每个投影仪都会把同一连通的 `glass_extension`/`glass_projector` 面加入自己的 `surfaces`。`GlassRenderSnapshots.states` 将每个网络的面转换为独立 view（`.../client/GlassRenderSnapshots.java:44-63`），`drawProjector` 再逐网络提交 composite draw（`.../client/render/VulkanBlaze3DViewRenderer.java:970-995`），也没有按面位置和频道去重。

直接反例：在同一朝向、同一有线连通面组件上放置两个投影仪，分别绑定不同频道并同时供电。旧版传播先占用的面不会被另一个频道覆盖；当前服务端会为两个投影仪各自生成包含相同面位置的 network，客户端会对同一面提交两个不同频道的投影，出现频道串画、过度混合或结果依赖 draw 顺序。变化类型为有线传播从“方块实体上的单频道状态”改成“逐网络无状态求解”，原因来源明确为当前求解与绘制入口。

意图与接受证据：`0f33e54b45b37692c1fbf1fbc127ff46f368feea:docs/reference/upstream-behavior.md` 明确记录当前采用外露面图重写，但没有记录跨网络共享面的排他取舍或用户接受；因此“为什么采用逐网络求解”有开发记录，“是否接受不同频道重叠”仍未知。最终串画、覆盖和 draw 顺序只能列为动态可能结果。

排查过的替代路径：检查了 `GlassTopologySolver`、`GlassContent.isSurface`、`GlassRenderSnapshots`、`appendSurfaceDraw` 以及 `GlassProjectionMesh.shouldRenderFace`；后者只判断方块面/邻居遮挡（`.../client/render/GlassProjectionMesh.java:52-57`），没有频道排他。现有拓扑测试覆盖单网络几何和距离，未找到跨网络冲突断言；本轮不运行测试，需动态场景确认最终混合顺序和视觉表现。

### R05 / U04、U05、U06：拓扑求解器输入未接入方块连接状态，合同待决

状态：`静态机制确认；行为与合同待决`。Target：`26.2-fabric`。

- 当前方块状态维护有实际连接谓词：`GlassConnectedBlock.connectionAt` 先调用 `connectsTo`，再区分 `WIRED`、`SUPPORT` 和 `NONE`（`versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/content/block/GlassConnectedBlock.java:131-139`）；`GlassProjectorBlock.allowsWiredConnection` 还明确禁止投影仪朝向面作为有线连接（`.../GlassProjectorBlock.java:56-66`）。`GlassWirelessExtensionBlock` 不继承 `GlassConnectedBlock`，只有 support 状态（`.../GlassWirelessExtensionBlock.java:24-35、81-87`）。
- `GlassServerState.rebuildSnapshot` 把求解器的候选条件绑定到 `GlassContent.isSurface`（`.../state/GlassServerState.java:290-302`），而 `GlassContent.isSurface` 只按 `PROJECTOR`、`EXTENSION`、`WIRELESS_EXTENSION` 类型返回 true（`.../content/GlassContent.java:71-73`）。`GlassTopologySolver` 在相邻与对角转移处也只测试 `isSurface.test(neighbour/diagonal)`（`.../core/GlassTopologySolver.java:49-69`），从未读取上述 blockstate 的 `wired`/`support` 值或双向 `connectsTo`。

候选反例：把一个没有有线连接面的无线扩展放在投影仪所求平面，或把朝向使其连接面为 `NONE` 的另一个投影仪/扩展放在连通边上。方块状态和模型不会表达 `WIRED`，但 solver 仍把该方块当作候选面；若产品合同要求求解必须服从 blockstate 的有线连接，这条路径会产生错误网络。上游 `TileEntityGlassWireless` 本来继承 `TileEntityGlassBase` 并参与普通传播，Initial port 参考又明确记录了外露面图重写，因此不能仅凭此输入差异宣称原版缺失。

意图与接受证据：初始 port 参考明确允许外露面图的现代重写，但没有说明是否有意脱离 `WIRED`/`SUPPORT` state 作为传播输入；当前代码能证明输入为何忽略连接状态，不能证明删减意图或用户接受，合同保持未知。

排查过的替代路径：检查了 block placement/update、六向 state properties、`GlassContent.isSurface`、solver 的相邻/对角分支及 `GlassProjectionMesh.shouldRenderFace`；没有发现把 connection state 注入 solver 的适配层。`GlassTopologySolverTest` 使用自定义 `isSurface`/`isFaceExposed`，未覆盖 projector facing gate 或 wireless extension 的 `NONE` 边；需先确认合同，再动态观察最终面集合。

### R03 / U09：跨维度配对可保存、不可捕获；同维度远端只支持已加载终端

状态：`部分实现；跨维度空壳不可达`。Target：`26.2-fabric`。

- 当前 `GlassServerState.setProjectorChannel` 仅按 UUID 查找终端并校验访问权（`versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/state/GlassServerState.java:175-191`），没有要求终端和投影仪同维度；`rebuildSnapshot` 也按 `terminal.pos().dimension()` 获取 `terminalLevel`（`.../GlassServerState.java:275-283`），所以跨维度 `Network` 在服务端状态和 payload 中可出现。
- 当前 `GlassRenderSnapshots.states` 只过滤投影仪所在维度（`.../client/GlassRenderSnapshots.java:32-42`），仍会为跨维度终端生成配对所需的 terminal view。`VulkanBlaze3DViewRenderer.capture` 调用 `GlassChannelCaptureContext.ensure`（`.../client/render/VulkanBlaze3DViewRenderer.java:546-559`），而 `ensure` 明确要求 `terminalDimension.equals(requestedLevel.dimension())` 且终端区块在当前 `ClientLevel` 已加载（`.../client/render/GlassChannelCaptureContext.java:95-108`）；不满足时直接返回 `false`，没有另一维度的 `ClientLevel`、WorldPortals view 或远端区块获取路径。
- 因此，GUI/packet 可以把当前维度投影仪绑定到另一维度终端，但捕获阶段永远跳过；同维度而终端区块不在客户端加载范围时也会跳过。服务端已加载终端（`GlassServerState.java:276-278`）不能替代客户端 `ClientLevel.hasChunk`。
- 上游基线 `EventHandlerClient.getTerminalPlacement` 使用当前 `mc.world` 查找终端（`src/main/java/me/ichun/mods/glass/client/core/EventHandlerClient.java:159-181`），`TerminalPlacement` 也由同一个 `World` 构造一对 view（`src/main/java/me/ichun/mods/glass/common/block/TerminalPlacement.java:43-54`）。这只能证明基线是同世界入口，不能证明上游承诺跨维度；当前问题是新增 GlobalPos 配对模型没有对应捕获实现。

直接影响：跨维度状态在服务端看似建立，客户端没有画面；远端同维度若终端区块未加载也会保持无输出。原因来源为当前 `ensure` 的硬条件和缺少替代世界入口，跨维度相对上游是否属于要求仍未知。

意图与接受证据：当前 `GlobalPos` 状态模型和 GUI 跨维度排序能证明跨维度选择被表示出来，但没有找到“跨维度必须可捕获”的产品接受记录，也没有找到为何保留选择而不提供捕获替代的说明；原因和接受状态均未知。上游 packet 通过 `player.getEntityWorld()` 隐含使用当前世界，旧 packet 无维度字段，不能据此推断旧版允许任意跨维度绑定。

排查过的替代路径：检索了 `ClientLevel`、`terminal.dimension`、`ServerLevel`、`WorldPortals`、`remote` 和 `dimension` 相关生产入口；当前兼容目录只有说明性 `compat/package-info.java`，没有外部世界或远端区块适配器。需要动态验收分别观察“同维度已加载”“同维度未加载”“跨维度”三种结果。

### R04 / U03、U07、U10：活动投影仪允许重绑频道，和基线的 inactive gate 不同

状态：`静态对应已确认；行为待动态验证`。Target：`26.2-fabric`。

- 上游基线 `BlockGlass.onBlockActivated` 只为 `TileEntityGlassMaster`/wireless 的 `!active` 状态打开设置 GUI（`src/main/java/me/ichun/mods/glass/common/block/BlockGlass.java:59-77`），`PacketSetChannel.execute` 对 master 也保留 `!active` 条件（`src/main/java/me/ichun/mods/glass/common/packet/PacketSetChannel.java:56-63`）。
- 当前 `GlassProjectorBlock.useWithoutItem` 无 `POWERED` 检查，始终调用 `GlassNetworking.openScreen`（`versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/content/block/GlassProjectorBlock.java:118-125`）；`openScreen` 自身只检查玩家与目标的维度/距离（`.../network/GlassNetworking.java:55-60`），目标 block 类型由调用方 `GlassProjectorBlock` 和客户端 `open` 路由检查，`GlassServerState.setProjectorChannel` 也没有 `GlassProjectorBlock.POWERED` gate（`.../state/GlassServerState.java:175-191`）。
- 当前 renderer 明确实现了 `GlassProjectionVisualState.isHotChannelSwitch`（`.../client/render/GlassProjectionVisualState.java:20-25`）及旧/新 capture handoff（`.../client/render/VulkanBlaze3DViewRenderer.java:151-162、322-480`），所以这项差异可能是有意的现代化能力，不能直接称作回归。

直接影响：供电中的投影仪可以被授权玩家实时改绑频道，且状态会立即重建并持久化；这与旧版“活动时不可改 master 频道”不同。原因来源明确；是否符合当前产品合同、旧白色淡出和热切换副作用仍未知。

意图与接受证据：`GlassProjectionVisualState.isHotChannelSwitch` 及 handoff 代码是允许热切换的实现意图证据，但没有找到产品验收记录确认“活动时可编辑”应覆盖 GUI、服务端状态和持久化三层；保留为行为待动态验证。

排查过的替代路径：检查了 projector block 的 POWERED 更新、GUI confirm payload、服务端设置入口和热切换 helper；未找到第二个 inactive gate。`GlassProjectionVisualStateTest` 只验证 helper 的布尔结果（`.../src/test/java/io/github/recrivenvi/glass/client/render/GlassProjectionVisualStateTest.java:32-43`），不能证明服务端 gate 或活动投影的最终画面。

## 其他已定位单元与未决项

### N11 / U02：`GlassChannelRules` 是生产不可达的旧辅助实现

状态：`生产不可达；静态对应已确认`。Target：`26.2-fabric`。

- `versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/core/GlassChannelRules.java:5-70` 仍实现基于玩家名称的 `public:<name>` / `<owner>:<name>` 字符串策略，并固定 `MAX_NAME_LENGTH = 15`。
- 当前生产路径 `GlassServerState.setTerminal` 使用 `UUID` owner、独立 `publicChannel` 布尔值以及配置长度（`versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/state/GlassServerState.java:156-172、440-447`）；`rg` 检索证明 `GlassChannelRules` 只被 `GlassChannelRulesTest` 引用，没有任何 main 入口调用。

直接影响：该类的测试可以全部通过而不覆盖实际 packet、SavedData、GUI 或 renderer 的频道语义；其名称长度、分隔符和 name-based owner 与运行时 UUID/64 字符语义不一致。它本身不改变当前运行行为，属于维护与测试合同脱节，不应作为运行时权限实现的证据。

排查过的替代路径：检索了 `GlassChannelRules`、`create`、`canAccess`、`publicChannel` 和 owner 相关生产调用，未找到转接或兼容适配；已把它从“实现缺失”与“当前权限已完成”两种结论中分离，保留为生产不可达旧辅助实现。

### U10：旧版方块实体 NBT 没有迁移解码

状态：`确认缺失（显式迁移断点）`。这项是当前源码明示的兼容边界，不把它伪装成意外 bug。

- 基线把频道、活动面、距离、无线位置和终端朝向写入方块实体 NBT：`TileEntityGlassBase.writeToNBT/readFromNBT`（`99d9b417...:src/main/java/me/ichun/mods/glass/common/tileentity/TileEntityGlassBase.java:304-342`）、`TileEntityGlassMaster.writeToNBT/readFromNBT`（`.../TileEntityGlassMaster.java:141-170`）和 `TileEntityGlassTerminal.writeToNBT/readFromNBT`（`.../TileEntityGlassTerminal.java:47-68`）。
- 当前只从 `GlassSavedState.TYPE` 的 `glass:state` 读取新结构（`versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/state/GlassSavedState.java:18-26、79-97`），源码注释明确写着旧版方块实体 NBT 不再读取；当前也没有旧 block ID/metadata 或 DataFix 解码入口。

直接影响：原版 1.12.2 方块实体中的频道、无线关系和终端朝向不会自动进入新 `SavedData`；四种拆分后的 `glass:*` 方块也没有从原版 namespace/metadata 的迁移表。来源为当前显式注释与静态搜索；是否允许在发布版本中作为断点需由产品迁移决策覆盖。

意图与接受证据：`GlassSavedState` 类注释明确声明这是现代化存档格式的断点，证明“为何代码不读取旧 NBT”的来源是显式设计；没有找到用户批准或迁移验收证据，故兼容接受状态保持待决。

排查过的替代路径：搜索旧 ID、NBT 字段、DataFix、legacy decoder 和 migration 入口，只找到语言文件中的 `block.glass.block_glass` 文本及历史迁移文档，没有运行时转换器。未读取实例或启动游戏。

### U06-1（R12/M14）：无线节点拆除后只从当前快照过滤，未从投影仪持久顺序移除

状态：`确认缺失`。Target：`26.2-fabric`。

- 上游基线 `99d9b4170a64d3692b46100d9d3e63cb41c735ee` 的 `TileEntityGlassMaster.update` 会在节点不再是 `TileEntityGlassWireless` 时从 `wirelessPos` 移除，并通知方块更新（`src/main/java/me/ichun/mods/glass/common/tileentity/TileEntityGlassMaster.java:96-100`）。
- 当前无线方块被移除时，`GlassServerState.removed` 只调用 `GlassSavedState.remove` 删除同位置的 terminal/projector 记录（`versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/state/GlassServerState.java:142-149`；`.../state/GlassSavedState.java:72-77`），没有扫描其他 projector 的 `wirelessPositions` 反向引用。
- `rebuildSnapshot` 的 `validWireless` 只在构造本次网络时过滤“当前区块已加载且方块仍为 wireless extension”的位置（`.../state/GlassServerState.java:285-323、389-400`），不会把过滤结果写回 SavedData。于是被破坏节点仍留在投影仪的持久顺序中：当前网络暂时按有效节点渲染，但重放无线顺序 GUI 会看到旧位置；同位置重新放置 wireless extension 后，旧关联会自动恢复。

直接影响：实际拆除与“临时区块卸载”没有区分。对于已加载并明确破坏的无线节点，当前只做输出层过滤，未完成基线的反向状态清理；重放或复建同位置可能恢复已删除的关联。变化类型为当前 SavedData 采用 projector→nodes 单向索引但没有删除回写，原因来源明确为当前移除、快照过滤和持久化路径。

意图与接受证据：上游自动移除无线节点是直接行为证据；当前 `validWireless` 只过滤快照、保留 SavedData 的原因没有说明。没有找到接受“破坏节点后保留未来重放关联”的记录，故把当前后果作为确认缺失，具体产品迁移意图仍未知。

排查过的替代路径：检查了 attach、order、chunk load/unload、`validWireless` 和 `GlassSavedState.remove`；`setWirelessOrder` 能由用户手动清理，但不是节点破坏后的自动迁移替代。未运行服务器或修改存档，需用副本动态验证“破坏→快照→GUI→同位置重放”链路。

### U08-1（R13/M15） / U11：环境声创建条件与原版不同，按全量活动网络创建

状态：`播放创建条件与原版不同；静态机制确认；实际声量/资源开销待动态`。Target：`26.2-fabric`。

- 当前 `VulkanBlaze3DViewRenderer.capture` 先把 `findPairs` 结果过滤为 `isNetworkObserved`/视锥内的 `pairs`（`versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/client/render/VulkanBlaze3DViewRenderer.java:514-523`），但随后把未过滤的 `currentPairs` 传给 `updateAmbience`（`.../VulkanBlaze3DViewRenderer.java:524-527`）。`findPairs` 从所有当前 `observedViews` 活动网络收集 pair，未应用 `observationDistance`（`.../VulkanBlaze3DViewRenderer.java:1311-1350`）。
- `updateAmbience` 对每个 pair 创建或移动循环 `GlassAmbienceSound`（`.../VulkanBlaze3DViewRenderer.java:1384-1420`），没有 `isNetworkObserved` 或可见面条件；即使 `pairs` 为空，代码也在此之后才提前返回（`.../VulkanBlaze3DViewRenderer.java:524-527`）。因此只要全量快照中存在远处的活动网络，客户端仍会创建声音实例。
- 上游 `TerminalPlacement.drawPlane` 在 portal plane 首次实际绘制时创建 ambience（`99d9b4170a64d3692b46100d9d3e63cb41c735ee:src/main/java/me/ichun/mods/glass/common/block/TerminalPlacement.java:78-84`）；其持续/停止还由 placement 和 active block 生命周期驱动。当前 `updateAmbience` 按全量活动 pair 尝试创建，首次创建条件与原版不同；声音资源本身已注册，持续播放是否可闻不能从静态路径决定。

直接影响：声音创建实例数量随客户端收到的活动网络数量增长，远离所有投影的玩家也会进入循环 sound 的创建路径；`observationDistance` 和主渲染过滤没有覆盖这一步。声音管理器的距离衰减、实际声量和资源开销仍需动态测量。变化类型为渲染可见性筛选与声音创建生命周期使用了不同的 pair 集合，原因来源为当前调用顺序直接证据。

意图与接受证据：上游/参考的“显示时播放、跟随最近表面”语义支持与当前创建条件不同，但没有找到允许远端活动网络预创建声音的记录。声音是否实际被 SoundManager 距离衰减而不可闻，以及创建是否带来可见资源压力，均需动态验证；这里只确认创建入口差异。

排查过的替代路径：检查了 `findPairs`、`isNetworkObserved`、`networkPositions`、`updateAmbience`、`retainActiveChannels` 和 `GlassAmbienceSound`；没有找到其他声音播放入口或后置距离过滤。本轮未启动游戏，未把“可听音量”写成已观测。

### U08-2（R14/M16） / U09：捕获失败后保留旧 RenderTarget，绘制入口没有无数据判定

状态：`静态机制确认；无数据策略与最终画面待动态验证`。Target：`26.2-fabric`。

- 当前 `capture` 为每个 pair 复用 `channelContexts` 中的 `GlassChannelCaptureContext`（`versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/client/render/VulkanBlaze3DViewRenderer.java:546-559`）。在“客户端终端区块卸载、服务器仍保留该网络 active、此前已有成功捕获 target”的可达序列中，`ensure` 因终端区块未加载直接返回 `false`（`.../client/render/GlassChannelCaptureContext.java:95-108`），调用方只 `continue`，没有清空已有 `target` 或记录本帧无效。
- `capture` 在进入早退前执行 `retainActiveChannels(currentPairs)`，只要网络仍 active 就保留 context（`.../VulkanBlaze3DViewRenderer.java:514-527、1353-1382`）。当同一个 pair 之前成功捕获过、随后终端暂时不可用时，旧 `TextureTarget` 因而继续留在 context。
- `drawProjector` 只判断 context 和 `target` 是否非空（`.../VulkanBlaze3DViewRenderer.java:970-995`），没有检查最近一次 capture 是否成功、目标区块是否仍在客户端、或当前帧是否应显示空白。相同问题也适用于 capture 内 RuntimeException：异常路径只恢复主 target/粒子状态，没有记录本帧有效性；已有 target 可能是旧内容、已清空内容或部分绘制内容（`.../VulkanBlaze3DViewRenderer.java:575-585`）。

直接影响：在上述终端区块卸载序列恢复前，投影表面可能继续合成上一帧远端画面；这是 stale frame 与无数据策略分叉的静态路径，不是已观测到的画面结论。正常跨维度切换会触发 `switchLevel/reset`，不能据此推断已有旧 target 一定被跨维度复用；热切换另有 handoff 路径。变化类型为新增实时 capture cache 没有与失败状态绑定，原因来源为当前 ensure/retain/draw 调用链。

意图与接受证据：开发参考要求无线/投影使用实时投影源，未找到允许在远端数据不可用时无限保持旧帧的记录；是否应显示空白、关闭表面或保留最后一帧仍需产品/动态验收决定。

排查过的替代路径：检查了 context 的 `ensure`、`close`、renderer `reset`、`retainActiveChannels`、surface/wireless draw 入口和异常 finally；context 只在网络失活、维度切换或客户端停止时关闭，没有捕获失败失效标记。未启动游戏，未把 stale frame 写成已观测。

### U12：终端方块与物品模型的材质不一致

状态：`静态对应已确认；行为待动态验证`。这属于资源 parity 反例，不判定构建或资源加载失败。

- 基线终端渲染器明确使用 `Blocks.OBSIDIAN` 作为终端主体、`Blocks.GLASS` 作为标记（`99d9b417...:src/main/java/me/ichun/mods/glass/client/render/TileEntityGlassTerminalRenderer.java:23-53`）。
- 当前方块注册仍复制 obsidian 属性（`versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/content/GlassContent.java:24-28`），但放置后的 block model 使用 `minecraft:block/black_concrete` 和 `green_concrete`（`.../src/main/resources/assets/glass/models/block/glass_terminal.json`）；物品模型却以 `minecraft:block/obsidian` 为 parent（`.../models/item/glass_terminal.json`）。因此 inventory 与 placed block 的主体材质必然走不同资源路径，是否为有意的重新着色需视觉验收。

意图与接受证据：当前 `GlassResourcesTest.terminalUsesBlackCoreAndGreenFrontMarker`（`.../src/test/java/io/github/recrivenvi/glass/content/GlassResourcesTest.java:152-168`）表明黑色核心/绿色标记是当前资源设计意图；基线 obsidian/glass 视觉等价是否仍为产品要求未知。

排查过的替代路径：确认 blockstate 六向旋转均指向 `glass:block/glass_terminal`，确认模型和 sound/recipe/loot 的 ID 有对应文件；没有发现资源路径完全失联，问题限定为材质/旧版视觉等价未知。

### U12-1：旧 GUI 纹理和 legacy 方块语言键存在，但没有当前生产入口

状态：`资源存在但生产不可达`。Target：`26.2-fabric`。

- 当前资源仍包含 `versions/26.2-fabric/src/main/resources/assets/glass/textures/gui/channel_set.png` 和 `.../textures/gui/wireless_order.png`；检索当前 GUI 生产源码没有 `ResourceLocation`、`blit` 或这两个文件名的引用。当前 `ProjectorChannelScreen`、`WirelessProjectorScreen`、`WirelessOrderScreen` 均使用 `GlassGuiTheme`、控件和 `graphics.item`，没有加载旧纹理。
- `assets/glass/lang/en_us.json` 和 `zh_cn.json` 仍包含 `block.glass.block_glass`，但 `GlassContent` 只注册四个拆分后的 `glass_terminal`、`glass_projector`、`glass_extension`、`glass_wireless_extension`（`versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/content/GlassContent.java:24-46`），当前没有 `block_glass` registry 或 blockstate/item 资源。

直接影响：这些资源不会参与当前 GUI 或内容注册，属于旧 GUI/旧 block 身份的残留，不能被计入当前功能已接入的证据；本身不构成加载失败。变化类型为旧 GUI/资源路径迁移后未删除或未标记，来源为当前搜索直接证据。

排查过的替代路径：核对了当前 GUI 的所有 screen/theme 类、资源索引和内容注册，未找到隐式纹理加载或 legacy block 注册；原版基线 GUI 确实直接使用 `channel_set.png` 等纹理，但当前 GUI 是允许的独立实现，是否保留这些残留只需在整理阶段决定。

### N06 / U13：外部 WorldPortals/iChunUtil 入口被自研 renderer 替代，但等价未确认

状态：`定位但接入未确认；行为待动态验证`。Target：`26.2-fabric`。

- 基线 `ProxyCommon.preInit` 调用 `WorldPortals.init()` 并注册 iChunUtil `PacketChannel`（`99d9b417...:src/main/java/me/ichun/mods/glass/common/core/ProxyCommon.java:18-31`）；`TerminalPlacement` 继承 `WorldPortal` 并通过 WorldPortalRenderer 绘制终端 view（`.../common/block/TerminalPlacement.java:24-54、168-185`）。
- 当前 `versions/26.2-fabric/build.gradle.kts` 未声明 iChunUtil/WorldPortals 依赖，兼容目录仅有说明性 package 文档；实际入口是 `VulkanBlaze3DViewRenderer` 和自有 `GlassChannelCaptureContext`。因此当前 renderer 的接入路径已定位，能否在不依赖旧 API 的前提下保持观察结果等价仍需动态验证；不据此额外推断碰撞、跨维度或其他能力缺失。

排查过的替代路径：搜索当前生产源码中的 `WorldPortals`、`WorldPortalRenderer`、`PacketChannel`、外部 module 及 compat adapter，没有找到对应调用；未把依赖未知升级为确认缺失，等待独立动态验证。

意图与接受证据：当前仓库文档允许自有渲染内部实现，当前 renderer/context 的接入路径已定位；没有找到一份静态记录能证明它与 WorldPortals 的最终可观察结果等价，故只保留“等价待动态验证”，不把依赖替换写成能力缺失。

## 静态覆盖与动态前置

本轮已静态覆盖：当前 `26.2-fabric` 的注册/内容、频道权限写入与快照广播、终端/投影仪/无线 GUI 入口、有线求解器与多网络绘制、连接状态与无线节点边界、跨维度/远端捕获条件、持久化读取边界、终端模型资源和旧版 WorldPortals/iChunUtil 入口。没有运行构建、测试、游戏 GUI、客户端或网络请求；现有测试源码只用于了解断言覆盖，不能证明当前行为。

建议动态验收按以下观察顺序前置：

1. 先确认产品对 private 频道的合同范围，再分别验证 visitor 的配置/写入拒绝、可见投影面和元数据是否应隔离；不要把三者均按 owner 隔离作为未经确认的判定前提。
2. 在同一有线面组件放置两个不同频道的供电投影仪，记录每个面最终显示的频道、draw 顺序变化和拆除其中一个投影仪后的重建结果；判定依据是不同频道不能共享同一激活面。
3. 分别验证同维度已加载终端、同维度客户端未加载终端、跨维度终端；记录 GUI 选择、服务端快照、capture 日志和最终画面，区分状态建立与实际输出。
4. 在活动投影仪上重绑频道，观察旧版 gate 差异、热切换淡入淡出、无线三角形和声音/资源释放；随后断开重连检查快照与 SavedData。
5. 对旧 1.12.2 世界只做副本升级观察，检查 block ID、频道、无线位置和终端朝向的迁移结果；不要把新 `glass:state` 的空状态误判成历史数据已保留。

本轮结论分开表述：静态路径覆盖已达到上述单元；R02 和 U06-1 有直接的当前实现反例，U10 有明确迁移断点，R01 的广播粒度与私有读侧合同待决，R03 的跨维度捕获是当前不可达路径，R04、R05、U08-1、U08-2、U12、N06、N11 的最终等价、声量、资源开销和视觉结果必须保留为行为待动态验证。主账对应：U06-1=R12/M14，U08-1=R13/M15，U08-2=R14/M16。

## 结论

历史结论按正文保留。迁移后的当前代码位置与本轮验证结果见 [构建迁移说明](../development/porting-template_migration.md)。
