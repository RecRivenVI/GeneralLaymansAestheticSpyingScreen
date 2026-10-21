# GLASS 渲染产品化当前状态

路线 A 已完成五端产品化，SSH 签名 Update `35f1fc0` 已正常推送 origin/develop。路线 B 已在 1.21.1 NeoForge 打通远端、下界、Sable 和 Sodium；54 号测量已采齐客户端、A/B、远端与子世界、服务端和网络数据，保持统一 B，产品和预算未改。A 原验收 PID 在本轮开始时已退出，未操作或重启；B 验收实例只准备、不启动。路线 B 未提交、未推送，用户视觉验收仍待进行。

| 工作 | 当前状态 |
| --- | --- |
| 原生方块与覆盖 | 五端四种方块均为完整方块，保留所有原生面；动态内饰使用原版模型入口和实际位置光照；无显示方块实体 |
| 参考端 | 一致：六朝向 6–16 格、0–80°，东西向额外 64 格；两档改进透明显示、前景遮挡、无线、薄物体、两种动画、两处裁切及四种环境光已看图复核 |
| 26.3 Fabric | 一致：两档透明设置下有线、无线、玻璃水前景及实体遮挡；正常退出 |
| 26.1.2 NeoForge | 一致：有线、无线及切换返回后的实体与玻璃遮挡；独立编译缓冲与串行调度消除任务饥饿，正常退出 |
| 1.21.1 NeoForge | 一致：Fancy / Fabulous 有线、无线及实体与玻璃遮挡；频道编译缓冲独立持有，正常退出 |
| 1.20.1 Forge | 一致：Fancy / Fabulous 有线、无线及实体与玻璃遮挡；已修正相机前向约定、首帧未就绪矩阵和内饰后的地形光照状态，正常退出 |
| 门禁与交付 | 五端产品化门禁已成功，Update `35f1fc0` 已签名推送；A 用户实例已退出，本轮没有重启 |
| 路线 B | 近端地形、箱盖、文字、装备、水雾与 A 一致；8192 格、下界持续更新，邻区实体移动、缩放、死亡移除与粒子已实测；自有票据最终归零 |
| Sable 与 Sodium | 三种摆放均完成平移、三轴旋转、旋转中心、非均匀缩放、倾斜裁切、GUI 和生命周期核对；八场景矩阵与定向修正完成，无线地板与水晶光照已恢复 |
| B 验收交接 | 七个顶层发行 JAR 组合实机完成进入世界、F8 暂停恢复和保存退出；用户目录含三个演示世界，仅准备、不启动 |
| B 性能测量 | 0/1/2/4 网络、无/有 Sodium、A/B/B/A、8192 格、下界、Sable 连续运动、独立服务端和真实 TCP 已采集；第二观看者为 FakePlayer，性能结论附干扰边界 |

## 当前证据

参考端索引为 `validations/visual-render_review/result/candidate.json`，补拍路径也在其中。所有截图保留在本机产物，不进入源码检查点。旧报告的图片已移到 `D:\Workspaces\Scratches\GlassProduction\historical-images`，历史结论未变。

其余端原图位于 `D:\Workspaces\Scratches\GlassProduction`：

- Fabric：`fabric-smoke/result` 与 `fabric-smoke/occlusion-result`。
- 26.1.2：`261-fresh/owned-buffer-result` 为最终复验位置；`261-fresh/relight-result` 保留修复前的返回场景缺失证据。
- 1.21.1：`21-fresh/fancy-owned-buffers`、`21-fresh/fabulous-owned-buffers`。
- 1.20.1：`20-fresh/fancy-camera-basis`、`20-fresh/fabulous-camera-basis`；NaN 首帧无限循环的线程记录为 `20-thread.txt`，两次卡住的自有自动实例强制退出，未宣称正常保存。

路线 B 候选身份、视觉差异和 MCP 限制分别为 `validations/integration-remote_projection/result` 下的 `candidate.json`、`visual-review.json`、`mcp-limitations.json`。详细发现见[交付报告](../research/review-remote_projection_delivery.md)，原理见[架构](../design/architecture-remote_projection.md)与[移植说明](../development/porting-remote_projection.md)。

用户一次完成的步骤分别见[路线 A 体验清单](../development/testing-render_acceptance.md)和[路线 B 体验清单](../development/testing-remote_acceptance.md)。本轮不启动 B；未来启动命令在 B 清单中。

54 号的成本排序、逐窗数据、并行负载和复测边界见[性能报告](../research/review-remote_projection_performance.md)，本轮输入身份、门禁与退出记录在 `validations/performance-remote_projection/result/candidate.json`。测量增加的代码只在验证组件与 probe 中，未进入产品 JAR。

## 开放项与下三步

默认 25 个源区块实际需要 81 个普通 FULL 区块，单船和双船为 82、83 个加载位置；全服默认 100，建议试验阶段保留，正式并发容量待用户决定。[配置说明](../usage/configuration-remote_projection.md)集中记录其余预算及首次加载、范围更换的等待差异。

两个真实客户端并发、粒子压力性能及 Iris 未验证。Sodium 独立视图的粒子采用原版提交分支，未定位快速提交内部失效点。Sodium 主世界的普通色墙仍有比原版更暗的渐变，投影内容未见同样差异；A 加 Sodium 的对照因旧钩子冲突未能启动，来源尚未确定。六向 64 格中的南北及上下被其他测试台遮挡，无法比较；正式验收范围为六向 6–16 格与 0–80°，东西向额外 64 格。薄物体结论只覆盖本轮样本。路线 B 只在 1.21.1 NeoForge，其余四端仍使用路线 A。

1. 用户决定是否安排安静环境与两个真实客户端复测；当前数据不足以确定整帧 B/A 和 Sodium 加速比。
2. 用户按 B 的三世界清单体验运动、F8 暂停、倾斜裁切与 GUI；本轮未操作验收实例。
3. 用户决定是否优先调查重复捕获、持续源网格编译或共享编码；预算保持原值，不自行开展优化。

测试数据软 3 GiB、硬 5 GiB，合计包括本项目 validations、固定 MCP 输入、全部 GlassFeasibility 与 GlassProduction。旧可重建实例与安装缓存已送回收站，当前合计记录在性能候选清单；迁移保护材料未动。
