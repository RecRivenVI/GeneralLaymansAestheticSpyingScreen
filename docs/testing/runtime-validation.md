# 运行验证

## 自动验证

在仓库根目录连续运行两次：

```powershell
.\gradlew.bat spotlessCheck verifyTargets testAll checkAll buildAll collectArtifacts `
  --configuration-cache --configuration-cache-problems=fail `
  --no-daemon --stacktrace --console=plain
```

记录两次结果、第二次 configuration cache 状态、测试数量、产物路径和工作树污染检查。

CI 还会对四个 planned Target 分别请求生命周期、编译、资源处理、Stonecutter 生成、生产归档和运行入口。每个命令必须在离线模式下由 Settings planned 状态门禁拒绝，不执行任何节点任务，且对应 Target 的 build 文件清单不变。

## 客户端验证

1. 确认没有其他进程占用 `run/fabric-26.2/client`；
2. 选择 `saves/` 下与 Minecraft 26.2 兼容的世界；
3. 运行 `:fabric-26.2:runClient`；
4. 用 mcdev-mcp/DebugBridge 确认 Minecraft 版本、gameDir、玩家、维度、位置、世界状态和 framebuffer；
5. 必要时用 computer-use/Orca 完成主菜单、世界选择和键鼠操作；
6. 验证与改动直接相关的 GLASS 交互和画面；
7. 检查 `run/fabric-26.2/client/logs/latest.log`；
8. 正常保存退出并确认 Gradle 任务结束。

运行证据必须来自真实客户端。编译成功、日志仍增长或 Java 进程仍存活都不能替代世界画面与游戏状态。

需要交给用户验收时，先完成上述闭环，再启动一个唯一实例、进入世界并保持运行，同时报告 Target、世界目录、窗口或 PID、日志位置和已知限制。
