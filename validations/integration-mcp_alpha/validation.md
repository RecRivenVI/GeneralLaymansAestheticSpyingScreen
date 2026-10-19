# MCP alpha 接入

## 目标

固定并核验 MCP 快照，在独立正常 NeoForge 安装中先完成单 JAR 最小 READ，再用发行 GLASS 与 MCP 完成进入项目新夹具世界、移动、视角、截图和保存退出。GLASS 内部状态检查仍由参考投影探针负责；MCP 不替代该探针或人工视觉验收。

## 运行

从仓库根目录执行；`local.toml` 的本地输入路径须已登记，原快照须按所附手册登记使用。实例目录必须为新目录，保留已有尝试，不覆盖旧实例。

```powershell
.\gradlew.bat :runtime_validation:installMcpRuntime
.\gradlew.bat :runtime_validation:smokeMcpMinimal
.\gradlew.bat :runtime_validation:smokeMcpReference
```

自动测试退出后，按用户验收预设启动同项验证下独立的验收实例：

```powershell
.\gradlew.bat :runtime_validation:launchAcceptance
```

验收实例同样位于本项验证的专用目录。MCP 只用于准备世界、核对预设，交接前释放输入与 Lease、退回 READ；快照使用登记在验收结束前保留。Custom Skin Loader 从 Modrinth 精确匹配 26.3 NeoForge，Zume 无匹配版本则跳过。验收实例持续运行，视觉由用户确认。

上述 Gradle 任务调用工具组件的同名 Python 入口；也可直接执行 `python components/runtime_validation/mcp_alpha.py` 并传入 `install`、`minimal`、`reference` 或 `acceptance`。完整门禁入口为 `python components/runtime_validation/mcp_alpha.py verify`，依次运行格式化、`check` 与 `verifyRelease`。本项验证自行安装正常 Loader，没有 `[instances]`；模板生成的开发运行始终加载产品模组，无法提供用户要求的单 MCP JAR 最小基线。

## 通过标准

最小实例只安装一个完整 MCP JAR；快照及手册哈希匹配，清单的 `packagedRuntimeVerified` 为真。绑定端点目录、Target、PID、进程开始时间与在线 UUID，READ 成功。

参考实例只加载 MCP 与已通过发行检查的 GLASS。报告记录真实玩家位姿变化、1920×1080 截图、输入释放、保存返回标题和正常退出；端点移除，无崩溃。自动实例静音，数据软限制 3 GiB、硬限制 5 GiB；清理仅使用回收站。

验收实例使用 2560×1440、总音量 50%、音乐 0、玩家名 RecRivenVI；用户视觉确认单独记录，不由自动报告宣称通过。
