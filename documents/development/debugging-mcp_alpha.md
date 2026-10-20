# MCP alpha 兼容性清单

本清单对应 2026-10-19 的 GLASS 参考端接入。固定批次为 `20261019-1245-bfdda69c`，26.3 NeoForge JAR 的 SHA-256 为 `8b443fa8925a1305360fb907b4e4ed7a95bc536ef4414f3cc16f57d350bee158`。原样安装，没有修改 MCP 或补充依赖；清单的 `packagedRuntimeVerified=true`，五个 JAR 与所附手册哈希均已核验。

## 首次世界进入后的相对视角

- 状态：待 MCP 线程核实；通过原版界面恢复后可用，不影响本轮移动、视角、截图与保存退出闭环。
- 环境：Minecraft 26.3、NeoForge 26.3.0.94、Vulkan，项目自己的正常 Loader 安装。MCP 编译 Loader 为手册所列版本；本项未修改该 JAR。
- 复现：在 READ 标题页申请 TAKEOVER，通过语义节点进入本项目世界；窗口处于后台，`hostFocused=false`、`agentPointer.gameCaptured=false`。执行 W 700 ms，再执行 `mouse.delta(dx=36,dy=-12)`，轮询 USER 玩家位姿。
- 期望：视角变化；若当前原生捕获状态不允许输入，则返回清晰的上下文拒绝。
- 实际：输入管线为 `completed`、`GAME_ROUTED_RAW`；玩家前进约 2.88 格，yaw/pitch 在后续读取中维持 -180°/15°。未将管线完成当成视角成功。
- 恢复：通过 MCP 执行原版 Esc 与“返回游戏”，重新读取当前状态后执行一次新的 `mouse.delta(dx=12,dy=4)`；`gameCaptured=true`，yaw/pitch 变为 -178.2°/15.6°。没有宿主键鼠注入、修改 MCP、调试写入或依赖补丁。
- 证据：`integration-mcp_alpha/result/attempt_reference_20261019_211802/` 保存最初尝试；同项 `result/camera-pause-menu.json`、`diagnostic-session.json` 与机器报告保留现场。后续完整复跑通过，当前 `result/reference.json` 为最终结果。

## 2026-10-20 固定快照

渲染原型使用 `20261020-0422-52bc024f`，26.3 NeoForge JAR SHA-256 为 `fd7f7dc6c5c1563955837fd4a932b57c350e3181eaf45bfac1c2ea9d0af0e4a0`。本机分发的七文件原样复制并核验，`packagedRuntimeVerified=true`；只装 MCP 的 READ 和正常退出通过。该批次不包含 CF-072 修复；本轮快速进入独立副本后首个相对转向确实改变 yaw，没有复现，不据此宣称修复。

一项待 MCP 核实的导航回执差异：在此批次的 GLASS 集成世界，通过 `menu.returnToMenu` 保存退出，有时返回 `409 REQUEST_CANCELLED`，但独立观察已到 TitleScreen，世界也已保存。期望是导航完成回执或清晰的已切换状态，实际回执未确认。本项目记录 `not_confirmed`，只观察标题与进程退出，不重放导航；退出应用时另见 `503 RUNTIME_SHUTTING_DOWN`，同样单独检查退出码为零及 endpoint 删除。记录位于渲染验证相应运行的 `save-navigation.json` 和 `exit.json`，没有修改 MCP 或补依赖。

## 当前结论

47 号外部可行性运行仍使用上述 0422 快照，1.21.1 的单 MCP JAR 实例也完成端点／READ／正常退出。两端均再次遇到相同的返回标题 `REQUEST_CANCELLED` 回执，处理仍为记录 `not_confirmed` 后独立观察，没有重放；复现与实际退出证据在 `D:\Workspaces\Scratches\GlassFeasibility` 相应运行的 `navigation.json`、`exit.json`，JAR 身份沿用快照的唯一清单。`tick freeze` 被判为宿主管理指令、模组自定义释放命令被判为未分类指令，均是已声明的命令边界，不报告为缺陷；外部验证探针自行冻结及释放，未修改 MCP。

只装 MCP 的最小端点与 READ、GLASS 参考端进入世界、移动、视角、截图、输入释放、保存返回标题与正常退出均已在本项目复验。渲染运行器先验证相对视角的实际变化，只有复现 CF-072 才使用原版暂停和返回游戏。导航回执与效果分开记录；没有修改 MCP 或补依赖。

此清单供用户转交 MCP 线程；没有向其他线程自动发送消息。它不证明 MCP 全部 UI、命令、世界操作或完整里程碑已通过。
