# 运行验证工具

`runtime_validation` 是 Windows 上以 Python 3.11+ 执行的工具组件，验证 S1、S2、M1、M2 配置并准备静音客户端实例。它不随产品发布。

Gradle `check` 运行配置与测试数据配额检查；`prepareS1`、`prepareS2`、`prepareM1`、`prepareM2` 从对应 `validation.toml` 读取角色，将全局声音置零。运行由模板生成的验证任务承担。数据软限制为 3 GiB、硬限制为 5 GiB；超限时停止并人工核实，清理只送回收站。

参考端的 `smoke-reference_projection` 使用独立 `glass_probe` 模组创建新世界并配置测试场景。`prepareReference` 准备静音实例并归档旧结果；`reviewReference` 校验网络、帧缓冲、图像尺寸与标记颜色计数，保存日志与图像哈希。探针不进入产品 JAR；机器结果明确保留用户视觉验收待完成状态。

`checkPython` 使用固定版本 Ruff 检查工具源码格式，`spotlessApply` 同时执行 `formatPython`。依赖安装在组件自身的 `build/python`，不修改全局 Python。迁移验证入口 `verify-migration` 先格式化、再执行全部门禁与五端构建，保存命令退出码、测试计数和发行 JAR 的 SHA-256。

`mcp_alpha.py` 为 [MCP 接入验证](../../validations/integration-mcp_alpha/validation.md) 准备正常 NeoForge 发行安装，安装官方加载器与经过校验的 Minecraft 依赖，不使用开发 classpath。最小实例只有一个 MCP JAR；参考实例额外安装通过发行检查的 GLASS JAR。参考场景输入由 `reviewReference` 从自身已关闭的测试世界导出 ZIP，不打开用户世界。

运行器从本实例端点绑定 Target、目录、PID、进程开始时间与在线 UUID。控制前检查人工撤销状态，以当前模式版本申请有限 Lease；使用原生语义节点与输入管线，记录真实效果，退出时检查正常停止与端点移除。MCP 只承担操作与采证，GLASS 内部断言仍在项目探针中。配额包含验证结果、安装文件、MCP 快照和验收模组。

同项验证的独立验收实例使用 2560×1440、总音量 50%、音乐关闭和 RecRivenVI。MCP 在交接前确认世界就绪，释放输入与 Lease 并退回 READ；该实例仍在使用快照，因此使用登记保留到验收结束。验收模组仅从 Modrinth 精确匹配，缺少对应版本时记录跳过；该实例保留给用户，不自动宣布视觉通过。
