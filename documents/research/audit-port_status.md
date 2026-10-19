# 旧 port 移植现状静态审计

调查日期：2026-09-10。以下为迁移前审计的原始发现；2026-10-19 仅增加文档格式小节并迁移位置，未重新判定历史结论。

## 问题

核对旧 port GLASS 的移植状态和证据边界。

## 方法

按下列原始记录中的固定基线与只读方法调查。

## 发现

审计日期：2026-09-10。状态：本轮静态审计已完成并封存；不能据此声明移植功能完整或行为等价。结论、覆盖边界及后续任务见下文。

## 范围与快照

- 对象：`D:\Workspaces\Repositories\GitHub\RecRivenVI\GeneralLaymansAestheticSpyingScreen`，旧 port，排除 Local 下的 Second Implementation。
- 实时 Git root 与上述路径相同；分支 `stonecutter`；HEAD `da59323c918c4bc2a928bce6d2dca860db26e881`。
- origin：`https://github.com/RecRivenVI/GeneralLaymansAestheticSpyingScreen.git`；upstream：`https://github.com/iChun/GeneralLaymansAestheticSpyingScreen.git`，upstream push 为 `DISABLED`。
- 开始状态：180 个已跟踪删除、7 个已跟踪修改、189 个未跟踪文件；暂存差异为空，只有一个工作树。本轮新增审计文档不在此开始计数内。
- 已读工作区 `D:\Workspaces\AGENTS.md`、其 CLAUDE 引用、仓库 AGENTS 和现有 [结构迁移记录](../development/porting-repository_layout.md)。仓库 README/CLAUDE 当前不存在，不恢复旧文件。
- 本轮只研究并写审计记录；不改实现、资源、依赖、构建或运行行为；不整理、不删除、不 stage/commit/push、不启动游戏、不执行构建或产品测试。

开始清单：`D:\Workspaces\Artifacts\GLASSPortAudit\20260910\manifest-start.json`，SHA-256 `81a3b04fab8f0e47572e376f09f59a90f53a0744605e13bd64ff428d1cfa5019`。同目录 `SourceSnapshot` 保存选定的 210 个现存文件，共 801,798 字节；清单还记录 173 个在选定范围内已缺失的 tracked 路径。完整 Git 状态另存 `git-status-start.txt`，index 表 `git-index-start.bin`，index/worktree patch 分别 `git-index-start.patch`（0 字节）和 `git-worktree-start.patch`。

选择范围是 Git 已跟踪或未忽略的源码、原生测试、资源、构建配置、Target Facts、现有文档和验证 Profile；排除 `.git`、各层 build/.gradle/.kotlin、run、instances、validations/*/instance、local.properties 和本轮 audit-port 文档。实例、缓存和本机私密配置不进入快照。未提交内容的结论绑定此清单和工作树，不归于 HEAD。结束时重新比对内容及 index。

## 上游基线

Git 原始对象证明 `0f33e54b45b37692c1fbf1fbc127ff46f368feea`（Initial port）的直接父提交为 `99d9b4170a64d3692b46100d9d3e63cb41c735ee`。本轮选用此父提交为移植比较基线，进一步检查来源及独立上游副本。它的 build.gradle 声明 Forge `1.12.2-14.23.0.2493`、ForgeGradle `2.3-SNAPSHOT`、Java 8、产品 `1.12.2-7.0.0`，iChunUtil 来自未锁定的本地 deobf JAR。

独立上游副本已由 A 获取：`D:\Workspaces\Repositories\GitHub\iChun\GeneralLaymansAestheticSpyingScreen`，detached HEAD=`99d9b4170a64d3692b46100d9d3e63cb41c735ee`，主任务 `git ls-files` 复核为 53 个 tracked 文件，clean；该参考副本 remote 名为 upstream。A 初报的 81 是含 Git 内部文件的目录计数，不用于源码覆盖。A 实时核对 master 与远程 master 也同 hash，故本轮所见没有基线之后的上游主线新增；这不推广到未审查的发行二进制或其他 fork。主任务又从 `0f33e54:docs/reference/upstream-behavior.md` 读到相同基线 hash，以及 iChunUtil 参考版本 `1.12.2-7.1.3`。Git 直接父关系和已提交来源记录相互支持该基线。

参考内容清单：`D:\Workspaces\Artifacts\GLASSPortAudit\20260910\reference-manifest.json`，SHA-256 `40c01c3994534f0eeb0d98f5229dae9945221940ca48ff2a031943ed608038c8`；包含 GLASS 全部 53 个 tracked 文件与 iChunUtil 所选 24 个 WorldPortals/network/版本/构建入口文件。它不是整个 iChunUtil 功能审计。历史 Initial port 的来源说明与验收说明以 `initial-port-source-record.md`、`initial-port-acceptance-record.md` 保存在同一外部证据目录，并明确其 commit 来源，不恢复进工作树。

不以最新上游直接判定原始遗漏。iChunUtil 7.1.3 是移植时记录的参考版本；原始上游 build.gradle 未锁定本地 deobf JAR，因而不能断言所有历史构建都用了该版本。准确依赖源码与外部行为仍在研究。官方 [GLASS 基线提交](https://github.com/iChun/GeneralLaymansAestheticSpyingScreen/commit/99d9b4170a64d3692b46100d9d3e63cb41c735ee) 可复查其 iChunUtil API 适配。

## 工作分工与状态

主任务拥有本文件，负责基线、快照、统一账本及高风险独立复核。三个指定的 GPT-5.6 Luna / Max 子代理分别拥有：

- [上游能力与来源](audit-port_upstream.md)：上游能力、触发、实现、资源、依赖清单。
- [当前映射与 Target](audit-port_mapping.md)：当前实际入口、五个 Target、反向新增来源。
- [独立反例复核](audit-port_review.md)：遗漏、空壳、不可达、资源失联和关键等价反例。

代理已实际创建：`/root/upstream`、`/root/mapping`、`/root/review`。三次调用均为 `agent_type=luna_worker`、`fork_turns=none`，该工具角色固定 `gpt-5.6-luna` / `max`，创建后 list_agents 确认三者 running；工具只返回 canonical task_name，不编造其他 UUID。主任务配置已由总协调从本轮 turn_context 读回，并由本任务读取 `D:\Workspaces\Artifacts\OldPortAudit\20260910\main-task-models.json` 的 GLASS 条目核对：thread=`01a08652-a9cd-72a3-b18c-c58934f56e01`，turn=`01a08a35-3e1d-7643-b604-1799b162c4c2`，model=`gpt-6-astra`、effort=`max`。总协调文件 audit-port_coordination.md 不归本任务编辑。

总协调另有独立开场封存：`D:\Workspaces\Artifacts\OldPortAudit\20260910\GLASS-start.json`，其报告 SHA-256 `f900a8e8f4be307c27d55b2fc8b5d9d5f71ef81f6da5c761d73620e8dd53cd7d`、sourceSeal `eece50c673baf18f8f47d9cd848acb597b4b9cc92fc81f71f6c3a9dc7da5fb52`。两套清单选择范围以各自内容为准，不假设哈希应相同；总协调封存保持原位。

结束时 collaboration.list_agents 已确认三者均为 completed，均返回最终结果；B/C 的初稿经主审要求修订后已统一状态、编号和合同边界。子报告中标注的第一批阶段记录是历史进度；最终覆盖和闭合结论以本入口索引及末尾封存为准。没有遗留仍运行的子代理或待创建的基线任务。

## 统一编号与证据规则

初始能力分组供三方对齐，必要时只增加子编号，不回收旧 ID：U01 注册与内容；U02 频道与权限；U03 终端；U04 投影与拓扑；U05 有线扩展；U06 无线扩展；U07 GUI 与交互；U08 渲染；U09 跨维度与远端世界；U10 状态、持久化与网络；U11 配置、限制与生命周期；U12 资源、配方与声音；U13 iChunUtil/WorldPortals 外部边界。反向新增用 N01 起编号，风险用 R01 起编号。

每条记录区分：未检查、尚未找到对应、定位但接入未确认、部分实现、空壳或不可达、确认缺失、静态对应已确认、行为待动态验证。实现存在不等于行为等价；一个 Target 的对应不代表其他 Target。记录上游 commit/路径/符号、当前路径/符号/入口/Targets、变化类型、行为证据、原因证据及缺证和下一步。

## 首批已读入口

- `settings.gradle.kts` 明确注册五个 `:version:<target>`，只允许 `26.2-fabric` 执行业务任务，其他 Target 的直接业务请求在 settings 阶段被阻止；该观察尚不代替逐 Target 源码和资源检查。
- 根 `build.gradle.kts` 的 testAll/checkAll/buildAll/collectArtifacts 只依赖 `:version:26.2-fabric` 的对应任务。
- `gradle.properties` 声明 `glass`、`io.github.recrivenvi.glass`、`8.0.0-alpha.1`；旧身份数据是否兼容需要单独证据。

## 合并结论与覆盖索引

**本轮静态覆盖范围**：原始上游 U01–U13 均已有能力清单、当前映射或明确未承接/有限对应结论；五个当前 Target 均检查了 Facts、源码输入、Loader 描述、任务入口与产物接线。主任务亲自复核频道权限、终端、拓扑竞争、无线模式、存档、渲染/跨维度及关键外部依赖，见 M01–M17。不是对每个 Minecraft/依赖分支的穷举证明，也不包含任何动态验收。

**移植功能完整性**：不能判为完整、行为等价的移植。当前仅 Fabric 26.2 有 85 个 main Java、43 个资源、36 个测试源码文件；其他四个 Target 只有 planned 配置，没有产品 src。Fabric 主链已接入，但有原版交互未承接、明确行为变化和当前选择/捕获边界不一致；复杂画面、输入、网络往返和持久化结果仍待动态验证。上游覆盖清单完成率不作为移植完成率。

下表的 A/B/C 分别指 [上游清单](audit-port_upstream.md)、[当前映射](audit-port_mapping.md)、[反例复核](audit-port_review.md)；M 为本文件亲自复核。每行的行为/原因/路径证据保存在对应分项，避免用超宽表重复全文。

| 能力 ID | 当前对应与范围 | 合并状态与证据 |
| --- | --- | --- |
| U01 注册与内容 | Forge 事件/TE 改为 Fabric initializer、四 Block/Item 和声音 | 静态接入确认；更新通知未承接。A/U01，B/U01，M11 |
| U02 频道与权限 | 字符串频道改 UUID/owner，服务端校验，全量同步 | 重写已确认；配置权限与读侧保密分开。A/U02，B/U02，C/R01，M01/M09/M13 |
| U03 终端 | 方块朝向、SavedData、服务器打开 Screen、目标锚点 | 静态对应；身份/存档断点及不同维度选择受限。A/U03，B/U03，M01–M03 |
| U04 投影与拓扑 | 每投影器独立外露面 BFS、红石与客户端过渡 | 部分原版条件未承接；不同频道面排他变化已确认。A/U04，B/U04，C/R02，M04 |
| U05 有线扩展 | 有线/支撑模型及外露面传播 | 传播对应；左击临时白罩缺失；模型连接不自动等于传播合同。A/U05，B/U05，C/R05，M04/M05 |
| U06 无线扩展 | 有序 GlobalPos、输入校验、三角面与动画 | 静态对应；单节点模式和拆除后持久关联与原版不同。A/U06，B/U06，C/U06-1，M12/M14 |
| U07 GUI 与交互 | 四 Screen、距离排序、世界轮廓预览、配置界面 | 对应及主动新增；热切换/输入体验待动态。A/U07，B/U07/N07，C/R04，M05/M08 |
| U08 渲染 | 独立 renderer/target 复用同一个 client.level，投影合成 | 实际链和关键目标签名确认；数学/视觉等价、旧帧策略未证。A/U08，B/U08，C/U08-2，M06/M07/M10/M16 |
| U09 远端世界 | 同维度已加载 ClientLevel；GlobalPos 可保存异维度终端 | 原版同 World；当前异维度配对可存而捕获不可达。A/U09，B/U09，C/R03，M02 |
| U10 状态与网络 | 世界级 SavedData、两类下行/四类上行 payload | 新格式静态对应；原版 TE/旧 ID 自动转换缺失。A/U10，B/U10，C/U10，M03/M13 |
| U11 配置与生命周期 | JSON 参数、tick 合并、加载/卸载与资源关闭 | 静态接入；40→默认64等范围、阈值和动画改变。A/U11，B/U11，M04/M06/M11/M12 |
| U12 资源、配方、声音 | 现代数据路径、模型/Shader引用、同音频素材 | 引用链有对应；另有未消费旧资源，材质/声音条件不同。A/U12，B/U12，C/U12/U08-1，M15/M17，总协调 GLS-C003 |
| U13 外部依赖 | iChunUtil/WorldPortals 改自研实现及 Fabric/Minecraft API | 候选库源码可查、替代入口确认；不是外部库全能力迁移。A/U13，B/U13，M06/M07/M10/M11/M13 |

反向内容统一采用 B 文档的 N01–N11：N01 身份与命名空间；N02 五 Target/构建协调；N03 世界级 UUID 状态；N04 服务端校验；N05 连接/支撑模型；N06 渲染资源与 Mixin；N07 GUI/空间预览；N08 数据/模型/Shader资源；N09 动画与热切换；N10 本次源码目录落点；N11 生产不可达的旧规则辅助类。N01/N07 有原始用户记录，N02/N10 有当前规范和迁移记录，其余有开发记录/代码来源但不自动表示全部行为影响已获用户接受；N11 保留动机未知。

本轮已排查但只能给有限结论的项目是：原始 deobf JAR 的唯一二进制身份、严格相机/合成等价、全部 Mixin 的运行注入、模型/声音/输入的实际表现、旧格式数据升级结果、私有读侧合同与连接模型合同。没有“尚未检查”的 U 顶层能力；尚未做的静态细节包括另外十个 Mixin 的 Minecraft 目标逐一字节码复核、完整 iChunUtil 非 GLASS API、各历史发行二进制/fork 对照、每个游戏原生分支与全部测试逐断言评估。这些不计入已确认覆盖；四 planned Target 没有源码可供语义移植审查。

## 按影响排序的风险与待决

| 优先级 / ID | 已确认范围 | 后续处理 |
| --- | --- | --- |
| 高 / R02 | 不同频道可各自生成同一有线面；原版单块频道排他条件未在新图出现 | 若保留原版竞争语义，应补明确仲裁并做双网络对照；最终覆盖/混合画面未实测 |
| 高 / R03 | 异维度终端可选择和保存，但 capture 硬拒绝 | 应先统一选择与捕获能力边界；实现跨维度是额外产品工作，不能在本轮推定授权 |
| 高 / R07 | 原版 TE NBT/旧注册 ID 无自动迁移入口 | 发布/升级前明确是否支持各旧格式；原数据保护与格式转换分别验收 |
| 高 / R12 | 无线节点明确拆除后，旧坐标仍留在投影器持久顺序；同位置重放可重新关联 | 原版自动移除未承接；区分真实拆除与临时卸载后决定清理策略，见 M14 |
| 中 / R06 | 左击/持续挖掘的临时白罩传播没有输入到动画的替代 | 原版交互未承接；恢复或明确移除由用户决定，原因仍未知 |
| 中 / R11 | 1 个有效无线节点时当前退回有线、原版无普通传播/无线面 | 明确阈值/卸载时策略；覆盖 0/1/2 节点与重载 |
| 中 / R09 | 相机数学、裁切/透明/异常恢复、按网络捕获和预算不同 | 不声称严格等价；按 M06/M07/M10 的观察点验证 |
| 中 / R14 | 已成功捕获的终端后来仅在客户端卸载时，已有 target 未失效且仍可提交 | 先定义无数据时旧帧/空白/关闭策略，再动态核对，见 M16 |
| 变化 / R13 | 声音对象可在距离/视锥筛选之外、首次成功捕获前创建 | 创建条件变化确认；实际可听范围与声音引擎成本未测，见 M15 |
| 待合同 / R01 | 所有玩家收到包括私有频道的元数据，物理显示不按 owner 过滤 | 这是已确认机制；未找到保密承诺。先定义私有范围，再决定是否修改读侧 |
| 待合同 / R05 | 模型的 WIRED/SUPPORT 与外露面 solver 使用不同输入 | 不是已证缺陷；上游无线节点也属于 Base。先确认连接杆与投影传播是否必须同一合同 |
| 变化 / R04、R08 | 活动投影可热切换；现代终端放置模型与物品材质不同 | 有现代化/GUI开发意图，最终视觉与交互是否接受仍需验收 |
| 辅助 / R10、N11 | 旧更新通知未承接；两个旧规则类只有测试引用 | 不自动增添网络服务或删类；分别决定产品需要与维护归属 |

上游未提供的跨维度传送、私有读侧保密和库的通用递归能力，不计为 port 的原始功能遗漏。对“当前实现为何这样运行”有代码证据，并不等于“为何选择该取舍/是否获接受”也有证据。

## 后续动态验收任务与恢复位置

这些是后续任务定义，本轮没有执行；需另行授权运行游戏。共同前置是固定待验收代码/依赖快照、使用明确专用实例及必要的存档副本，并注明 Target 与服务器/客户端加载状态。

1. **D01 同世界真实投影**：终端和投影器都在客户端已加载区块，布置六向与不同距离；移动观察相机，检查视差、roll、出口面、透明遮挡、实体/方块实体/粒子/天气/云和声音。判定必须是实时目标世界与约定遮挡结果，不能用构建成功或固定图片代替。
2. **D02 网络竞争与传播**：同连通面两个不同频道，分别/同时供电、拆除再放置、凹凸角、遮住源面。记录每个网络面集合与最后可见频道；若目标为原版语义，应有单面归属，不能靠 draw 顺序掩盖冲突。
3. **D03 无线与触发**：0/1/2/多个节点、重新排序、节点卸载/加载、重复/超长请求、红石与活动热切换；另测左击和持续挖掘。判定按用户确认的阈值/交互合同，并检查实时画面不中断、状态不被非法请求改变。
4. **D04 权限与可见范围**：owner 和 visitor 分别尝试编辑私有终端、选择私有频道、管理无线；同时记录 GUI、payload 与物理屏幕。配置权限按现行 owner/public 规则判定；元数据/物理观看是否需隔离先由产品合同确定，不能擅设为全部隐藏。
5. **D05 世界边界**：同维度已加载、同维度客户端未加载、跨维度三个场景，分别记录 GUI 可选项、server snapshot、capture 拒绝及最终画面。判定选择范围/提示与实际能力一致，不能把保存 GlobalPos 视为跨维度实现。
6. **D06 生命周期与数据**：先区分原版 1.12.2 TE、早期 port 旧 namespace、新 glass:state 三种数据；各自在副本中保存退出/重连/区块卸载/资源重载/窗口尺寸变更，并观察异常后主世界状态、声音和 GPU 资源释放。没有转换器时应明示不支持升级，不能将空新状态判为恢复成功。

已完成的基线/代理创建/源码研究无需重做。恢复审计时先复核末尾封存，再按 R02/R03 的具体边界和未决合同选定下一项；在用户选定范围前，现有证据不授权继续重写渲染、增加跨维度支持、删除辅助代码、迁移实例或恢复旧文档。

## 主任务独立复核记录（持续追加）

以下 `P` 为 `versions/26.2-fabric/src/main/java/io/github/recrivenvi/glass/`；`U` 为上述固定上游副本的 `src/main/java/me/ichun/mods/glass/`。行号绑定开始快照，上游均绑定 `99d9b417`。这些记录是主任务亲自读取代码所得，未借用子代理结论。

### M01 / U02、U03：频道身份与编辑权限已重写

- 上游 `U/client/gui/GuiChannelSetter.java:141` 拼接 `public:` 或玩家名加频道文本；`U/common/packet/PacketSetChannel.java:45` 接收后直接更新 Terminal.channelName 或非 active Master.setChannel，没有在该入口校验 owner、距离或角色权限之外的输入。`U/common/tileentity/TileEntityGlassTerminal.java:19` 按字符串将已加载终端放入客户端 map。
- 当前 `P/state/GlassSavedState.java:39` 给每个终端生成独立 UUID；`P/state/GlassServerState.java:156` 仅同维度八格内、已加载且类型正确的终端可编辑，已有 owner 时只允许相同 UUID 玩家；编辑保留频道 UUID。`setProjectorChannel:175` 按目标频道 UUID 找终端并检查 public/owner；其入口没有旧版的 inactive 条件。
- 变化类型：重写与新增校验，静态对应已确认；频道重名、改名、公开切私有、原拥有者不在场及活动投影热切换的可观察行为并不与字符串频道自动等价。原因证据：`0f33e54:docs/reference/upstream-behavior.md` 明确要求现代服务端重新校验，属于开发记录；不是用户接受全部语义差异的证明。
- 边界：当前 snapshot 在 `GlassServerState.java:247` 向所有在线玩家发送，`329` 包含所有 terminal/projector，不可把 private 解释为未授权客户端收不到元数据。是否要保护展示内容/坐标是产品决策，不能用 GUI 筛选证明保密。后续复核 GUI 与服务端权限是否一致。

### M02 / U09：原版同世界边界与当前跨维度选择不一致

- 上游 `U/client/core/EventHandlerClient.java:159` 从 `mc.world` 查询 terminal；`:181` 用同一个 `mc.world` 创建 TerminalPlacement。`U/common/block/TerminalPlacement.java:43` 两个配对 WorldPortal 都使用同一个 world。故原版 GLASS 的这条实际入口是当前客户端同世界观察，不能因外部 WorldPortals 名称就声称原版 GLASS 支持跨维度。
- 当前 `P/client/gui/ProjectorChannelScreen.java:43` 从全局终端中按 public/owner 过滤，没有维度过滤；`P/state/GlassServerState.java:175` 允许同维度近距投影器选择其他维度的合法频道，`:276` 只要求终端的 ServerLevel/区块已加载。`P/client/GlassRenderSnapshots.java:35` 只过滤投影器所在维度，保留跨维度 terminal。
- 当前捕获最终在 `P/client/render/GlassChannelCaptureContext.java:95` 检查 terminalDimension 必须等于 client.level.dimension 且终端区块存在，否则返回 false；`VulkanBlaze3DViewRenderer.java:552` 随即跳过该捕获。
- 状态：已定位并确认选择/保存可达，但跨维度捕获不可达；属于当前新增选择范围与渲染边界不一致，不列为原始跨维度能力遗漏。静态影响为选择可能无实时画面；具体白面/旧帧/提示表现待动态验证。原因未知，不能称已接受取舍。

### M03 / U10：存档格式与身份的明确断点

- 上游 Terminal 的 `channelName/facing` 和 Master 的 `setChannel/powered/placingFace/wirelessPos` 由 TileEntity NBT 写读（`U/common/tileentity/TileEntityGlassTerminal.java:54`、`TileEntityGlassMaster.java:141`）。
- 当前 `P/state/GlassSavedState.java:17` 明文说明旧版方块实体 NBT 不再读取；`:21` 使用 `glass:state` SavedData，内容为 UUID 频道/GlobalPos/投影关系。`:92` 仅通过新 GlassStateSnapshot 解码，没有原始 TileEntity 迁移路径。
- 状态：数据模型替换已确认；旧世界的自动兼容不能宣称。项目当前 AGENTS 已明确 daily 旧身份实例只是原数据归位，不等于转换。原版世界与早期 port 世界是不同来源，后续验收须分别指定版本和备份，不能混用。此处不运行转换或修改世界。

### M04 / U04、U05：拓扑由单块频道状态改为独立网络外露面图

- 上游 `U/common/tileentity/TileEntityGlassBase.java:152` 限制 distance<40；`:186` 在 inactive 时接受频道，active 时只处理相同频道的更短距离或关闭。每块只有一个 channel/active/distance。`TileEntityGlassMaster.java:112` 由红石切换并从 distance=1 起传播，`:135` 有无线节点时禁止普通传播。
- 当前 `P/state/GlassServerState.java:260` 对每个投影器独立重算；`:290` 调 `GlassTopologySolver.solve`，输入仅方块类型与外露面判定，不读取其他网络占用。`P/core/GlassTopologySolver.java:29` 以正面为 distance=0 的虚拟种子，按同块凸角、相邻平面、对角凹角扩展，`:77` 最终省略内部面；`P/content/GlassContent.java:71` 将投影器/有线/无线扩展均认作可传播表面。
- `P/client/render/VulkanBlaze3DViewRenderer.java:970` 对每个网络提交表面，没有在这个消费入口消除不同网络的相同面。已确认旧单块排他条件不在新图的对应入口；相接的不同频道网络可能共享几何，最终覆盖/深度视觉结果待动态验证，不能从单网络测试推广到冲突网络。
- 变化类型：重写；普通平面和拐角有静态对应，但传播计数、范围、堵住种子及多网络竞争不等价。原因证据：`0f33e54:docs/reference/upstream-behavior.md` 明确外露面图和同 tick 合并重算；尚未找到接受跨网络重叠效果的记录。后续先核对反例，不在本轮修改算法。

### M05 / U07、U08：左键临时白遮罩传播确认缺失

- 上游 `U/common/block/BlockGlass.java:82` 在左击 active 显示块时设置 clickedPos、12 tick fadeout、2 tick fadePropagate、fadeDistance=2；`U/client/core/EventHandlerClient.java:54` 持续挖掘时重复刷新；`U/common/tileentity/TileEntityGlassBase.java:124` 把效果传到同频道 active 邻接块。它是用户触发的临时遮罩，不等同于红石启停。
- 已在当前全部 main Java/resources 搜索 attack/Attack、clickedPos、fadePropagate、fadeDistance、hitting、startDestroy、destroyBlock、left-click，并人工检查 `GlassClient` 的全部注册、`GlassClientInteraction` 两个接收器、block 层与 `glass.mixins.json` 全部列项：没有攻击输入到投影动画的接线。
- 当前 `P/client/render/GlassProjectionAnimation.java:17` 的 legacyFadeAnimation 只改变已有时间线的白罩外观；`VulkanBlaze3DViewRenderer` 的变化由 replaceViews/拓扑/启停/频道切换触发，未找到替代左击触发。
- 状态：在当前唯一实现的 Fabric 26.2 main 源码范围确认缺失；原因未知，不能写成用户接受删除。四个 planned Target 没有可独立承接此行为的实现。恢复与否应由用户根据原版交互价值决定；动态验收应独立测试左击与持续挖掘，不能只测红石动画。

### M06 / U08、U13：WorldPortals 已取得候选依赖源码，渲染替换不等于全量等价

- 独立参考 `D:\Workspaces\Repositories\GitHub\iChun\iChunUtil` 已固定 detached HEAD `5cf488107bc71e1fa5c0866e888f286bb3468e94`，clean。官方 commits API 截止 GLASS 基线日期的最近提交为此提交（2017-12-06）；其 build.gradle 声明 `1.12.2-7.1.3` / Forge `14.23.0.2493`，与 Initial port 记录一致。它是有依据的依赖候选，不是未锁定历史 deobf JAR 的唯一身份认定。已读其 README，仅作为来源说明，未执行其构建步骤。
- 下称 `D` 为该副本 `src/main/java/me/ichun/mods/ichunutil/common/module/worldportals/`。`D/client/render/WorldPortalRenderer.java:69` 在 stencil 可用、递归层数/每 tick 次数未超限且 portal.hasPair 时渲染；先写 stencil/清局部深度，调用 drawWorld，再恢复显示面的深度。GLASS 自身 `U/client/render/TileEntityGlassRenderer.java:123` 另有 renderLevel==0 与每帧 drawnChannels 限制，因此不把库的递归或传送能力视为 GLASS 实际产品入口。
- 当前 `P/mixin/client/GameRendererMixin.java:39` 声明 renderLevel 投影设置之后调用 capture，`GlassClient.java:22` 声明 AFTER_TRANSLUCENT_TERRAIN 合成。`VulkanBlaze3DViewRenderer.java:500` 禁止 capturing 递归；`:552` 确认同世界且区块已加载；`:675` 用投影器中心和终端后方中心变换相机，并通过独立 LevelExtractor/LevelRenderer/TextureTarget 捕获。`:626` 外层 finally 恢复相机/主 target/雾/粒子，`:780` 内层 finally 恢复主投影与全局数据；有明确异常日志和释放路径，但未实测异常注入或 GPU 生命周期。
- `GlassProjectionCompositor.java:56` 将捕获 target 绑定到主 target、使用对应 shader 和深度附件绘制；`:104` 定义 surface/wireless GPU pipeline。`VulkanBlaze3DViewRenderer.java:809` 单方向面且非无线的出口裁切策略、以及关闭动画延迟裁切，是原版 `TerminalPlacement.generateActiveFaces` 的对应替换；具体边界像素、透明顺序、实体/粒子/云、旁观者相机和资源重载仍待动态验证。
- 范围：本轮证明替换链有实际调用与资源绑定，不能证明 Minecraft 26.2 中所有 Mixin 注入点和 GPU 后端已执行。上游每 tick 渲染预算、1.0125 相机缩放、库级视锥/高度限制及异常路径，需逐项视作差异/未决，不能全部藏在“现代适配”内。

### M07 / U08：平面变换的“等价”注释超出已证范围

- 主任务对照 `D/common/portal/QuaternionFormula.java:87` 与 `P/core/GlassPortalTransform.java:34`，平面面向翻转、水平/垂直分支和位置旋转的四段共轭乘法有直接对应。
- 但依赖候选 `QuaternionFormula.java:19` 水平角减 `0.0125°`，`:33` 垂直角乘 `0.99`；当前 horizontal/vertical 使用精确 90°。旧姿态路径 `:112` 计算 Euler 差值，当前 transformOrientation 变换完整基底并重新构造四元数，cameraAngles/unwrapYaw 另作极点与跨 180° 处理。
- 因此当前源码头部“与 iChunUtil 7.1.3 等价”不能作为严格数值/所有观察行为等价的证据。分类为结构承接并重写部分数学策略；`0f33e54` 开发记录有精确六向相机的意图，但上/下方向、长距离视差、roll 和第三人称需要对照观测。没有运行数学测试或客户端以填补此缺口。

### M08 / U07、反向新增：GUI 与身份存在明确用户来源

- 主任务只读检索已引用旧任务的原始 JSONL：`019ff1d0-45c4-73f0-9cce-c9929bc270b0`，第 41322 行（2026-08-15T09:32:20.668Z）用户明确要求按节点直线距离排序、醒目标记可能未加载节点、可拖拽滚动条、统一按钮风格，以及无线面板可拖拽/缩放/旋转的真实世界轮廓预览、三角面广播和逐块播放。这为当前 GlassNodeDistance/GlassNodeList/GlassWorldPreviewWidget 的主动新增提供直接原因，不能当作原版缺失。
- 同任务第 45800 行（2026-08-24T19:08:12.252Z）的后续指令允许 GLASS package/namespace/group/artifact identity 迁移，禁止替换渲染技术栈或引入 Second Implementation 新架构。故当前 namespace 变化有明确授权来源；它不证明每项旧存档影响都完成转换或验收。
- 最小摘录保存为 `D:\Workspaces\Artifacts\GLASSPortAudit\20260910\historical-reason-records.json`，SHA-256 `a7975715aabbd2a22aadd4f8f31e4837fc01763705f61fe852789387032489a7`；记录原任务、时间和行号。历史指令仅用作原因证据，不作为本轮执行任何旧工作的授权。

### M09 / U02、U05：有测试的旧规则类没有当前产品接线

- 全量搜索当前 `src` 的 GlassChannelRules 与 GlassPropagationRules 引用：除声明外只在各自 core 测试出现；生产入口分别是 UUID 频道的 GlassServerState 和外露面图 GlassTopologySolver。
- 这两个类不是空实现，但属于当前产品入口不可达的旧规则辅助代码；存在对应测试不代表运行中的频道/传播仍使用它们。它们的保留原因未找到明确记录，不擅自删除，也不把其历史测试 PASS 当作新实现的语义证据。

### M10 / U08：核心 Mixin 目标做了依赖字节码静态核对

- 仅使用 JDK 的 javap 读取既有 Loom 缓存 JAR，没有执行 Minecraft/Mod 代码，也没有运行构建或产品测试。JAR 为 `.gradle/loom-cache/minecraftMaven/net/minecraft/minecraft-merged-043a8b3edf/26.2/minecraft-merged-043a8b3edf-26.2.jar`，37,396,303 字节，SHA-256 `0b8f29f21b6fe34800fa5514ce90fbb0e8013cafbf15afa18953bb53309aa590`；内嵌 version.json 记录 id=26.2、Java=25、protocol=776。
- 字节码中实际存在 `GameRenderer.renderLevel(DeltaTracker)`，其中 `RenderSystem.setProjectionMatrix(GpuBufferSlice, ProjectionType)` 调用与 GameRendererMixin 的目标 descriptor 相符；首个调用偏移 303。`LevelRenderer.invalidateCompiledGeometry(ClientLevel, Options, Camera, BlockColors)` 内有 `Options.getEffectiveRenderDistance()`，与对应 Redirect 目标相符；`getTransparencyChain()` 返回 PostChain。ShaderManager 的 `apply(Configs, ResourceManager, ProfilerFiller)` descriptor 也与 ShaderManagerMixin 声明相符。
- 静态转储保存在外部 `D:\Workspaces\Artifacts\GLASSPortAudit\20260910\MinecraftReference`，其 manifest.json SHA-256 为 `c53954cebf7120cb44bc1c9e94ccaea6fef3dd8e3f535c72b5384bdd4a1d0df2`。这是明确纳入的额外依赖证据，不把整个缓存算进 SourceSnapshot。
- 确认范围仅限上述三类关键方法/调用签名，未声明所有 13 Mixin 的运行时注入成功、GPU 管线完成或任何视觉验收。

### M11 / U01、U11、U13：旧更新通知接线没有当前替代

- 原版 `U/common/GeneralLaymansAestheticSpyingScreen.java:49` 在 preInit 向 iChunUtil UpdateChecker 注册 GLASS 的产品名/MC 版本/版本信息。候选依赖 `common/module/update/UpdateChecker.java:24` 保存登记项，`:29` 比较返回版本并写入更新集合/日志，`:64` 服务端启动时输出通知；`common/thread/ThreadGetResources.java:68` 是资源获取后调用 processModsList 的路径。
- 当前 main Java/resources 中未找到 UpdateChecker、版本检查或更新 URL 接线；`P/client/GlassModMenu.java` 仅提供配置界面 factory。当前已删除 iChunUtil 产品依赖且使用新产品身份，因此不能把 Mod Menu 安装或主页 URL 当成该能力的替代。
- 状态：原版的依赖级更新通知登记未承接；不涉及投影核心。去除原因仅能间接联系到依赖与身份替换，未找到明确取舍记录。是否需要新项目自己的更新通知由用户决定；本轮未访问旧更新服务，也未把候选库代码当成旧服务仍有效的证明。

### M12 / U06、U11：无线列表保留核心顺序，但阈值与编辑边界改变

- 上游 `TileEntityGlassMaster.canPropagate:135` 只要 wirelessPos 非空就关闭普通传播；`U/client/render/TileEntityGlassRenderer.java:198` 对恰好一个 Wireless 节点直接不绘面，至少两个节点时以 Projector 为首顶点画三角带并逐段显现。PacketSetProjector 可重复追加同一坐标，PacketWirelessOrder 只允许 inactive Master 替换列表。
- 当前 `GlassServerState.java:285` 先筛出已加载且类型正确的节点，只有数量达到配置 wirelessMinNodes 才进入无线模式；否则调用有线 solver 并向网络发送空无线列表。默认最少 2 个、最多 256 个，编码层绝对上限 4096（`GlassConfig.java:20`、`SetWirelessOrderPayload.java:24`）；保存/排序另检查重复项、同维度、方块和访问权限，未沿用 inactive gate。
- 具体静态反例：只链接 1 个有效 Wireless 节点时，原版普通传播已停且不形成三角面；当前默认配置退回有线面图。节点暂时未加载也可能改变 validWireless 的数量，从而切换模式。顺序与实时三角面存在对应，但这几个阈值/状态分支不等价。
- 原因证据：开发记录明确保留三角带顺序、允许配置和热切换；低于阈值退回有线是否是已接受设计未找到明确说明。动态验收须覆盖 0/1/2 节点、排序、重复请求、节点卸载再加载，不能只检查正常三节点面板。

### M13 / U02、U10、U13：网络分发边界已有静态证据

- 候选 iChunUtil 的 `common/core/network/AbstractPacket.java` 默认 requiresMainThread=true；`PacketHandlerWrapper.channelRead0` 检查该值后使用当前连接的 WorldThread，必要时 addScheduledTask，再调用 execute。GLASS 原始三个包没有覆盖这个默认值；执行坐标隐含在发送者的 getEntityWorld()，没有维度字段不等于能改任意其他维度。
- 当前 `P/network/GlassNetworking.java:28` 的四个 Fabric receiver 直接调用 GlassServerState；接收线程由 Fabric API 合同约束，不能只因 GLASS 本体没调用 execute 就判成网络线程写世界。对现有 Fabric API `0.156.0+26.2` 内嵌 networking 模块 `6.3.3+72073ef09e` 做了只读 javap：AbstractChanneledNetworkAddon.handle 在调用 receive 前检查 isOnReceiveThread，否则抛 RunningOnDifferentThreadException；ServerPlayNetworkAddon 的该检查使用 server.packetProcessor().isSameThread()，receive 才进入已注册 handler。
- 该模块 standalone 缓存与总 API 内嵌 JAR 的整体字节不同，但本次检查的两个 class 字节完全一致。转储、两 JAR/hash 与 class 对照保存在 `D:\Workspaces\Artifacts\GLASSPortAudit\20260910\NetworkingReference`，manifest.json SHA-256 `a9a99d7429a04a2bbfb154fdb01c431712c774285447a71827eb214d92a4d18c`。没有执行网络请求回放或 Minecraft 代码。
- 确认范围为接收入口、线程守卫与写入校验的位置；不推广成所有竞态、异常或恶意负载均安全。原版和现代都有已定位的线程边界，实现方式不同，实际服务器事件顺序仍需 D03/D04/D06 观察。

### M14 / U06、U10 / R12：拆除无线节点没有清除持久反向关联

- 主任务复核 C/U06-1：`P/content/block/GlassStateBlock.affectNeighborsAfterRemoval` 调 `GlassServerState.removed:142`，包含无线 surface；但 `GlassSavedState.remove:72` 只删除相同位置的 terminal/projector map 项。搜索所有 wirelessPositions/移除调用，未找到自动修改其他投影器列表的路径。
- `GlassServerState.validWireless:389` 只过滤本次 Network 的已加载有效节点，完整 Projector 列表仍保存在 SavedData，并由 snapshot 的 projectors 发送。原版 `TileEntityGlassMaster.update:96` 会从 wirelessPos 移除非无线 TE 坐标。
- 故已加载区域中明确拆除节点后，其持久关联仍存在；同位置重新放置无线节点会再次满足过滤条件。手工 setWirelessOrder 可以清理，但不是原版拆除自动清理的替代。静态行为差异及缺少该自动清理机制已确认，产品是否接受保留关联未知，未运行存档或 GUI 验证。

### M15 / U12、U11 / R13：声音创建条件与画面筛选不相同

- 总协调 GLS-C003 已独立核对同一 amb.ogg、注册/资源引用、音量 0.015/音高 0.6/循环和停止路径。本任务据 C 新反例复核调用条件：`VulkanBlaze3DViewRenderer.capture:514` 保留未过滤 currentPairs，另生成经过 observationDistance/视锥的 pairs；`:525` 把 currentPairs 交给 updateAmbience，`:1384` 遍历所有 pair 并创建/移动声音对象。
- `observedViews` 是 replaceViews 收到的当前维度全局网络视图，不是已经过距离过滤的集合；不能因其变量名含 observed 就推定声音受到观察距离限制。原版在 `TerminalPlacement.drawPlane:78` 首次实际绘面时创建声音；一旦 placement 留存，旧声音也不一定在每次不可见时立即停止。
- 静态确认的是首次创建/候选点与保留条件的变化。远端是否实际可闻、SoundManager 是否丢弃不可闻播放、资源成本多大均未测，不写成“全世界能听见”或已经发生性能问题。需按 D01/D06 对声音和画面分别观察。

### M16 / U08、U09 / R14：捕获失败时最后纹理的策略未显式绑定

- 主任务复核 C/U08-2 的可达场景：先成功捕获同维度终端；之后终端区块仅从本客户端卸载，而服务器因其他加载源仍保留该活动 network。`GlassChannelCaptureContext.ensure:104` 返回 false，调用方 `capture:552` 只 continue；context 的已有 target 不清空。
- `retainActiveChannels:1353` 仍保留 active network 的 context；`drawProjector:972` 只检查 context/target 非空，没有本次捕获成功标志。因此代码仍可提交最后纹理。最终画面与持续时间待动态验证；这不是所有失败情况都会保留同一旧帧的证明。
- 正常切维度有 switchLevel/reset，热切换另有旧/新 context handoff，不将这些路径与上述场景混为一谈。是否无数据时保留最后帧、显示空白或关闭，是需要明确的策略；不能无标记地把旧帧当作持续实时捕获的证据。

### M17 / U12、N08：旧 GUI 资源存在但没有现行消费入口

- 主任务复核 C/U12-1：资源仍包含 textures/gui/channel_set.png、wireless_order.png；全量 main Java/resources 的文件名/纹理路径检索和当前 GUI 绘制入口检查没有对应引用，四个 Screen 使用 GlassGuiTheme/控件/物品或世界轮廓绘制。fabric.mod.json 也未将这两文件用于图标入口。
- 两语言文件的 `block.glass.block_glass` 只保留为旧文字键，当前 GlassContent 只注册四个 `glass_terminal/projector/extension/wireless_extension` ID。它不是旧 Block 或旧存档解码器存在的证据。
- 这些是未消费的残留资源，与 B 的“当前引用指向的资源均能定位”并不矛盾：没有坏引用不等于每个资源都被使用。保留/删除原因未定，本轮不清理；资源图的真实加载/呈现仍属动态验收。

## 结束封存与本轮实际变更

- `D:\Workspaces\Artifacts\GLASSPortAudit\20260910\manifest-end.json`，SHA-256 `e2cfb8a3341da7037ec70965db02d7882863118ffecc7f9fc64434fd92c6efee`。与开始清单相比，所选 210 文件的新增/删除/内容变化均为 0；缺失 tracked 路径集合不变。
- 同目录 `verification-end.json`，SHA-256 `3aed42e3fd06d63ab006c915548687b36c08413e145c5131301d1451a98a186e`。HEAD、分支、remotes、index 内容清单及 tracked 工作树 patch 均未变化；staged patch 仍为 0 字节。独立 GLASS/iChunUtil 参考的 HEAD 与所选内容未漂移，工作树 clean。
- 最终本仓状态为 180 个 tracked 删除、7 个 tracked 修改、194 个未跟踪文件。相对开始仅增加五份 audit-port 文档，其中 status/upstream/mapping/review 四份由本任务及三子代理写入，coordination 由总协调写入。本轮未 stage、commit、push、改 Target/实现/资源/配置，未运行构建、产品测试、游戏或 GUI 验收。
- 源码清单刻意排除实例、缓存和本机私密资料；不声称本轮重新哈希所有 ignored 存档。外部新增仅为本轮审计快照/清单/静态转储/最小来源摘录，以及授权的独立参考仓库；历史迁移证据与实例数据未整理或清理。
- 文档本身不放入 source manifest，避免审计过程写记录造成伪漂移；交付时四份本任务文档的版本另记在外部 `audit-documents.json`。它不包含总协调仍可能更新的文档内容封印。

## 结论

历史结论按正文保留。迁移后的当前代码位置与本轮验证结果见 [构建迁移说明](../development/porting-template_migration.md)。
