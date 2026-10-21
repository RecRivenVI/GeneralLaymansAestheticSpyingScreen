# 路线 B 体验清单

适用端为 1.21.1 NeoForge，验收实例含 Sable、Sodium、Custom Skin Loader 和 Zume。实例只准备，不自动启动；2560×1440、全局声音 50%、音乐关闭，玩家名 RecRivenVI。

## 启动

从仓库根目录执行：

```powershell
python components/runtime_validation/remote_projection.py acceptance
```

目录和准备状态记录在 `validations/integration-remote_projection/result/acceptance-prepared.json`。此入口只启动路线 B 的专用实例。路线 A 已经开着的验收实例继续保留。

## 一次体验

1. 进入 `GLASS B sodium both`，转身面向石框投影墙；移动并从斜角观察，画面应贴住完整方块，随两端船体姿态改变。
2. 按 F8 暂停船体运动，靠近看青橙棋盘管道与出口面的交界；应有连续裁切，无洞、无裂缝。再按 F8 恢复，画面应连续变化。
3. 暂停后按 T 输入 `/gamemode creative`，右键终端和投影方块，检查频道列表、按钮、焦点及滚动；按 Esc 返回。
4. 保存退出，分别进入 `GLASS B sodium terminal` 和 `GLASS B sodium projector`，重复移动、F8 和斜角观察。前者只有终端船，后者只有投影船。

首次观看需要等待区块和网格就绪；源移动到新范围时会暂留上一帧。完整差异、预算和自动验证证据见[交付核对](../research/review-remote_projection_delivery.md)。
