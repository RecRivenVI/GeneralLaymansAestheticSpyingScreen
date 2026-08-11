# AGENTS.md

本文件约束在 General Layman's Aesthetic Spying Screen（GLASS）仓库中工作的全部编码 Agent。违反本文件的行为视为缺陷。

GLASS 由 RecRivenVI 独立维护。`upstream` 只用于只读查询原始实现及历史，不是当前项目身份、开发基线或发布目标。本文件的独立维护治理重写已由用户明确授权；此后除非用户再次明确授权，Agent 不得自行编辑、删除、重命名或覆盖 `AGENTS.md`。

## 1. 开始工作前

依次阅读并核对：

1. `AGENTS.md`；
2. `README.md`；
3. `docs/architecture/project-layout.md`；
4. `docs/development/getting-started.md`；
5. `docs/development/compatibility-policy.md`；
6. 与任务有关的 `docs/testing/**` 和 `docs/reference/**`；
7. 当前分支、`git status --short --branch --untracked-files=all` 与 `git remote -v`。

保留用户已有改动。不得用 `reset --hard`、破坏性清理或上游内容覆盖工作树。未经用户明确授权，不得提交、推送、打标签、发布或改写历史；任何情况下都不得向 `upstream` 推送。

## 2. 项目身份与兼容边界

- 公开仓库：`https://github.com/RecRivenVI/GeneralLaymansAestheticSpyingScreen`；
- 当前维护者：RecRivenVI；
- 公开 mod ID、资源命名空间和存档兼容标识保持不变；
- 原作者、原仓库和行为参考提交记录在 `CREDITS.md` 与 `docs/reference/upstream-behavior.md`；
- 许可证文件和源码中的版权声明不得删除或弱化；
- 外部模组兼容逻辑必须由 GLASS 自身承载，不要求第三方模组重新编译。

## 3. Target 契约

Target ID 严格使用 `<loader>-<minecraft-version>`。loader 只能是 `fabric`、`forge` 或 `neoforge`。

| Target | 状态 | 边界 |
| --- | --- | --- |
| `forge-1.20.1` | `planned` | 注册并受门禁保护，不得声明可运行或可发布 |
| `neoforge-1.21.1` | `planned` | 注册并受门禁保护，不得声明可运行或可发布 |
| `neoforge-26.1.2` | `planned` | 注册并受门禁保护，不得声明可运行或可发布 |
| `fabric-26.2` | `implemented` | 当前活动、构建、测试和客户端验收目标 |
| `neoforge-26.2` | `planned` | 注册并受门禁保护，不得声明可运行或可发布 |

默认活动目标由 `versions/current` 声明，必须是 `fabric-26.2`。所有目标在 `stonecutter.properties.toml` 中声明 `target.status`、`target.client` 和 `target.server`；不得用空实现或伪造状态绕过 planned 门禁。

## 4. 源码、产物与实例布局

- `src/main/java`、`src/main/resources`、`src/test/java`：跨目标共享内容；
- `versions/<target>/src/**`：目标专属入口、API、网络、Mixin、渲染、资源与测试；
- `build.planned.gradle.kts`：planned 节点的轻量构建脚本，不应用 Minecraft Loader 插件；
- `gradle/stonecutter-target-conventions.gradle.kts`：Target 解析、状态、运行侧、产物与 planned 快速失败门禁；
- `build.<loader>.gradle.kts`：加载器插件、工具链、依赖、运行配置和资源处理；
- `stonecutter.properties.toml`：`mod.*`、`deps.*`、`target.*` 中央属性；
- `run/<loader>-<minecraft-version>/<side>/`：隔离运行实例；
- `build/artifacts/<loader>-<minecraft-version>/`：收集后的生产产物；
- 产物名：`<modid>-<mod-version>+<loader>-<minecraft-version>.jar`。

节点本地文件可覆盖共享同路径文件。只有少量映射或 API 差异时才使用 Stonecutter `//?` 条件；整类加载器实现留在目标目录，不复制整个共享包。`build/`、`run/`、日志、世界、截图、录像、GPU 捕获和 DebugBridge 数据均不得提交。

## 5. 编码与格式

- Gradle 使用 JDK 25；目标源码工具链由相应加载器脚本声明；
- 所有受管文本为 UTF-8、LF、无行尾空白并以换行结尾；
- Java 使用 google-java-format AOSP；Kotlin/Kotlin DSL 使用 ktlint；
- 不自动改写 `COPYING`、`COPYING.LESSER` 或其他许可证正文；
- 加载器 API 不得泄漏到共享纯逻辑；
- 新增或修复逻辑必须同步增加职责级单元或契约测试；
- 渲染、GUI、声音、网络、Mixin 和运行配置还必须实际启动目标客户端验证；
- 不把内部实现暴露成无必要的 public API，不以任意等待、静帧或状态泄漏掩盖渲染生命周期问题。

格式入口：

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat spotlessCheck
```

## 6. 构建、测试与运行

单目标入口：

```powershell
.\gradlew.bat :fabric-26.2:test
.\gradlew.bat :fabric-26.2:check
.\gradlew.bat :fabric-26.2:build
.\gradlew.bat :fabric-26.2:runClient -Pruntime.world=<save-directory> -Pruntime.username=<profile>
.\gradlew.bat :fabric-26.2:runServer
```

仓库级入口：

```powershell
.\gradlew.bat spotlessCheck verifyTargets testAll checkAll buildAll collectArtifacts `
  --configuration-cache --configuration-cache-problems=fail `
  --no-daemon --stacktrace --console=plain
```

完整命令必须连续成功两次，第二次确认 configuration cache reused。`checkAll` 必须包含 `spotlessCheck`。planned Target 的生命周期、编译、源码生成、归档与运行请求必须在 Settings 任务选择阶段快速失败，不得执行任何该节点任务、应用 Loader 插件、解析 Minecraft/Loader 依赖或依靠 `dependsOn` 排序。

`fabric-26.2:runClient` 的 gameDir 固定为 `run/fabric-26.2/client`，并必须保留 Vulkan 后端参数。不得增加 PowerShell 启动脚本、批处理编排器或仓库特有运行聚合任务；所有运行只通过统一 Gradle 入口。

## 7. 客户端验收

自动验证通过后，对 `fabric-26.2` 完成真实客户端闭环：

1. 使用 `:fabric-26.2:runClient` 和迁移后的兼容世界；
2. 确认 Vulkan 后端、正确 gameDir、Minecraft 版本与 mod 加载状态；
3. 使用 mcdev-mcp/DebugBridge 读取玩家、维度、位置、世界状态和真实 framebuffer；
4. 必要时使用全局 computer-use/Orca 完成可见 UI 操作；
5. 检查 `run/fabric-26.2/client/logs/latest.log`，确认没有致命异常；
6. 自动回归正常保存退出并等待 Gradle 任务结束；
7. 需要用户验收时再启动唯一一个实例，进入世界并保持运行。

日志或进程存活不能替代可见世界画面和语义状态证据。当前功能状态与已知限制记录在 `README.md` 和 `docs/testing/acceptance-matrix.md`，不得用时间绑定的历史进度叙事掩盖回归。

## 8. Git 与上游

- 日常开发分支为 `stonecutter`；
- `origin` 是 RecRivenVI 仓库；
- `upstream` fetch 保留原仓库，push URL 必须禁用；
- 不删除许可、版权、公开 mod ID、资源命名空间或存档兼容标识；
- 不把上游源码快照重新放入当前源码树；需要追溯时使用 Git 历史、只读 remote 和参考文档；
- 获得提交授权后，只暂存源码、文档和配置，并在提交前运行完整门禁与 `git diff --check`。
