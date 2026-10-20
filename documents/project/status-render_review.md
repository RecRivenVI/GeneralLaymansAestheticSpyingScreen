# GLASS 渲染产品化当前状态

49 号路线 A 已在五端实现完整方块与原生覆盖。参考端固定场景已复核；五端 check 与 verifyRelease 已全部成功，严格合规 0 失败、0 警告。参考端用户验收实例已启动并释放 MCP 控制，保持运行；尚未提交或推送，随后连续执行路线 B、Sable 与 Sodium。

| 工作 | 当前状态 |
| --- | --- |
| 原生方块与覆盖 | 五端四种方块均为完整方块，保留所有原生面；动态内饰使用原版模型入口和实际位置光照；无显示方块实体 |
| 参考端 | 一致：六朝向 6–16 格、0–80°，东西向额外 64 格；两档改进透明显示、前景遮挡、无线、薄物体、两种动画、两处裁切及四种环境光已看图复核 |
| 26.3 Fabric | 一致：两档透明设置下有线、无线、玻璃水前景及实体遮挡；正常退出 |
| 26.1.2 NeoForge | 一致：有线、无线及切换返回后的实体与玻璃遮挡；独立编译缓冲与串行调度消除任务饥饿，正常退出 |
| 1.21.1 NeoForge | 一致：Fancy / Fabulous 有线、无线及实体与玻璃遮挡；频道编译缓冲独立持有，正常退出 |
| 1.20.1 Forge | 一致：Fancy / Fabulous 有线、无线及实体与玻璃遮挡；已修正相机前向约定、首帧未就绪矩阵和内饰后的地形光照状态，正常退出 |
| 门禁与交付 | Spotless、五端 check / verifyRelease 已成功；参考端验收实例已启动并保持运行，随后签名提交推送 Update |
| 路线 B | 第二份 Goal 尚未开始；只在 1.21.1 NeoForge 开发，结束时准备含 Sable / Sodium 的实例但不启动 |

## 当前证据

参考端索引为 `validations/visual-render_review/result/candidate.json`，补拍路径也在其中。所有截图保留在本机产物，不进入源码检查点。旧报告的图片已移到 `D:\Workspaces\Scratches\GlassProduction\historical-images`，历史结论未变。

其余端原图位于 `D:\Workspaces\Scratches\GlassProduction`：

- Fabric：`fabric-smoke/result` 与 `fabric-smoke/occlusion-result`。
- 26.1.2：`261-fresh/owned-buffer-result` 为最终复验位置；`261-fresh/relight-result` 保留修复前的返回场景缺失证据。
- 1.21.1：`21-fresh/fancy-owned-buffers`、`21-fresh/fabulous-owned-buffers`。
- 1.20.1：`20-fresh/fancy-camera-basis`、`20-fresh/fabulous-camera-basis`；NaN 首帧无限循环的线程记录为 `20-thread.txt`，两次卡住的自有自动实例强制退出，未宣称正常保存。

算法与原生阶段差异见[移植说明](../development/porting-projection_overlay.md)。用户一次完成的步骤见[体验清单](../development/testing-render_acceptance.md)。

## 开放项与下三步

六向 64 格中的南北及上下被其他测试台遮挡，无法比较；正式验收范围为六向 6–16 格与 0–80°，东西向额外 64 格。薄物体仅对本轮地毯、单层雪、火把样本作结论。Sodium、Sable、远距离和跨维度仍由第二份 Goal 完成；当前产品要求终端在玩家加载范围内且同维度。

1. 保持参考端验收实例运行；其实例目录、PID、配置和控制交接记录在 `validations/visual-render_review/result/acceptance.json`。
2. 签名提交 Update、正常推送 origin/develop。
3. 按用户指定完成第一份 Goal，立即创建第二份 Goal 并继续，不关闭参考端验收实例。

测试数据受软 3 GiB、硬 5 GiB 约束，合计包括本项目 validations、固定 MCP 输入、GlassFeasibility 与 GlassProduction；最近合计约 3.74 GiB，未达硬上限。清理只送回收站，迁移保护材料不动。
