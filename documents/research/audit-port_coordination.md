# 旧 port 审计：总协调独立复核

调查日期：2026-09-10。以下为迁移前审计的原始发现；2026-10-19 仅增加文档格式小节并迁移位置，未重新判定历史结论。

## 问题

核对旧 port GLASS 的移植状态和证据边界。

## 方法

按下列原始记录中的固定基线与只读方法调查。

## 发现

审计日期：2026-09-10。此记录由总协调维护；项目主入口为 [正式证据账本](audit-port_status.md)。本轮研究和协调复核已收尾；不能据此宣布能力等价、移植完整或所有依赖细节均已穷尽。

## 身份与实际工作树

- 旧 port：`D:/Workspaces/Repositories/GitHub/RecRivenVI/GeneralLaymansAestheticSpyingScreen`；不包含 `Repositories/Local/GLASS` 第二次实现。
- origin `https://github.com/RecRivenVI/GeneralLaymansAestheticSpyingScreen.git`；upstream `https://github.com/iChun/GeneralLaymansAestheticSpyingScreen.git`。
- 当前 `stonecutter`，HEAD `da59323c918c4bc2a928bce6d2dca860db26e881`。开场为 180 个未暂存删除、7 个修改及未跟踪迁移内容，暂存区为空。源码分析针对实际工作树，而非仅 HEAD。
- 封存 `D:/Workspaces/Artifacts/OldPortAudit/20260910/GLASS-start.json`，manifest SHA-256 `f900a8e8f4be307c27d55b2fc8b5d9d5f71ef81f6da5c761d73620e8dd53cd7d`。
- 210 个实际文件；source seal `eece50c673baf18f8f47d9cd848acb597b4b9cc92fc81f71f6c3a9dc7da5fb52`。同时保留 index/status 和缺失 tracked 路径。
- 范围：tracked + 非 ignored untracked，排除 runtime/cache/IDE、本机私密配置、本轮 `audit-port-*` 文档及软链接目标。具体排除见 manifest；不是运行实例或所有 ignored 内容快照。
- 本轮只研究与写记录，不改实现、构建、矩阵和配置，不清理、不启动游戏、不提交或推送。没有 README 的现场状态保持原样。

## 已独立核实

### GLS-C001：上游谱系基点可恢复

`git rev-list --parents -n 1 0f33e54b45b37692c1fbf1fbc127ff46f368feea` 的唯一父提交为 `99d9b4170a64d3692b46100d9d3e63cb41c735ee`。提交说明为适应 iChunUtil 变更，日期 2017-12-12；它也是当前本地 `upstream/master` 引用。固定此 SHA 做能力盘点，不把分支名称或当前时间当版本依据。

仍需检查它绑定的实际 Minecraft、iChunUtil 与 WorldPortals 行为；外部依赖边界不能只根据 GLASS 本仓同名类判断。

### GLS-C002：当前实际声明为一个 implemented 加四个 planned

`settings.gradle.kts` 列出五个完整 Target ID，`vcsVersion = "26.2-fabric"`。设置脚本阻止 planned Target 的产品任务进入 Loader 解析、编译、打包或运行。Fabric 26.2 Facts 声明 `target.status=implemented`；例如 NeoForge 1.21.1 Facts 声明 `target.status=planned`。

因此必须将 planned 源码/接线情况逐一列明，不能用旧“四节点”的描述或单目标历史 PASS 推定全部可用。主任务负责其余目标及 capability 的完整映射。

## 调度与下一步

实时核实主任务“恢复 GLASS 仓库上下文”，ID `01a08652-a9cd-72a3-b18c-c58934f56e01`；调度 API 接受 `gpt-6-astra`、`max` 的审计请求。已避开旧的恢复前任务。主任务已回报实际创建且 running 的 `/root/upstream`、`/root/mapping`、`/root/review`，均为 `luna_worker`、`fork_turns=none`，工具角色固定 GPT-5.6 Luna/max；未返回额外 UUID。各自独占 upstream、mapping、review 文档。

总协调已审阅上游到当前的合并表，并独立抽查声音、广播、频道占用及审计词典，推动私有保密合同等过强判断更正。未取得原始 iChunUtil deobf JAR 的唯一二进制绑定，使用有依据的源码候选并明确限制；不能把其全部库功能视为原版 GLASS 已使用的能力。

补充调度读回：实际 rollout 本次 `turn_context` 的 `turn_id=01a08a35-3e1d-7643-b604-1799b162c4c2`，`model=gpt-6-astra`、`effort=max`，与任务状态接口一致。过滤后的证据位于 `D:/Workspaces/Artifacts/OldPortAudit/20260910/main-task-models.json`。

## 第一批能力复核

### GLS-C003：环境声资源及实际触发链有承接，空间选择仍有行为差异

上游 `99d9b4...:src/main/resources/assets/generallaymansaestheticspyingscreen/sounds/amb.ogg` 与当前 `versions/26.2-fabric/src/main/resources/assets/glass/sounds/amb.ogg` 均为 78,292 字节，SHA-256 `df8f582bf7779e41536e21e9442d21d1da91bcae8b00abf5a1686f47cf42192b`，字节一致。

上游 `common/block/TerminalPlacement.java:83` 以音量 0.015、音调 0.6 创建循环的 SoundGlassAmbience；其 `client/sound/SoundGlassAmbience.java` 根据 placement 存留停止播放，并从 activeBlocks 按 render-view 实体眼部坐标选最近声源。

当前 sounds.json 将 `ambience` 映射到 `glass:amb`，GlassContent 注册 SoundEvent，GlassAmbienceSound 保留相同音量、音调、循环。`GlassClient.java:15` 创建真实 VulkanBlaze3DViewRenderer，metadata 声明的 GameRendererMixin 在 `renderLevel` 中调用 capture (`GameRendererMixin.java:49`)；capture 的 `VulkanBlaze3DViewRenderer.java:525` 调用 updateAmbience，`:1384-1418` 实际播放/移动声音，`:1374-1380` 与 `:1447-1448` 实际停止/清理。

结论：限 Fabric 26.2，资源保留和调用接入均有静态证据，不是仅保留未引用 OGG。但当前按 player 距离选择 snapshot 中的 active 节点或无线节点，原版按 render-view/eye 与 placement.activeBlocks；候选集合、相机语境和退出条件不同，不能宣布听觉行为完全等价。

后续主审 M15/R13 的限定补充：上述 `observedViews` 是全维度 snapshot 的视图集合，不等于已经通过可见性筛选。capture 在 `:515-523` 形成过滤后的 pairs，但 `:525` 给 updateAmbience 的仍是未过滤 currentPairs，因此创建声音对象不以本帧捕获成功或距离筛选为前提。这里修正早期“已观察”措辞，避免误读为已渲染/在视野内；没有因此断言远处声音可听或已经发生性能问题。

后续动态验证：在相同的有线/无线投影场景中沿不同节点移动、切换视角、离开网络和世界，观察声源位置、持续时间与停止情况；前置是对应场景可渲染且 sound 开启。本轮未运行游戏或播放声音。此条也不证明另外四个 planned Target 有声音实现。

### GLS-C004：独立复核当前全量状态广播，保密合同需单独判断

`GlassServerState.java:239-250` 直接向指定玩家或所有在线玩家发送同一完整 snapshot；`:175-191` 的 owner/public 授权检查位于选择频道的写入入口。此时可以确认“该实现不按接收者过滤完整状态”，不能仅从 private 名称自动推导用户已要求物理屏幕对旁观者不可见。

主任务已核对历史设计并校准 R01：限制谁可以选择/连接频道，与限制谁能看到物理屏幕或获知位置是不同能力。尚未找到读侧保密承诺，因此记录传输机制和保密能力未提供，保留为用户待决项，不把新设想当已违反的移植需求。R01 的词典和后续验收标准均已按此更正。

### GLS-C005：双频道有线占用规则相对上游改变

上游 `99d9b4...:common/tileentity/TileEntityGlassBase.java:186-228` 的 bePropagatedTo 在未激活或已经同频道的条件下接受传播，活动方块承载单个频道。当前 `GlassServerState.java:287-302` 对每个 projector 独立调用 GlassTopologySolver.solve，predicate 只有方块类型与外露面；solver 的距离 map 是每次调用的局部状态，没有其他网络的占用输入。

这给出了可复查的语义差异：两个不同频道投影仪可分别产生含同一面的网络，原单频道方块的排他条件没有以等效输入进入求解器。主审和反例组已核对逐网络渲染提交，未在其对应入口找到跨频道共用面的仲裁，具体见 R02。不能将“有线投影已实现”直接视为所有多网络场景等价。最终混合、绘制顺序和可见现象仍须动态验证；是否接受新的占用语义无本轮授权决定。

## 本轮交付与结束核查

主任务与三个 Luna/max 代理均已完成研究和最终校准。正式入口归并 U01–U13、反向 N01–N11、主审 M01–M17、风险与六类后续动态任务；全部顶层审计单元都有对应或有限结论，额外依赖细节和动态行为明确列为未证明。当前唯一实际产品 Target 是 Fabric 26.2，另外四个是 planned 配置。

总协调结束封存 `D:/Workspaces/Artifacts/OldPortAudit/20260910/GLASS-end.json`，SHA-256 `30065182cfbbbb746dff34d7feb349111d39009b1c03b2e52dd7abfd37e3c7b9`。与开场同范围 210 个现存文件比较，变化 0；HEAD、分支、远端、index 和缺失 tracked 路径均相同。仅新增五份 audit-port 文档，既有迁移改动保留。

统一核查见 `D:/Workspaces/Artifacts/OldPortAudit/20260910/coordination-verification.json`，SHA-256 `92347f94f9acc06929bf18090539531411cb902e4ca85603d3130164bd5b11d4`。`git diff --check` 通过；未改产品实现/资源/配置、未构建或运行产品测试/游戏、未 stage/commit/push。运行实例和缓存不在源码封存声明范围内。

## 结论

历史结论按正文保留。迁移后的当前代码位置与本轮验证结果见 [构建迁移说明](../development/porting-template_migration.md)。
