# 检查点行为恢复复核

调查日期：2026-10-20。26.3 NeoForge 的产品动画、模型和外观已恢复检查点 `671755d`，仅保留两项出口裁切修复与“经典”文字修正。以下是原生 Vulkan 实例截图与源码／资源对照；用户人工视觉体验独立于本轮自动复核。

## 问题

确认撤回的完整方块、显示方块实体、隐藏方块面及不透明动画改造不再进入参考端产品，并验证出口余量仍适合检查点模型。

## 方法

产品 `src/main` 与检查点比较，差异仅为 `GlassExitPlane`、`RenderSectionRegionMixin` 及中英文语言文件；数学组件、模型、材质、动画和合成器均与检查点一致。参考端相关单元测试及 JAR 构建成功。

从已关闭的 45 号自动夹具复制测试世界，保留原实例；只在新副本中丢弃已撤回的 `glass:display` 类型。默认及经典动画分别使用独立副本、原有时长与曲线；外部验证模组只调用原版 tick 冻结固定中间帧，六秒后恢复，不改变产品或传播配置。MCP 操作供电、位置和截图并保存退出。

## 发现

| 对照 | 结果 | 观察 |
| --- | --- | --- |
| 模型／外观／两种动画源码 | 一致 | 与检查点逐文件比较无差异；显示方块实体及隐藏面实现不在产品中 |
| 默认动画 | 一致 | 中间帧显示面缩放，画面不作淡入淡出 |
| 经典动画 | 一致 | 面不缩放；开启白底上淡入画面，关闭时画面撤去、白底淡出；扩展仍按检查点传播延迟接续 |
| 紧贴及一纹素 | 有差异 | 相比检查点错误裁去贴面，保留修复后两例正面完整，未见中央洞口 |
| 掠射角余量细条 | 无法比较 | 当前截图未见明显细条，无法从有限分辨率证明理论上 1/4096 格残余完全不存在 |

图 1 两个出口用例正视：本机证据 `D:\Workspaces\Scratches\GlassProduction\historical-images\review-checkpoint_restore-1.png`（不随源码检查点提交）。

图 2 两个出口用例侧视：本机证据 `D:\Workspaces\Scratches\GlassProduction\historical-images\review-checkpoint_restore-2.png`（不随源码检查点提交）。

图 3 默认与经典的开启关闭中间帧：本机证据 `D:\Workspaces\Scratches\GlassProduction\historical-images\review-checkpoint_restore-3.png`（不随源码检查点提交）。

截图已经逐张检查。原图、tick 固定记录与正常退出证据位于 `D:\Workspaces\Scratches\GlassFeasibility` 的 `baseline`、`animation-default`、`animation-classic`；唯一冻结身份见 `handoff/manifest.json`。

## 结论

检查点行为恢复完成，保留六参数构造器注入及向被裁侧的 1/4096 格余量。两种动画、时长和曲线不作改造；其余四端未移植本轮修复。后续完整面与远端功能仍按[当前状态](../project/status-render_review.md)等待用户决定。
