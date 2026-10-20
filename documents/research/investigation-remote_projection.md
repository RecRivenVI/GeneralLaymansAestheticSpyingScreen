# 跨维度与超距离投影可行性

调查日期：2026-10-20。路线 B **有条件可行**。1.21.1 NeoForge 已证明同维度远端及下界的原版区块／光照数据，可以经真实网络负载到达两个独立客户端世界，主世界不被覆盖，停止订阅后释放视图及强加载票据。完整远端绘制、实体、粒子、持续更新、性能和 Sable 运行兼容尚未实现；它们不能由这次静态数据原型推断。完整面覆盖的路线 A 另见[报告](review-projection_feasibility.md)。

## 问题

终端超出玩家正常区块接收范围，或位于其他维度时仍需显示真实世界。只保持服务端加载不够，数据还必须授权发送、独立缓存，并供远端相机渲染。

Sable 不改变“终端记录真实世界”的原理。终端位于子世界上时，它在真实世界里的观察姿态随子世界平移和旋转；投影器在子世界上时，观察者相对投影器的姿态也随之变化。来源不是把子世界 plot 存储区当成真实世界。

## 方法

先检查最可能失败的三处：原版区块缓存会丢掉远端坐标；区块到达不等于光照正确且可能错误写入主世界；观看结束后的区块票据和渲染资源可能残留。随后在仓库外做一个有界数据原型，用服务端原生状态与光照值作标准答案。

原型与运行证据在 `D:\Workspaces\Scratches\GlassFeasibility`，身份与索引仅记在 `handoff/manifest.json` 和 `handoff/summary.json`。Sable 使用[Modrinth 的 2.0.6+mc1.21.1 NeoForge 发行包](https://modrinth.com/mod/sable/version/fg9dTRz9)，下载位置及来源在原型的 `inputs/sable-source.json`；只检查实际发行 API 与公开文档，没有安装 Sable，没有修改 Radiance 或其他项目。

## 发现

### 最小数据闭环

1. 同维度与下界分别在 X=8196、Z=4 放置不同颜色方块及发光源；对应区块 X=512，远超玩家 8 区块视距。
2. 每个维度只强加载一个源区块，总计两个票据；先验证最小样本，没有扩大到原建议的两区块半径。
3. 服务端构造原版 `ClientboundLevelChunkWithLightPacket`，由 NeoForge 自定义 payload 的真实连接编解码并送到客户端，不共享服务端对象。
4. 创建两个独立 `ClientLevel`、`ClientChunkCache`、光照引擎与 `LevelRenderer`。先初始化各自渲染器，再设置远端 view center，最后装入区块和光照数据；不赋值给 `Minecraft.level` 或连接的主世界。
5. 客户端对照服务端块状态、天空光与方块光，并确认主世界对象、连接世界、原有方块以及正常缓存都未被远端覆盖。
6. 收齐两个视图后发回释放负载。服务端强加载集合恢复原值，客户端关闭独立渲染器并丢弃两个视图。

| 对照 | 结果 | 实际值 |
| --- | --- | --- |
| 远端主世界方块及光照 | 一致 | 红色混凝土，方块光 14，天空光 15，与服务端相同 |
| 下界方块及光照 | 一致 | 黄绿色混凝土，方块光 14，天空光 0，与服务端相同 |
| 主世界隔离 | 一致 | 主世界及连接世界对象不变，原方块状态不变，原版主缓存没有收到 X=512 的区块 |
| 停止观看 | 一致 | 两个服务端强加载标记移除，集合恢复原值；独立视图及渲染器释放 |
| 完整远端画面、动态更新、Sable | 无法比较 | 本轮最小原型没有渲染远端视图或运行 Sable |

初始样本暴露了独立渲染器尚未初始化区段遮挡图的空指针问题；`ClientLevel.onChunkLoaded` 会通知其渲染器。修正初始化顺序后完成数据闭环。失败日志与最终 `route-b.json`、`server-release.json`、`release.json` 都保留。另查明 `ClientLevel.hasChunk` 恒返回 true，不能用来证明缓存收到区块；本原型实际检查 `ClientChunkCache.getChunk(..., false)`。

### 服务端强加载与权限边界

产品应由服务端依据获准终端、频道权限和观看者建立订阅，不能让客户端任意请求维度或坐标。私有频道在发送任何方块／实体／光照前检查；终端失效、频道变更、玩家离线及订阅超时都结束对应生命周期。服务端加载、普通玩家区块跟踪、远端数据订阅是三套职责，不会自动互相替代。

原型使用 `ServerLevel.setChunkForced`，只证明原生加载和释放；产品应使用各 Loader 的按 owner 管理的票据及引用计数，避免移除其他观看者或其他模组的票据。停止观看后释放自有引用，不承诺区块立刻卸载，因为正常玩家及其他票据仍可持有它。

建议第一个产品里程碑限制为每位观看者一个终端、固定最多两区块半径（25 个源区块），并对全服总区块数、每人带宽、初始快照速率和空闲期限设硬预算；具体预算交用户定。超限应明确拒绝，不悄悄无限扩张。区块生成必须显式授权，静态观察不能隐式要求远端实体持续模拟；加载级别、实体 tick 与数据更新频率需要分开规定。服务器重启后清理失去观看者的持久票据。

### 客户端独立视图的最小形态

每个视图至少包含视图 ID／世代、真实维度身份及属性、世界时间／天气、区块缓存与中心、方块实体数据、独立光照引擎、原生渲染器及捕获目标；收到更新必须按视图 ID 路由。结束后取消网络队列、释放 GPU 及网格，旧世代数据不能进入新视图。

原版 `ClientChunkCache` 只有一个中心，必须先移到源区块，否则远端包被丢弃。因此不能简单声称“同一维度多个相距很远的终端自动共用一个原版缓存”；第一个里程碑宜每个源区域一个有界视图，后续才设计稀疏共享缓存及重叠订阅去重。

完整 Minecraft 内容仍需远端实体跟踪、生成／移动／销毁、粒子事件、环境与维度效果、方块实体更新及客户端 tick。第三方渲染器依赖全局单世界时需要专门的上下文适配，不能只更换一个 `ClientLevel` 引用。有限自有网格只适合明确接受静态内容的合同，不推荐以它替代完整世界。

### Sable 实际姿态与生命周期

实际 2.0.6 JAR 包含 Companion 1.6.0。`SableCompanion.INSTANCE.getContaining(level, blockPos)` 可以从 plot 存储坐标找到子世界；客户端 `ClientSubLevelAccess.renderPose(partialTick)` 提供同一帧的插值姿态，`logicalPose()` 用于服务端逻辑位置。`Pose3dc` 还包含 rotation point 和 scale，适配时使用完整 `bakeIntoMatrix`／位置和法向变换，不能只把 position 与 quaternion 拼接而丢掉旋转中心。[Companion 的官方位置说明](https://github.com/ryanhcode/sable-companion#working-with-positions)也明确区分 plot 坐标与 global 空间。

设 P 为投影器局部坐标到真实世界的矩阵，T 为终端局部坐标到其真实世界的矩阵，C 为观察者真实世界相机姿态，F 为沿用检查点的投影面／出口坐标约定，则远端相机为：

```text
P = projectorSublevelRenderPose × projectorLocalFrame
T = terminalSublevelRenderPose × terminalLocalFrame
remoteCamera = T × F × inverse(P) × C
worldExitPlane = transpose(inverse(T)) × localExitPlane
```

普通方块的 sublevel pose 取单位矩阵。每帧先取终端和投影器的同一 partial tick 姿态快照，再同时计算相机、显示面及出口平面；不能混用 logical pose 与 render pose。终端的真实世界位置决定订阅的源区域，子世界移动跨越区块时更新这些订阅，不把 plot 的极大存储坐标送进普通远端缓存。

目前投影映射及可见性判断依赖六朝向。任意旋转不仅要改相机矩阵，还要把出口裁切和区段／方块的轴向判定推广为倾斜平面与 AABB 的相交分类；固定轴距离判断不能原样保留。法向要用逆转置并重新归一化，余量以真实世界格为单位。用户本轮要求平移与任意旋转；额外缩放的产品行为没有在本轮决定。

子世界自身还要保持加载：实际 `ServerSubLevelContainer` 提供 `addForceLoadTicket`／`removeForceLoadTicket` 和按类型登记的 `SubLevelLoadingTicketType`。产品应以订阅身份使用自有票据类型，不借用管理员命令票据，并与终端／投影器所属子世界的依赖链一起释放。子世界票据与真实来源世界区块票据并不相互替代。

### 可移植性与维护

未来不依赖游戏的部分可放进现有 `projection_math`，或需求实际出现后拆出单一职责的订阅组件：任意姿态组合、相机映射、平面／AABB 数学、订阅状态机、预算与引用计数、视图世代和权限判定所需的纯数据。权限系统的具体调用、维度注册表、原生票据、包编码、世界缓存、光照与绘制始终留在各 Target。这里的“薄适配”指隔离原生类型，不代表远端渲染适配只有少量代码。本轮没有新增这些产品抽象。

| Target | 原生适配量及内容 |
| --- | --- |
| 1.21.1 NeoForge | 大：本轮数据原型可作为第一步；还需持续更新、原生世界绘制上下文、完整生命周期；Sable 姿态／票据／绘制分派另做可选适配 |
| 1.20.1 Forge | 很大：Java 17、SimpleChannel、较早区块／光照包与世界渲染接口，不能复制 NeoForge 游戏代码 |
| 26.1.2 NeoForge | 很大：新版原生 payload、独立世界与 extract/render 流程，独立 GPU 资源上下文 |
| 26.3 NeoForge | 很大：独立世界、LevelExtractor、RenderPearl 的视图与 GPU 生命周期 |
| 26.3 Fabric | 很大：与 26.3 同一渲染接口族，但 Fabric 网络、事件、资源生命周期单独接线 |

额外成本包括源区块生成与保持、每观看者网络、客户端世界 tick、网格编译、光照、捕获目标及每网络重绘。没有进行性能测量，不能按“每频道只多一张纹理”估算资源。

### 来源与许可

- [Sable 官方仓库](https://github.com/ryanhcode/sable)：PolyForm Shield 1.0.0，只阅读 API／实现思路，没有复制代码，也没有将发行包放进产品。
- [Sable Companion](https://github.com/ryanhcode/sable-companion)：MIT；本轮只检查发行包内 API，不新增产品依赖或打包。
- [NeoForge 1.21.1 payload 文档](https://docs.neoforged.net/docs/1.21.1/networking/payload/)：原型采用原生注册与队列约定，实际原版包类型以本项目编译类路径核对。
- [Immersive Portals 实现说明](https://qouteall.fun/immptl/wiki/Implementation-Details.html)及其 [1.21 分支许可](https://github.com/iPortalTeam/ImmersivePortalsMod/blob/1.21/LICENSE)：Apache-2.0，仅作为多世界／带维度路由架构参考，未复制代码。

## 结论

推荐 B 的“有界服务端订阅＋独立客户端世界视图”作为超距离和跨维度方向。第一步放在用户实际需要 Sable 的 1.21.1 NeoForge：一个获准终端、最多 25 区块、静态方块及光照，先把本轮独立数据视图真正画进捕获目标；同时用独立于游戏的实际姿态数学接终端和投影器，再接 Sable。后续逐项增加动态内容并测量预算，最后再移植其余端。

主要风险是第三方渲染器的全局上下文、远端实体／粒子兼容、移动来源引起的订阅抖动，以及强加载／带宽的资源成本。Sable 目前只有实际发行 API 与文档可行性证据，必须在后续里程碑做运行验收。是否启动这项产品工作，由用户决定。
