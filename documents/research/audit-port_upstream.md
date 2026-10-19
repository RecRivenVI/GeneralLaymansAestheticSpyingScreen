# 旧 port GLASS 上游能力与实现审计

调查日期：2026-09-10。以下为迁移前审计的原始发现；2026-10-19 仅增加文档格式小节并迁移位置，未重新判定历史结论。

## 问题

核对旧 port GLASS 的移植状态和证据边界。

## 方法

按下列原始记录中的固定基线与只读方法调查。

## 发现

审计主体：iChun 原版 GLASS；本记录只建立上游事实，不宣布当前 port 的对应关系。

## 证据边界与固定基线

- 比较基线为 `99d9b4170a64d3692b46100d9d3e63cb41c735ee`（`Update for the change of iChunUtil`），因为当前 port 的 `Initial port` `0f33e54b45b37692c1fbf1fbc127ff46f368feea` 的直接父就是该提交。
- 独立参考副本：`D:\Workspaces\Repositories\GitHub\iChun\GeneralLaymansAestheticSpyingScreen`；remote 为 `https://github.com/iChun/GeneralLaymansAestheticSpyingScreen.git`，detached HEAD 与 live `master` 均为 `99d9b417…`，副本状态 clean，未运行构建、客户端或产品测试。
- 提交链从 `0558c3cdbd80b6fb7d17eb18e11dd722682aad0d` 到基线均可在副本中读取。各条能力的 `commit` 指首次引入或最后一次改变该语义的提交；不把提交主题当作运行证明。
- 状态标签：`静态对应已确认` 只表示上游源码/资源中确有该结构；`行为待动态验证` 表示线程、渲染、网络或游戏内结果不能由源码单独证明；`确认缺失` 表示上游没有所需机制；`定位但接入未确认` 表示调用点存在但依赖实现或运行接线尚未取得；`部分实现` 表示源码明确只覆盖一部分路径。

## U01 注册与内容

### U01.1 Mod 身份、生命周期与外部注册

- `commit`：`0558c3c…` 首次注册；`99d9b417…` 继续使用 iChunUtil 版本常量。
- `path/symbol`：`src/main/java/me/ichun/mods/glass/common/GeneralLaymansAestheticSpyingScreen.java:15-43` 的 `@Mod`、`preInit`；`common/core/ProxyCommon.java:16-31` 的 `preInit`；客户端覆盖在 `client/core/ProxyClient.java:12-24`。
- `trigger/state`：Forge 加载 Mod 后调用 `preInit`；代理先初始化 `WorldPortals`、注册四类 TileEntity、注册服务端事件监听器，再建立 `PacketChannel`。客户端代理随后注册客户端事件监听器及两个 TileEntity 特殊渲染器。
- `output/side effect`：建立全局 `LOGGER`、`channel`、`eventHandlerServer`，并在客户端建立 `eventHandlerClient`；注册 `GLASS_TEMaster`、`GLASS_TEBase`、`GLASS_TEWireless`、`GLASS_TETerminal` 四个 TE ID。`preInit` 还调用 `UpdateChecker.registerMod`。
- `dependency/exception`：源码显式依赖 Forge 1.12.2、iChunUtil 的 `Logger`、`iChunUtil`、`UpdateChecker`、`PacketChannel`、`WorldPortals`；Mod 注解要求 `ichunutil` 主版本的 `[VERSION_MAJOR.1.0, VERSION_MAJOR+1.0.0)`，接受远端版本 `[VERSION_MAJOR.0.0, VERSION_MAJOR.1.0)`，Minecraft 范围来自 `iChunUtil.MC_VERSION_RANGE`。`build.gradle:17` 只说明本地 `libs/iChunUtil-deobf.jar`，没有锁定下载坐标。依赖类具体版本和事件总线运行结果尚未由此副本动态证明。
- `status`：`静态对应已确认`；Forge/iChunUtil 接入和客户端真实初始化为 `行为待动态验证`。

### U01.2 GLASS 方块三种元数据与 TileEntity 角色

- `commit`：基础方块/Projector 为 `0558c3c…`；Wireless 变体在 `242d8d621ce24ac80731ddd0519b361f5f4be554`；当前最终接线在 `bcd69e1…` 及 `99d9b417…`。
- `path/symbol`：`common/block/BlockGlass.java:33-44,103-121,228-243` 的 `MASTER`、`WIRELESS`、`createNewTileEntity`、`getSubBlocks`、元数据映射；`common/item/ItemGlass.java:14-33` 的 subtype 名称。
- `trigger/state`：方块状态由 `master`/`wireless` 两个布尔属性承载；元数据 `0` 创建 `TileEntityGlassBase`，`1` 创建 `TileEntityGlassMaster`，`2` 创建 `TileEntityGlassWireless`。创造栏按 Projector、Wireless、Extension 顺序提供 metadata `1/2/0`。
- `output/side effect`：三种物品共用一个 `ItemGlass`，`getUnlocalizedName` 输出 `base/master/wireless` 后缀；Projector 可记录放置面，普通/无线块可参与对应活动状态。
- `dependency/exception`：`BlockGlass` 继承原版 `BlockGlass` 并实现 `ITileEntityProvider`；`breakBlock` 没有额外逻辑，依赖原版 TE 清理。状态变体的四个组合在 `assets/generallaymansaestheticspyingscreen/blockstates/block_glass.json:1-9` 都指向同一个玻璃模型；视觉角色来自 TE 渲染器而非方块模型。非法 metadata 会落入普通 Base 分支，未见显式拒绝。
- `status`：`静态对应已确认`；模型/TE 显示和非法 metadata 行为仍为 `行为待动态验证`。

### U01.3 Terminal 方块与注册资源入口

- `commit`：`b0e87292d90259a1f6b7f88c0b8bc42e1d85bc5f` 引入 Terminal 方块、TE 和注册；`65ca32a…` 完成客户端显示接线。
- `path/symbol`：`common/core/EventHandlerServer.java:18-40` 注册 `block_glass`、`block_glass_terminal`、对应 ItemBlock 和 `soundAmb`；`common/block/BlockGlassTerminal.java:28-101`；`common/tileentity/TileEntityGlassTerminal.java:13-90`。
- `trigger/state`：Registry 事件创建 GLASS 玻璃方块与硬度 `0.8F`，Terminal 为 Obsidian 子类、硬度 `50.0F`、抗性 `2000.0F`；右键 Terminal 在客户端打开设置 GUI。放置时服务端把 `facing` 设为放置者方向。
- `output/side effect`：Terminal 不透明/完整方块检查均为 false，但掉落自身 ItemBlock；客户端特殊渲染器画原版 Obsidian 方块并在其前方画缩小玻璃模型。
- `dependency/exception`：Terminal 总是创建 `TileEntityGlassTerminal`，metadata 被忽略；`onBlockActivated` 无论客户端/服务端都返回 true，服务端没有权限检查。`assets/.../blockstates/block_glass_terminal.json:1-5` 和 `models/block/block_glass_terminal.json:1-6` 只引用 `obsidian`；Item 模型在 `models/item/block_glass_terminal.json:1-3`。
- `status`：`静态对应已确认`；GUI、网络同步、渲染接线为 `行为待动态验证`。

## U02 频道与权限

### U02.1 PacketChannel 与三类服务端包

- `commit`：`b0e8729…` 建立 `PacketChannel("GLASS", PacketSetChannel.class)`；`8bfa3a8c99a84c6044c70ea3a32102718af05f` 增加 `PacketSetProjector`；`bcd69e1e3d79b18198a5d4787d88b485b193482c` 增加 `PacketWirelessOrder`。
- `path/symbol`：`common/core/ProxyCommon.java:27-30`；`common/packet/PacketSetChannel.java:15-71`；`PacketSetProjector.java:16-63`；`PacketWirelessOrder.java:17-73`。
- `trigger/state`：客户端 GUI 发送包；三类包均 `receivingSide() == Side.SERVER`。`PacketSetChannel` 编解码 BlockPos+UTF-8 字符串；`PacketSetProjector` 编解码两个 BlockPos；`PacketWirelessOrder` 编解码起点 BlockPos、整型数量和若干 BlockPos。
- `output/side effect`：服务端按坐标找到 TE，修改 `channelName`、`setChannel` 或 `wirelessPos`，调用 `markDirty()` 与 `notifyBlockUpdate(..., 3)`。
- `dependency/exception`：具体序列化/分发/线程切换依赖 iChunUtil `AbstractPacket`/`PacketChannel`，源码未提供长度上限、异常恢复或包版本校验。`PacketSetChannel` 对 Terminal 无活跃状态门槛，对 Master 仅允许 `!active`；另外两包只检查目标坐标的 TE 类型，没有检查发送者是否拥有该方块、距离、世界/区块、列表长度、重复项或 Wireless 与 Master 的关系。客户端负载可直接请求任意已加载坐标，越界/坏数量的解码行为未验证。
- `status`：包类和服务端接收侧 `静态对应已确认`；iChunUtil 分发以及真实权限结果为 `行为待动态验证`。源码层面明确存在“UI 过滤但服务端未复核”的权限缺口，不能把公开/私有前缀当作强制访问控制。

### U02.2 频道命名与公开/私有筛选

- `commit`：`b0e8729…` 引入 Terminal/Projector 频道设置；当前筛选随 `65ca32a…`、`8bfa3a8…` GUI 接线完成。
- `path/symbol`：`client/gui/GuiChannelSetter.java:17-173`；`GuiChannelSetterProjector.java:22-194`；`GuiProjectorSetter.java:21-168`；`GuiChannelList.java:12-69`。
- `trigger/state`：Terminal GUI 文本最大长度为 15，确认时前缀为 `public:` 或 `玩家名:`；Projector 频道列表从客户端 `terminalLocations` 中选择 `public:` 或本地玩家名开头的频道；Wireless 选择器从已加载 Master 中筛选空频道、公开频道或本地玩家名开头频道。
- `output/side effect`：选择后发送 `PacketSetChannel`；Projector 已激活时不能从该 GUI 设置频道，因为包端拒绝 active Master；Terminal 设置无此门槛。
- `dependency/exception`：前缀匹配只在客户端 GUI 中执行，`startsWith` 使用玩家当前名称；Terminal 初始化公开状态用 `contains("public")`，不是严格前缀；频道 Map 在客户端以原始字符串为 key，而运行传播常以 `equalsIgnoreCase` 比较，因此大小写差异可能产生多个客户端 Map 条目，是否可观察需动态验证。文本长度限制只在 GUI 设置，包端未复核。
- `status`：命名与 GUI 筛选 `静态对应已确认`；服务端强制权限为 `确认缺失`；大小写碰撞和超长/恶意负载结果为 `行为待动态验证`。

## U03 终端

### U03.1 Terminal 作为远端观察锚点

- `commit`：`b0e8729…` 引入 Terminal；`65ca32a…` 将活动 GLASS 与 Terminal 连接为投影；`797d5a9608ab10ab211f229095efcdc30088f9f4` 增加声音和释放逻辑。
- `path/symbol`：`common/tileentity/TileEntityGlassTerminal.java:18-44,47-90`；`client/core/EventHandlerClient.java:28-35,151-195,223-259`；`common/block/TerminalPlacement.java:24-57`。
- `trigger/state`：客户端 TE `onLoad` 把非空 `channelName -> BlockPos` 放入 `terminalLocations`；卸载或状态刷新移除；世界卸载/断线清理。活动 GLASS TE 由 Base/Master 的 `onLoad`、`onChunkUnload`、`shouldRefresh` 加入/移出 `activeGLASS[channel]`。
- `output/side effect`：渲染活动 GLASS 时 `getTerminalPlacement(channel)` 先寻找同频道 Terminal，再在活动集合中寻找 `active && distance == 1 && base instanceof TileEntityGlassMaster` 的 Master；满足后创建一次 `TerminalPlacement` 并缓存。目标不可用时写入值 `13` 的 tick 重试超时。
- `dependency/exception`：`terminalLocations` 与 `activeGLASS` 是客户端内存 Map，不是服务端索引；同频道多个 Terminal 会因 `put` 覆盖同一 key；同频道活动集合为空时没有投影；Master/Terminal 位于未加载区块时只表现为暂时找不到。实际区块加载顺序、13 tick 重试和释放调用依赖 Forge 客户端事件及 WorldPortals。
- `status`：TE 状态同步和锚点查找 `静态对应已确认`；实际显示、区块边界和多 Terminal 竞争为 `行为待动态验证`。

## U04 投影与拓扑

### U04.1 普通有线传播的面图与延迟

- `commit`：普通传播初始接线 `cf9502f6f86ff57eada97cb99e87bc63d445a465`；多侧/拐角修正 `32fecc4226af9c3dac618708c5b64e311e83b132`；延迟衰减/无线变体 `242d8d6…`；最终面处理继续由 `9e92b77…`、`d75263e…`、`8ce6636…` 调整。
- `path/symbol`：`common/tileentity/TileEntityGlassBase.java:23-37` 的 `PROPAGATION_FACES`；`update():84-122`；`propagate():152-184`；`bePropagatedTo():186-228`；`checkFacesToTurnOn():230-302`；`common/block/BlockGlass.java:124-225` 的邻居/红石处理。
- `trigger/state`：每个 Base 保存 `activeFaces`、`active`、`channel`、`distance`、`propagateTime`。从 Master 的 `placingFace` 种子开始，`PROPAGATION_FACES` 对每个活动面选择所有不同轴向的四个面；服务端每两 tick 执行一次 `propagate`，相邻 Base 在距离更短或尚未激活时激活，并计算面集合。
- `output/side effect`：传播改变 Base 的 active/channel/distance/activeFaces，设置 `fadeoutTime=12`、`propagateTime=2`，并通知方块更新。关闭传播在找到更近同频道来源前延迟 `PROPAGATE_TIME + 1`，否则关闭并清空 channel/distance；邻居增删会重新检查起点和活动面。传播上限为 `distance < 40`，Master 在 `wirelessPos` 非空时覆盖为不可继续普通传播。
- `dependency/exception`：拐角算法区分 inner/outer corner，按相邻 TileEntity 与坐标向量增加面；垂直来源时尝试优先水平面。`activeFaces` 使用 ArrayList，去重只对局部 `HashSet` 新面完成；邻居缺失、区块未加载、大小写频道、删除期间的延迟重算都可能改变结果。算法有 `TODO fix this` 的历史注释，不能从源码推断所有几何情形均正确。
- `status`：面级传播与 2 tick/40 距离限制 `静态对应已确认`；拓扑几何、并发邻居更新、关闭时序和视觉连贯性 `行为待动态验证`。

### U04.2 TerminalPlacement 成对平面拓扑

- `commit`：`65ca32a…` 首次把 TerminalPlacement 连接到实际 WorldPortal；`99d9b417…` 将上游旧 `renderAll` 改为 `setCullRender(cullRender)`。
- `path/symbol`：`common/block/TerminalPlacement.java:24-57,59-75,100-121,166-187`。
- `trigger/state`：创建时源平面位于 `master.pos + (0.5,0.5,0.5)`，朝 `master.placingFace`；成对目标平面位于 `terminal.pos.offset(terminal.facing,-1) + 0.5`，朝 `terminal.facing.opposite()`，上方向均为 `EnumFacing.UP`。`setPair` 在两端互相建立关系。
- `output/side effect`：`generateActiveFaces` 汇总活动块面；无线列表非空或活动面超过一个时设为不裁切，否则 `setCullRender(true)`。`drawPlane` 在目标端按活动块相对 `renderCaller` 的坐标绘制 GLASS 平面。`canCollideWithBorders=false`、`canTeleportEntities=false`、`getPlaneOffset=0`。
- `dependency/exception`：父类为 iChunUtil WorldPortals `WorldPortal`；`createFakeInstance(NBTTagCompound)` 永远返回 `null`，默认构造器只用于成对实例，无法由此证明跨世界/序列化恢复路径存在。注释掉的旧代码曾尝试为目标平面生成更多面，但基线中没有执行。
- `status`：同世界成对平面和裁切开关 `静态对应已确认`；WorldPortals 父类注册、实际裁切和实体/边界行为 `行为待动态验证`；WorldPortal fake-instance 恢复 `确认缺失`。

## 第一批里程碑（2026-09-10）

- 已完成独立上游副本和基线校验：目标仓库 live `master` 未偏离 `99d9b417…`，因此本记录不混入基线后的提交。
- 已落盘 U01–U04 的第一批上游事实：注册/内容、频道与 UI 权限边界、Terminal 锚点、普通传播和 TerminalPlacement 拓扑；其中 U02 的服务端权限复核明确缺失，U04 的 fake-instance 与跨世界恢复明确缺失。
- 待补：U05 有线扩展、U06 无线扩展、U07 GUI 交互、U08 渲染、U09 跨维度/远端世界、U10 状态持久化与网络、U11 配置/限制/生命周期、U12 资源/配方/声音、U13 iChunUtil/WorldPortals 外部边界；另需把上游依赖 URL/版本线索和基线后历史变更单列。

## U05 有线扩展

### U05.1 Base Extension 的邻接传播

- commit：Base 的多面传播由 cf9502f6 引入，侧向/拐角处理在 32fecc42、9e92b77、d75263e 继续修正。
- path/symbol：common/block/BlockGlass.java:124-225 的 neighborChanged；common/tileentity/TileEntityGlassBase.java:23-37,152-228,230-302 的 PROPAGATION_FACES、canPropagate、propagate、bePropagatedTo、checkFacesToTurnOn。
- trigger/state：普通 metadata 0 创建 TileEntityGlassBase。Master 红石开启后以 placingFace 作为首个活动面；每个 Base 保存频道、最短距离、活动面列表和传播计时。活动面按轴向选出四个不同轴向面，服务端在 propagateTime 归零时访问对应邻居。
- output/side effect：未激活同频道 Base 在 distance == 0 或更远时被激活；已激活同频道 Base 若发现更短来源会更新距离和面图；状态变化通知方块更新并设置 12 tick 的淡出与 2 tick 的下一步传播。普通模式距离达到 40 后停止继续扩散。
- dependency/exception：传播只检查相邻坐标的 TileEntityGlassBase 和频道/距离，未检查材质、方块状态、区块是否已加载或发送者。关闭传播保留旧 channel/distance 到传播分支结束，延迟分支把关闭重新评估推迟 3 tick。算法同时读写 ArrayList<EnumFacing>；提交 7584db9 说明曾因非线程安全 ArrayList 崩溃，最终只在 WorldPortal 面汇总处改为先取 HashSet，不能视为整体线程安全。
- status：邻接扩展的数据结构和触发链为静态对应已确认；距离、关闭竞态、区块边缘和真实可视连续性为行为待动态验证。

### U05.2 邻居增删、挖掘与面重建

- commit：邻居增删分支随 cf9502f6/32fecc42 接入；挖掘淡出由 242d8d6 引入，最终客户端重试/清理由 65ca32a、797d5a9 保留。
- path/symbol：BlockGlass.neighborChanged:134-209；BlockGlass.onBlockClicked:81-100；EventHandlerClient.onClientTick:47-86；TileEntityGlassBase.fadePropagate:124-150。
- trigger/state：服务端邻居变化先从活动面推导候选方向；删除方块时查找更近同频道活动块，找不到则让当前 Base 关闭，找到则调用 checkFacesToTurnOn。客户端开始挖掘活动 GLASS 时记录 clickedPos，把当前及附近同频道块的 fadeoutTime 设为 12、fadeDistance=2，并每两 tick 延迟传播淡出。
- output/side effect：客户端只改变淡出可见状态和渲染面，服务端邻居处理改变活动/距离/频道；fadeoutTime 归零且块已关闭时清空 activeFaces。notifyBlockUpdate 用于状态同步。
- dependency/exception：BlockGlass.neighborChanged 中 new base placed 和 block was removed 分支的 fromPos/pos.offset(...,-1) 方向约定依赖原版邻居事件顺序；源码有 TerminalPlacement.generateActiveFaces 的 TODO fix this，没有独立拓扑验证器。客户端点击处理只在持续命中同一 TE 时保留，离开或停止挖掘即清除点击位置。
- status：关闭淡出和邻居触发为静态对应已确认；复杂拐角、被挖断后新来源重选和客户端/服务端时序为行为待动态验证。

## U06 无线扩展

### U06.1 Projector 到 Wireless Extension 的坐标链

- commit：Wireless 变体/入口由 242d8d6 引入；Projector 选择链由 8bfa3a8 引入；完成排序与显示由 bcd69e1、8ce6636 接入。
- path/symbol：common/tileentity/TileEntityGlassMaster.java:16-24,26-64,88-110,134-171；common/tileentity/TileEntityGlassWireless.java:3-13；common/packet/PacketSetProjector.java:16-63；common/packet/PacketWirelessOrder.java:17-73。
- trigger/state：Wireless metadata 2 创建 TileEntityGlassWireless；PacketSetProjector 把 Wireless 坐标追加到 Master 的 wirelessPos，PacketWirelessOrder 在 Master 未激活时用客户端排序后的列表整体替换。Master 每 tick 移除目标位置上不再是 Wireless TE 的坐标；激活且频道等于 setChannel 时递增 wirelessTime，否则归零。
- output/side effect：Master 的无线列表决定一条按列表顺序连接的三角带；列表非空时 canPropagate 返回 false，因而不会再走普通邻接传播。客户端 Master 加载活动状态时遍历列表，把 Wireless 的 users 加一并加入同频道 activeGLASS；卸载/刷新时反向移除。Master 渲染包围盒扩展到整个坐标列表。
- dependency/exception：PacketSetProjector 的最终 baseline 版本没有重复坐标检查，PacketWirelessOrder 没有数量上限、同世界/同区块验证、距离限制或所有权验证；删除目标只按 TE 类型清理。无线列表可跨很远坐标，但源代码没有跨维度字段。TileEntityGlassWireless 只新增 ticks/users，没有自己的链接/频道持久化字段。
- status：链接列表、顺序和普通传播短路为静态对应已确认；Wireless 初次激活、列表非法项、多目标重载和实际三角带结果为行为待动态验证。

### U06.2 无线显示与传播动画

- commit：无线平面首次绘制于 bcd69e1；按顺序分段揭示和 wirelessTime 动画由 8ce6636 引入。
- path/symbol：client/render/TileEntityGlassRenderer.java:185-244 的 drawPlanes 无线分支；:89-99 的 Ender Crystal 标记；common/tileentity/TileEntityGlassMaster.java:102-109 的 wirelessTime。
- trigger/state：Master 存在无线列表且 setChannel.equalsIgnoreCase(channel) 时不绘制普通单面；列表长度为 1 直接返回，长度大于 1 才从 Master 面的 pushback 顶点开始按 wirelessPos 顺序发出 GL_TRIANGLE_STRIP。激活时仅当 wirelessTime > (i-1)*2 才显示第 i 段，否则颜色 alpha 为零；关闭时使用当前淡出 alpha。
- output/side effect：Wireless TE 的 Ender Crystal 旋转速度为 ticks *(1.2 + users* 10)；Master 的有线平面被替换为按坐标差构造的三角带。无线坐标列表的顺序直接决定几何连接顺序，GUI 中可预览、删除和上下移动。
- dependency/exception：顶点以 Master 相对坐标和 placingFace pushback 修正，未检查线段与方块是否相交或目标是否可见；只有一个 Wireless 时静态绘制函数不产生三角形。WorldPortal 的源/目标面仍由 TerminalPlacement 处理，Wireless 不是独立的远端世界。
- status：三角带、顺序动画和标记引用为静态对应已确认；OpenGL 状态、单目标空显示、遮挡与远端合成为行为待动态验证。

## U07 GUI 与交互

### U07.1 Terminal 与 Projector 频道设置

- commit：b0e8729 首次引入两种频道设置屏幕；65ca32a、bcd69e1 增加列表与无线入口。
- path/symbol：client/gui/GuiChannelSetter.java:17-173；GuiChannelSetterProjector.java:22-194；GuiChannelList.java:12-69；入口为 BlockGlassTerminal.openGui、BlockGlass.openGui(TileEntityGlassMaster)。
- trigger/state：右键未激活 Projector 或任意 Terminal 在客户端打开 GUI；两屏幕均暂停游戏。Terminal 文本框最大 15 个字符，可切换公开/私有；Projector 只能从 eventHandlerClient.terminalLocations 中的公开或本地玩家前缀频道选择，不能手工输入新频道。列表单击选中，双击确认，Projector 还提供进入无线顺序屏幕的按钮。
- output/side effect：Terminal 确认发送 PacketSetChannel(terminal.pos, public: + name) 或 玩家名:name；Projector 确认发送选中的完整频道字符串；关闭屏幕不发送未确认值。
- dependency/exception：列表完全依赖客户端已加载 Terminal 及其 TE 更新，未加载/过期时为空；GuiChannelSetter 用 contains("public") 判公开状态，Projector 列表显示用 startsWith("public")，筛选与标签并非同一严格条件。GUI 事件没有服务端回执或失败提示。
- status：入口、文本、前缀和包发送为静态对应已确认；列表时序、空列表用户体验和服务端接受结果为行为待动态验证。

### U07.2 Wireless Projector 选择屏幕

- commit：选择入口由 8bfa3a8 引入，列表显示随 bcd69e1 完成。
- path/symbol：client/gui/GuiProjectorSetter.java:21-168；GuiProjectorList.java:13-67；入口为 BlockGlass.openGui(TileEntityGlassWireless):252-255。
- trigger/state：右键未激活 Wireless Extension 时扫描 Minecraft.getMinecraft().world.loadedTileEntityList，收集 TileEntityGlassMaster 且 setChannel 为空、公开或以本地玩家名开头的 Projector。单击/双击选择目标，确认发送两个 BlockPos。
- output/side effect：客户端关闭 GUI，服务器收到后向 Master 的 wirelessPos 追加 Wireless 坐标并通知更新。最终 baseline 未保留早期 contains 去重检查，因此重复确认同一项会产生重复坐标（源代码静态可见，真实 GUI 复现未做）。
- dependency/exception：筛选只看客户端已加载列表和字符串前缀，不检查目标是否属于玩家、距离、维度或是否已有其他 Master 链接；服务端包也只检查两个坐标的 TE 类型。屏幕没有从服务器刷新可用目标的请求。
- status：选择与包接线为静态对应已确认；重复坐标、远距/跨世界目标及多客户端竞争为行为待动态验证。

### U07.3 Wireless 顺序与预览

- commit：bcd69e1 引入完整 GuiWirelessOrder；8ce6636 增加自动旋转、拖动与释放鼠标判断。
- path/symbol：client/gui/GuiWirelessOrder.java:28-315；GuiWirelessList.java:14-62。
- trigger/state：从 Projector 频道屏幕进入；初始复制 master.wirelessPos。Delete 删除选中坐标；^/v 调整顺序；鼠标滚轮缩放，预览区域拖动旋转，未拖动时自动 rotateX += 2.5F。确认发送完整坐标列表，不要求当前有选中项。
- output/side effect：预览使用 RendererHelper 绘制灰底、scissor 和按坐标确定的随机颜色三角带；确认后服务端整体替换 Master 列表。删除后索引减一，没有额外钳制；列表为空仍可确认并清空无线链。
- dependency/exception：预览直接使用本地列表，GUI 内 scale/rotate 状态不写入世界；GuiWirelessList 仅按坐标显示，没有校验目标仍存在。渲染 API、鼠标坐标换算和 OpenGL 状态恢复依赖 Forge/iChunUtil，源码没有动态异常处理。
- status：排序/删除/预览和全量提交为静态对应已确认；空列表、边界索引、输入法/鼠标异常及服务端负载处理为行为待动态验证。

## U08 渲染

### U08.1 TE 标记、面几何与淡出

- commit：基础面绘制由 cf9502f、32fecc4、3b0e065 逐步形成；Projector/无线标记和最终全局 TE 渲染器在 65ca32a、bcd69e1 完成；淡出 alpha 在 8ce6636 使用 partial tick。
- path/symbol：client/render/TileEntityGlassRenderer.java:28-121 的 render；:185-271 的 drawPlanes；client/render/TileEntityGlassTerminalRenderer.java:18-59。
- trigger/state：ProxyClient.preInit 为 Base（覆盖 Master/Wireless 子类）和 Terminal 绑定特殊渲染器。每次 Base 渲染设 lastDraw=8，关闭照明、启用 alpha blend 和满亮 lightmap；Master 旋转并绘制原版 Beacon，Wireless 缩放并绘制原版 Ender Crystal。普通面按 activeFaces 逐面绘制约 1.002 方块大小的彩色 quad，并尊重 shouldSideBeRendered。
- output/side effect：fadeoutTime 以 sqrt(clamp((fadeoutTime-partial)/12)) 计算 alpha；GL 状态在结束时恢复照明、纹理、normalize、blend、alpha 阈值和 lightmap。Terminal renderer 仅在 renderLevel==0 绘制原版 Obsidian 底和缩小玻璃图标。
- dependency/exception：这是旧 1.12.2 OpenGL immediate-mode GlStateManager.glBegin/Tessellator 路径，没有自有 shader 或 RenderTarget。isGlobalRenderer 恒为 true，包围盒过小或大范围 Wireless 仅依赖 TE 自己的覆盖实现；真实 GL 状态和显卡兼容性未动态验证。
- status：面几何、标记、淡出公式为静态对应已确认；渲染线程、遮挡、透明排序和设备结果为行为待动态验证。

### U08.2 频道级远端画面合成

- commit：WorldPortal 画面接线由 65ca32a 引入；797d5a9 修正视锥释放；99d9b417 改变 cull 开关 API。
- path/symbol：client/render/TileEntityGlassRenderer.java:123-183 的 drawScene；common/block/TerminalPlacement.java:43-98 的 drawPlane；client/core/EventHandlerClient.java:151-249 的 placement/cache。
- trigger/state：同一 RenderTick 首次遇到活动频道且 WorldPortalRenderer.renderLevel == 0 时，drawnChannels 记录该频道；查找/创建 TerminalPlacement 后，将当前 TE 设为 renderCaller，计算观察实体相对 Master 中心的位置和 yaw/pitch/roll 的旋转数组，再调用 WorldPortalRenderer.renderWorldPortal。
- output/side effect：在 Portal 渲染前，按活动块相对 renderCaller 的坐标绘制源侧 GLASS 面；Portal 调用完成后恢复 GL 状态。活动频道每帧最多触发一次，避免同一频道的多个 TE 重复远端合成。
- dependency/exception：实体平移/旋转使用 iChunUtil EntityTransformationStack 和 EntityHelper；平面旋转使用 WorldPortal getQuaternionFormula；远端画面、视锥和裁切由外部 WorldPortalRenderer 控制。placement 缓存按频道字符串索引，多个大小写变体可能形成不同缓存。
- status：调用链与每频道一次的静态门控为静态对应已确认；相机视差、透明深度、WorldPortal 实际绘制及视锥状态为定位但接入未确认/行为待动态验证。

### U08.3 递归抑制与资源释放

- commit：频道去重/世界清理在 65ca32a 接入；释放视锥与声音在 797d5a9；ArrayList 崩溃修复相关面汇总在 7584db9。
- path/symbol：EventHandlerClient.onRenderTick:37-44；drawScene:125-132；onWorldUnload:110-147；removeGlass:223-249；disconnectFromServer:252-259；SoundGlassAmbience.isDonePlaying:34-38。
- trigger/state：每个渲染 tick 开始清空 drawnChannels；进入 Portal 渲染后 renderLevel 不再满足 0，因此嵌套场景不会再次进入 drawScene。活动集合为空、世界卸载或断线时释放 placement pair 的 view frustum 并清空缓存。
- output/side effect：递归 GLASS 不会继续创建同频道新 placement；断线/世界切换移除声音判定所引用的 placement。世界卸载事件在 mc.world != null 时只移除失效 TE，mc.world == null 时才全量清理并释放视锥。
- dependency/exception：递归保护依赖 WorldPortalRenderer 的全局 renderLevel 正确递增/恢复；TE 的 activeGLASS 增删回调通过 Minecraft.addScheduledTask 排到客户端线程，而 render tick/client tick 直接操作 drawnChannels 与其他 Map。该状态机没有独立断言，真实嵌套/断线序列待动态验证。
- status：源码中明确有递归门控和释放路径为静态对应已确认；异常退出、视锥泄漏、跨线程顺序和嵌套 Portal 结果为行为待动态验证。

## U09 跨维度与远端世界

### U09.1 上游实际存在的远端画面范围

- commit：65ca32a 的 TerminalPlacement/drawScene 是该能力的最终基线实现。
- path/symbol：common/block/TerminalPlacement.java:34-57；client/render/TileEntityGlassRenderer.java:136-170；client/core/EventHandlerClient.java:159-183。
- trigger/state：源 Master 和目标 Terminal 都从当前客户端 mc.world 取得，TerminalPlacement(World world, ...) 的 source 与 pair 均传同一个 World；相机只通过 Quaternion/EntityTransformationStack 映射到 Terminal 后方坐标。
- output/side effect：玩家得到一个从 Projector 面看到 Terminal 面的实时同世界画面，位置变化产生变换后的观察偏移；没有独立远端客户端世界、维度 ID、服务器传送或跨维度网络包。
- dependency/exception：WorldPortal 父类可能提供同世界视锥/裁切能力，但 GLASS 自己没有选择第二 World 的字段或构造路径。createFakeInstance(NBTTagCompound) 在 TerminalPlacement.java:182-187 永远返回 null，所以源码没有可恢复的 fake/序列化远端实例。
- status：同世界实时变换画面为静态对应已确认；跨维度远端世界为确认缺失；WorldPortals 是否额外支持不同维度而未被 GLASS 调用属于外部依赖边界。

### U09.2 递归/自观察边界

- commit：递归抑制随 65ca32a 的 renderLevel==0 门控和后续 drawnChannels 清理稳定在 baseline。
- path/symbol：TileEntityGlassRenderer.drawScene:125-132；TileEntityGlassTerminalRenderer.render:23-25；EventHandlerClient.onRenderTick:37-44。
- trigger/state：初始世界渲染只处理每个频道一次；WorldPortal 进入子渲染层后，Base/Terminal renderer 看到非零 renderLevel 不再开启新场景。
- output/side effect：防止 GLASS 远端画面递归套入自身；同一帧多个活动 TE 共享频道的画面不会重复提交。
- dependency/exception：该保护是渲染层级和客户端 HashSet 的静态机制，不是服务端权限或世界级防递归标记。WorldPortal 层级若异常未恢复，后续画面可能永久跳过，需动态验证。
- status：递归门控为静态对应已确认；实际嵌套 Portal/异常恢复为行为待动态验证。

## U10 状态、持久化与网络

### U10.1 TileEntity 状态与 NBT 字段

- commit：基础 NBT 在 0558c3c；活动面/更新接线在 cf9502f、65ca32a；Master 无线字段在 8bfa3a8；最终字段在 baseline。
- path/symbol：common/tileentity/TileEntityGlassBase.java:304-364；TileEntityGlassMaster.java:140-171；TileEntityGlassTerminal.java:47-90。
- trigger/state：Base 写入 activeFaces、activeFace_i、active、channel、distance、propagateTime、fadeoutTime；Master 追加 powered、setChannel、placingFace、无线坐标数量及 wPx_i/wPy_i/wPz_i；Terminal 写入 channelName、facing。
- output/side effect：读回时先清空面列表/无线坐标列表再按计数恢复；Base/Terminal 提供 getUpdateTag 和 SPacketUpdateTileEntity，接收更新时先 onChunkUnload、读 NBT、再 onLoad，从而重建客户端 activeGLASS/terminalLocations 索引。
- dependency/exception：Wireless 的 ticks/users、Master 的 wirelessTime/rotationBeacon、Base 的 fadePropagate/fadeDistance/lastDraw 不写入 NBT；它们是运行时瞬态。NBT 计数来自数据本身，没有数量上限或版本迁移字段，坏数据的循环/内存影响未验证。
- status：字段和更新包为静态对应已确认；世界存档加载、坏 NBT、客户端重连和瞬态重建为行为待动态验证。

### U10.2 三类配置包与服务端更新

- commit：b0e8729 引入 PacketSetChannel；8bfa3a8 引入 PacketSetProjector；bcd69e1 引入 PacketWirelessOrder。
- path/symbol：三个 common/packet/*.java 的 writeTo/readFrom/execute/receivingSide；注册在 common/core/ProxyCommon.java:27-30。
- trigger/state：客户端 GUI 发往 PacketChannel("GLASS", ...)，包只接收于 Side.SERVER。服务端用发送者当前 EntityWorld 按坐标取 TE，改变字段后 markDirty 和 notifyBlockUpdate。
- output/side effect：服务器状态写入 TE 并通过 vanilla TE update tag 回送客户端；配置包本身不带维度、世界 UUID、玩家权限或请求序列号。
- dependency/exception：序列化/网络线程和异常处理完全依赖 iChunUtil AbstractPacket/PacketChannel。PacketWirelessOrder.readFrom 读取原始 int 数量后循环，包端没有上限；服务端没有 sender ownership、距离、区块、目标关系或频道规范校验。源码未提供拒绝消息。
- status：网络字段、接收侧和更新通知为静态对应已确认；实际分发、竞争覆盖、恶意负载与客户端回放为行为待动态验证；服务端权限复核为确认缺失。

### U10.3 服务端持久化范围

- commit：baseline。
- path/symbol：全仓源码仅见 TE NBT 实现与 markDirty；未见 WorldSavedData、自定义服务器保存对象、命令或跨维度索引。
- trigger/state：每个 Master/Terminal/Base 的状态随所在方块的 TileEntity 保存；无线链接是 Master 内的绝对 BlockPos 列表。
- output/side effect：存档/区块保存与 vanilla TE 生命周期共同决定状态恢复；客户端索引在 TE load/update 回调中重建。
- dependency/exception：缺少全局频道注册表，频道发现只在客户端已加载 Terminal 上构建；跨区块/跨维度链接没有独立持久化验证。不同 Master 可以写相同频道而无服务器仲裁。
- status：TE 级持久化为静态对应已确认；全局频道/跨维度状态为确认缺失；存档重载行为为行为待动态验证。

## U11 配置、限制与生命周期

### U11.1 可观察限制与时间常量

- commit：242d8d6 引入淡出/传播时间；cf9502f/32fecc4 建立距离传播；65ca32a 引入 placement 重试；8ce6636 引入无线分段计时。
- path/symbol：TileEntityGlassBase.java:39-51；canPropagate:152-155；EventHandlerClient.java:151-195；GuiChannelSetter.java:51-56；TileEntityGlassRenderer.java:99,187-220。
- trigger/state：固定 FADEOUT_TIME=12、PROPAGATE_TIME=2、普通传播 distance < 40、placement 不可用时 13 tick 后再试、TE 最近绘制标志 lastDraw=8、Terminal 文本框最大长度 15。无线列表和网络坐标数量无源码上限。
- output/side effect：这些常量控制传播可见速度、关闭淡出、placement 创建退避和每个活动块是否参与远端源面绘制；wirelessTime 按 2 tick 分段揭示列表。
- dependency/exception：常量是 public/static 或直接字面量，未接入配置文件；客户端文本限制不等于服务端限制。距离只对普通 Base 传播生效，Wireless 模式通过 canPropagate=false 短路。
- status：限制和时序为静态对应已确认；用户可配置性为确认缺失；真实 tick/渲染节奏为行为待动态验证。

### U11.2 生命周期清理与红石边界

- commit：客户端活动集合和 world/disconnect 清理由 65ca32a、797d5a9 接入；无线 TE 清理在 bcd69e1。
- path/symbol：TileEntityGlassBase.onLoad/onChunkUnload/shouldRefresh:55-82；TileEntityGlassMaster.onLoad/onChunkUnload/shouldRefresh:26-86；TileEntityGlassTerminal.onLoad/onChunkUnload/shouldRefresh:18-45；EventHandlerClient.onWorldUnload:110-147；disconnectFromServer:252-259；BlockGlass.neighborChanged:213-223。
- trigger/state：TE 加载/卸载/状态替换更新客户端 Map；世界卸载和断线释放 placement pair view frustum。服务端邻居通知检查 Master 红石电源并调用 changeRedstoneState。
- output/side effect：Master 激活时设置 channel=setChannel、distance=1、追加 placingFace；断电时设 active=false 并开始淡出/传播。只有在 setChannel 非空且 Master 未激活或当前频道等于 setChannel 时才改变红石状态。
- dependency/exception：断电时若 active Master 的 channel 与 setChannel 不同，外层条件会阻止关闭；Master 无线清理只按当前位置是否仍是 Wireless，客户端 users 的增减与列表修改不完全对称。placement 的 addActiveGlass/removeActiveGlass 只调用 generateActiveFaces，面计算主体仍是注释块。
- status：加载/卸载/红石触发为静态对应已确认；断线时序、重复 users、频道热切换和红石不一致状态为行为待动态验证；可配置生命周期策略为确认缺失。

## U12 资源、配方与声音

### U12.1 方块状态、模型与物品资源

- commit：基础资源在 0558c3c；Terminal 资源在 b0e8729；Projector 物品模型在 2a9da7c；终版模型替换在 bcd69e1。
- path/reference：assets/generallaymansaestheticspyingscreen/blockstates/block_glass.json:1-9；block_glass_terminal.json:1-5；models/block/block_glass.json:1-6；models/block/block_glass_terminal.json:1-6；models/item/block_glass.json:1-3；models/item/block_glass_projector.json:1-59；models/item/block_glass_terminal.json:1-3。
- trigger/state：ModelRegistry 将 block_glass metadata 0/1/2 分别映射到普通玻璃、Projector 复杂模型、普通玻璃，Terminal 映射到 Obsidian 模型；ModelBake 再把 Terminal 的 normal 模型替换为 ModelEmpty 加原版 Obsidian atlas sprite。
- output/side effect：Projector 物品模型显式引用 vanilla blocks/glass、blocks/obsidian、blocks/beacon，包含玻璃壳、黑曜石底座和 beacon 内层元素；世界方块模型仍是玻璃/黑曜石基础模型，动态标记来自 TE renderer。
- dependency/exception：资源命名空间为旧 generallaymansaestheticspyingscreen；blockstate 同时有 normal 与属性变体，四种属性组合都指向 glass。缺少其他语言文件和自有玻璃/黑曜石纹理；资源解析和模型烘焙未动态验证。
- status：路径、引用和实际资源文件为静态对应已确认；Forge 1.12.2 资源加载结果为行为待动态验证。

### U12.2 配方与物品元数据

- commit：Extension/Projector/Terminal 配方随 2a9da7c 引入；Wireless shapeless 配方在 bcd69e1。
- path/reference：assets/generallaymansaestheticspyingscreen/recipes/glass_extension.json:1-27；glass_projector.json:1-27；glass_terminal.json:1-26；glass_wireless.json:1-17；物品元数据由 common/item/ItemGlass.java:14-33 与 BlockGlass.getStateFromMeta/getMetaFromState:235-243 解释。
- trigger/state：三种 shaped 配方均使用三行 WCW/WNW/WRW；Extension 输入玻璃/金锭/末影珍珠/红石，结果 GLASS data 0；Projector 输入玻璃/末影珍珠/beacon/daylight detector，结果 data 1；Terminal 输入黑曜石/玻璃/末影之眼/红石，结果 Terminal；Wireless 用 data 0 GLASS 加 End Crystal 无序合成 data 2。
- output/side effect：ItemGlass 无耐久、保留 metadata subtype；创造栏顺序为 Projector、Wireless、Extension。配方输出依赖旧 Forge 1.12.2 assets/.../recipes 和 data metadata 语义。
- dependency/exception：配方没有自定义 serializer、权限或数量限制；输入/输出是否被目标版本实际注册取决于 Forge 1.12.2 资源扫描。现代数据包路径不在上游 baseline 中。
- status：配方内容和元数据对应为静态对应已确认；配方发现、合成结果和旧 Forge 兼容性为行为待动态验证。

### U12.3 声音与本地化

- commit：797d5a9608ab10ab211f229095efcdc30088f9f4 引入声音资源、注册和跟随最近活动块逻辑。
- path/reference：common/core/EventHandlerServer.java:35-40 注册 soundAmb；assets/generallaymansaestheticspyingscreen/sounds.json:1-8；二进制 sounds/amb.ogg；client/sound/SoundGlassAmbience.java:15-67；lang/en_us.lang:1-15。
- trigger/state：第一次 TerminalPlacement.drawPlane 时播放 SoundGlassAmbience，音量 0.015F、音高 0.6F、循环；声音位置每 tick 选择活动块中离观察实体最近者。placement 不再存在时 isDonePlaying 返回 true。
- output/side effect：声音类别为 BLOCKS，资源事件名为 generallaymansaestheticspyingscreen:amb；英文本地化提供四个方块名和频道/列表标签。
- dependency/exception：声音生命周期依赖 placement Map 删除和 WorldPortals view frustum 清理；没有音量配置、服务器声音包或多语言 fallback 的自有逻辑。drawPlane 只在实际 Portal plane 回调时触发，是否每个频道只建一个循环声音由 placement 缓存决定。
- status：资源引用、播放参数、最近面跟随为静态对应已确认；声音引擎循环/停止和资源解码为行为待动态验证。

## U13 iChunUtil 与 WorldPortals 外部边界

### U13.1 依赖来源与构建事实

- commit/path：上游 baseline 的 build.gradle:1-62 声明 ForgeGradle 2.3-SNAPSHOT、Forge 1.12.2-14.23.0.2493、Java 8、snapshot_20170929 mappings、runDir run；注释要求本地 libs/iChunUtil-deobf.jar，没有 Maven 坐标或 SHA-256。
- source URL/version：候选官方仓库 <https://github.com/iChun/iChunUtil.git>；与 GLASS baseline 时间线相符的依赖候选为 1.12.2-7.1.3、Forge 14.23.0.2493，已由主任务独立固定为 commit 5cf488107bc71e1fa5c0866e888f286bb3468e94。此版本是强候选来源，不等于 libs/iChunUtil-deobf.jar 的唯一二进制身份。
- WorldPortals source：源码包名为 me.ichun.mods.ichunutil.common.module.worldportals...，说明此 GLASS 构建通过 iChunUtil 内嵌模块接入；独立仓库 URL 线索为 <https://github.com/iChun/WorldPortals.git>，但 GLASS build.gradle 没有独立 WorldPortals 依赖坐标，不能把独立仓库 master 当成 baseline 依赖。
- status：构建事实和 URL/版本候选为静态对应已确认；deobf JAR 精确来源、签名和实际类路径为定位但接入未确认。

### U13.2 GLASS 实际调用的 iChunUtil API

- path/symbol：
  - common.iChunUtil：VERSION_MAJOR、VERSION_OF_MC、MC_VERSION_RANGE、GUI_CONFIG_FACTORY；
  - common.core.Logger：createLogger；
  - common.module.update.UpdateChecker：registerMod、ModVersionInfo；
  - common.core.network.PacketChannel/AbstractPacket：频道注册、包编解码和服务端执行；
  - common.core.util.EntityHelper：interpolateValues、interpolateRotation；
  - client.model.item.ModelEmpty：Terminal 模型替换；
  - client.render.RendererHelper：GUI 颜色、随机颜色、OpenGL scissor；
  - common.module.worldportals.common.WorldPortals：init、客户端 eventHandlerClient.renderGlobalProxy.releaseViewFrustum；
  - common.module.worldportals.common.portal.WorldPortal：TerminalPlacement 父类、pair、位置/面/Quaternion、Portal 回调；
  - common.module.worldportals.common.portal.EntityTransformationStack：相机实体目标位置/旋转变换；
  - common.module.worldportals.client.render.WorldPortalRenderer：renderLevel、getRollFactor、renderWorldPortal。
- dependency/trigger：这些调用分布在 GeneralLaymansAestheticSpyingScreen.java:15-43、ProxyCommon.java:18-30、EventHandlerClient.java:144,239,256、TerminalPlacement.java:24-187、TileEntityGlassRenderer.java:123-180 及各 GUI/声音类。
- exception/status：上游 GLASS 自身没有这些 API 的实现或 fallback；任一 API 签名、线程或渲染状态不匹配都会影响注册、网络、模型或 Portal。调用点为静态对应已确认，依赖实现对 GLASS 所需行为的完整接入为定位但接入未确认；具体 WorldPortal/Quaternion/cull 行为以主任务的 5cf 外部审计为准。

### U13.3 外部边界不能反推出的语义

- 缺失证据：GLASS 源码没有 iChunUtil PacketChannel 的包 ID/线程调度/异常策略，没有 WorldPortalRenderer 的实际 RenderTarget/视锥/裁切实现，也没有 EntityTransformationStack 的完整旋转约定。
- 可确认事实：GLASS 传入同一个 World 创建 source/pair；调用 renderWorldPortal 前后手工恢复一组 OpenGL 状态；在断线/世界清理时调用 releaseViewFrustum；createFakeInstance 返回 null。
- 状态：外部类存在的源码引用为静态对应已确认；外部 API 的运行行为和二进制精确对应为行为待动态验证，不可把 upstream clone clean 或历史提交主题当成产品运行通过。

## 基线后历史与遗漏边界

- git ls-remote --heads <https://github.com/iChun/GeneralLaymansAestheticSpyingScreen.git> 在本审计时只返回 master -> 99d9b417；独立 clone 的 master 也固定该 hash，因此没有基线后的 live commit 可单列。
- 版本链中的最后三次语义变化为：8ce6636 无线按顺序传播动画；7584db9 针对非线程安全 ArrayList 的 WorldPortal 面汇总 workaround；99d9b417 将 renderAll 改为 setCullRender 以适配 iChunUtil 变化。它们都已纳入上述 U06/U08/U13 记录。
- 本文不计算文件相似率，也不把当前 port 的实现、测试或资源当作上游事实；对应关系、Target 覆盖和缺失判定留给主任务及其他审计文档。

## 结论

历史结论按正文保留。迁移后的当前代码位置与本轮验证结果见 [构建迁移说明](../development/porting-template_migration.md)。
