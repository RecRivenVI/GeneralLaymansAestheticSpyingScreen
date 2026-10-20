# 完整方块面上的投影可行性

调查日期：2026-10-20。路线 A **有条件可行**：推荐在实体方块之后、透明物之前绘制，并施加朝相机方向的小深度偏移。OpenGL 与 Vulkan 外部原型均保留全部原版方块面，六朝向、近远及各自透明路径的静态图案保持可见。原型没有接入 GLASS 实时捕获、两种动画、Sable 或第三方渲染器，不能据此承诺任意距离及所有组合都稳定。路线 B 见[远端视图报告](investigation-remote_projection.md)。

## 问题

判断投影能否覆盖完整方块的共面表面，同时接受普通场景遮挡；不隐藏方块面，不添加显示方块实体。产品仍保持检查点，原型不进入发行 JAR。

## 方法

运行前的联合风险清单记录了深度精度、透明阶段及远端隔离问题。路线 A 的三处主要风险如下，第三项的严格相等测试只作接口分析，没有另做运行原型：

1. 远处及掠射角的深度精度，以及正向／反向 Z 的偏移符号不同，可能闪烁或穿透前景。
2. 透明渲染的目标及阶段不同，投影可能在透明合成后被覆盖；需要实际切换两条路径。
3. 原生几何与投影矩阵的舍入不同，严格深度相等可能漏像素；Sable 的姿态与绘制顺序又可能增加差异。

原型、源码、脚本、正常 Loader 安装及原始截图位于 `D:\Workspaces\Scratches\GlassFeasibility`。输入身份集中在该目录的 `handoff/manifest.json`；运行索引为 `handoff/summary.json`。该目录保留到用户决定，不复制到产品源码。MCP 固定分发身份沿用项目已登记的快照清单，使用登记名为 `glass_feasibility_01a08652`。

对照标准是未经替换的原版闪长岩／石头完整面、原版金块遮挡和玻璃。一个一格立方体及一个由完整原版方块组成的三格区域，分别覆盖共面的棋盘；后者的每个可见侧面代表一个无线显示平面。几何坐标没有向外移动，偏移只发生在深度计算。棋盘使用索引三角形，检验的是 GPU 共面覆盖，不是无线拓扑及频道功能。

26.3 NeoForge 实际日志确认 Vulkan；1.21.1 NeoForge 记录 OpenGL 4.6，均为 RTX 4080 SUPER / NVIDIA 617.14。自动实例 1920×1080、静音、四个逻辑核、低优先级，一次一个客户端。MCP 执行原生命令定位、原生设置操作、截图、保存退出；场景布置和内部断言由外部探针完成。

## 发现

### 做法比较

| 做法 | OpenGL | Vulkan | 透明路径与判断 |
| --- | --- | --- | --- |
| 仅改变阶段 | 原生实体方块阶段之后 | `executeSolid` 之后的同一个 RenderPass | 解决谁先绘制，不能解决共面深度；单独使用不足 |
| 阶段＋深度偏移 | `LEQUAL`，polygon offset factor/units 为 -1/-1 | reverse-Z `GREATER_THAN_OR_EQUAL`，bias scale/constant 为 +1/+1 | 本轮实际原型；均不写深度。保留前景深度，实测两条透明路径覆盖关系一致 |
| 深度相等 | `GL_EQUAL` | `CompareOp.EQUAL` | 概念等价，区块顶点压缩、相机相对坐标与矩阵舍入不能保证位相等；不推荐作为唯一判据，未另做运行原型 |
| 模板缓冲 | 需要带 stencil 的目标及相应 pass | 需要对应 attachment 格式与 stencil pipeline | 不是一个跨后端开关；需维护新目标或掩码及深度复制，现有深度目标不能直接假定有 stencil。本轮未实现 |

26.3 必须在活动 pass 外上传顶点和矩阵，pass 内只绑定、绘制，pass 结束后才结束缓冲帧。初始样本曾因未声明颜色目标数而失败，修正显式 `ColorTargetState.DEFAULT` 后运行矩阵；失败报告保留在原型目录。1.21.1 使用原生 `AFTER_SOLID_BLOCKS` 事件。

### 画面对照

截图拼图逐张来源于原始 MCP 截图，远处中心另作最近邻放大便于检查；原始 1920×1080 图保留在运行目录。已亲自检查拼图、遮挡和侧视原图。

图 1 OpenGL Fancy 六朝向及近远对照：本机证据 `D:\Workspaces\Scratches\GlassProduction\historical-images\review-projection_feasibility-1.png`（不随源码检查点提交）。

图 2 OpenGL Fabulous 六朝向及近远对照：本机证据 `D:\Workspaces\Scratches\GlassProduction\historical-images\review-projection_feasibility-2.png`（不随源码检查点提交）。

图 3 Vulkan 改进透明显示关闭：本机证据 `D:\Workspaces\Scratches\GlassProduction\historical-images\review-projection_feasibility-3.png`（不随源码检查点提交）。

图 4 Vulkan 改进透明显示开启：本机证据 `D:\Workspaces\Scratches\GlassProduction\historical-images\review-projection_feasibility-4.png`（不随源码检查点提交）。

图 5 OpenGL 避开夹具遮挡的补充视角：本机证据 `D:\Workspaces\Scratches\GlassProduction\historical-images\review-projection_feasibility-5.png`（不随源码检查点提交）。

| 对照 | 结果 | 可见影响与范围 |
| --- | --- | --- |
| 六朝向与近远 | 一致 | 共面棋盘持续可见，没有看到原生底面穿插的条纹；中心距离为 4 和 64 格 |
| 原生前景遮挡 | 一致 | 金块保持在棋盘前，玻璃边框及透明部分正常显示；北向无线近处 4 格的中心被金块刻意挡住，另有 7 格遮挡场景及移走金块的近处补图 |
| OpenGL Fancy / Fabulous | 一致 | 两条原生透明目标路径没有改变棋盘覆盖及金块遮挡 |
| 26.3 改进透明显示关／开 | 一致 | 原生 F3＋X 操作后，选项持久值确认为 true，覆盖关系未变 |
| 1.21.1 的同名开关 | 无法比较 | 该版本不存在“改进透明显示”；Fancy / Fabulous 是原生两条路径，不冒充同一设置 |
| OpenGL 与 Vulkan | 有差异 | 原版天空、云和雾不同；同一类棋盘覆盖与前景遮挡没有观察到差异，不作整帧像素一致主张 |
| 无限距离、完整捕获、动画及 Sable | 无法比较 | 未接入本轮最小原型，不能从有限静态截图推断 |

OpenGL 的初始两个远处轴向视角中，三格夹具与一格夹具发生真实互相遮挡。这些截图仅证明深度遮挡。斜视补图仍有相接轮廓，因此进一步只对这四个受影响样本，临时移走三格夹具及其覆盖绘制，按 64 格轴向独立检查一格方块，结束后恢复夹具。独立补图见原型的 `prototype21/isolation-result`；北向移走金块的近处补图在 `prototype21/followup-result`。没有重跑已通过的路线 B。

### Sable 上的完整面

实际 Sable 2.0.6 JAR 的 `SubLevelRenderDispatcher.renderSectionLayer` 与 `renderAfterSections` 存在，vanilla mixin 将子世界层接入原生层渲染。因而原则上可以在子世界实体面已经写入同一深度目标之后，按同一帧 render pose 绘制偏移覆盖，不需要隐藏 Sable 的面。Fabulous、Fancy、Sodium 分派器的阶段顺序必须各自接线；仅监听普通方块阶段不能证明所有 Sable 分派器都已结束。此项为源码／API判断，未启动 Sable，不计运行兼容通过。姿态接口及许可见[远端报告](investigation-remote_projection.md)。

### 可移植边界

复用现有 `projection_math` 组件的面局部坐标、三角化和姿态数学；未来泛化为任意姿态时仍不引用游戏类型。阶段顺序、深度比较方向、偏移量、目标绑定、上传时机和 GPU 资源生命周期属于各端原生适配，不能放进纯数学组件。原型没有预建新的公共框架。

| Target | 适配量及内容 |
| --- | --- |
| 26.3 NeoForge | 中：本轮验证的 RenderPearl 方向；接入实际捕获纹理、两种动画和透明场景仍需工作 |
| 26.3 Fabric | 中：同一 GPU 接口族；单独接 Fabric 生命周期与阶段注入，不共享游戏源码 |
| 26.1.2 NeoForge | 中至大：较早 GPU API、原生 pass 和深度约定需独立映射，不能照搬 reverse-Z 正偏移 |
| 1.21.1 NeoForge | 中：本轮 OpenGL 原型方向；Sable vanilla/Fancy/Sodium 的阶段适配另计 |
| 1.20.1 Forge | 中：经典 Blaze3D / OpenGL 与 Forge 阶段，沿用向近处的偏移语义，Java 17 独立实现 |

## 结论

建议采用“实体面之后＋小深度偏移”，保留正常深度测试。第一步只把参考端现有投影纹理接入这个阶段，模型面全部保留，按检查点分别验证默认与经典动画，然后才逐端移植。不要把本轮静态样本解释为已经完成产品集成。

主要风险是掠射角偏移对很薄前景的误覆盖、远距离浮点精度，以及 Sable／第三方渲染器接管阶段。应把支持的观察距离及角度作为验收范围；有限偏移不是不受精度限制的绝对层级。是否进入产品实现，交用户决定。
