# 独立远端投影验证

## 目标

在 1.21.1 NeoForge 验证有界订阅、独立客户端世界、远端与下界更新、票据释放，以及 Sable 和 Sodium 的渲染路径。画面对照采用路线 A 的发行构件，截图由 MCP 获取并人工逐项比较。

## 运行

从仓库根目录执行：

```powershell
.\gradlew.bat :runtime_validation:verifyRemoteProjection
python components/runtime_validation/remote_projection.py packaged-smoke
python components/runtime_validation/remote_projection.py prepare-acceptance
```

自动实例使用 1920×1080、全局静音。工具只操作本验证的实例。发行构件冒烟使用独立副本；最后一步只准备验收实例，不启动。已验证的项目专用安装运行时位于本验证 `instance/native_runtime`。工具的 Python 环境需要 Pillow 读取 MCP 截图，以等待异步网格实际出现。具体场景与交付结果见[交付报告](../../documents/research/review-remote_projection_delivery.md)。

## 通过标准

独立视图显示投影，原版玩家世界与连接保持不变；有界订阅更新源画面，停止观看时 GLASS 自有票据与视图均归零。截图必须有人工对照结论；仅构建或日志成功不能判定画面一致。
