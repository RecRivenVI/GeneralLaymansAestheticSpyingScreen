# GLASS 目录迁移与开发入口

下文保留首次迁移时的路径、数量与执行结果；后续变化见文末“残留处理更新”，不要把历史记录当作当前运行配置。

本项目是旧 port GLASS。2026-09-09 迁移以 `da59323c918c4bc2a928bce6d2dca860db26e881` 为已保存基线；该 `Update` 提交已 SSH 签名并推送到 `origin/stonecutter`。提交前 HEAD 为 `0f33e54b45b37692c1fbf1fbc127ff46f368feea`。本次迁移本身未提交。

规范来源为 Ravens-mod-template 的 AGENTS.md，SHA-256：`5527638e1fc256cdd2c4594e1d3021de6665257673e7cc092452db1fa490c6b8`。本仓库 [AGENTS.md](../../AGENTS.md) 整合该规范与项目边界；没有恢复已删除的旧 README/docs。

## 源码与构建

| 原位置 | 当前归属 |
| --- | --- |
| `src/main`、`src/test` | `versions/26.2-fabric/src/main`、`src/test`，逐文件原样迁移 |
| `build.fabric.gradle.kts` | `versions/26.2-fabric/build.gradle.kts` |
| 根 Forge/NeoForge 适配脚本 | 对应 `versions/<target>/build.gradle.kts`，仍为 planned，没有新增业务源码 |
| `stonecutter.properties.toml` 的项目身份、公共依赖 | `gradle.properties` |
| 同文件的 Minecraft/Loader/Java/支持状态 | 各 `versions/<target>/target.properties` |
| 根 `stonecutter.gradle.kts` | 保留根 `stonecutter.gradle.kts` 管理拓扑，根 `build.gradle.kts` 协调汇总验证 |
| 旧 `VerifyGlassProject` 门禁 | `eng/structure/Verify-RepositoryLayout.ps1`，由 `verifyTargets` 调用 |
| `eng/runtime` | 保留共享工具，仅同步 Profile 和缓存路径 |
| `run/cache` | ignored `build/runtime-cache` |

Gradle 地址为 `:version:<target>`，中间 `:version` descriptor 显式映射到物理 `versions`。五个注册 Target 中，只有 `26.2-fabric` implemented。其他四个 Target 保留构建适配和 Facts，业务任务在 settings 阶段拦截；不能把维护任务成功视作可构建或已支持。

保留 Stonecutter 0.9.7、Gradle 9.7.1、Loom back-compat 0.4.2 以及全部原依赖版本。Stonecutter 创建时以仓库根为控制器目录，创建后显式将中间 descriptor 映射为 versions，避免生成重复目录；各 Target 显式使用自己的原生 src。没有跨 Target 共享业务源码、非 Mod 产品组件或子模块，不创建 components 占位。

## 运行场景映射

原 `run/profiles/26.2-fabric/client/{primary,secondary}/<profile>.json` 与 `server/dedicated/m2.json` 改为下表场景内的 `task/profiles/26.2-fabric/<variant>.json`。原 `run/instances/26.2-fabric/...` 对应数据原样迁入场景内的 `instance/26.2-fabric/<variant>`。

| Profile | 验证目录 | variant |
| --- | --- | --- |
| PRIMARY S1 | `validations/regression-clean-session` | `client` |
| PRIMARY S2 | `validations/compatibility-client-mods` | `client` |
| PRIMARY/SECONDARY M1 | `validations/integration-multiplayer-session` | `client` / `client-multiplayer` |
| PRIMARY/SECONDARY/SERVER M2 | `validations/integration-dedicated-session` | `client` / `client-multiplayer` / `server` |

这些是已有专项配置，不合并为日常实例。当前没有单独的日常实例数据，因此没有创建空 `instances`；以后真正的日常实例使用 `instances/<target>/{client,client-multiplayer,server}`。也未制造无使用者的 instance.properties/local.properties；本机 local.properties 已忽略。

原覆盖语义保持：`-Pruntime.profile` 默认 S2，PRIMARY 使用请求值；SECONDARY 仅在请求 M2 时使用 M2，否则使用 M1；SERVER 固定 M2。`-Pruntime.world` 仍只用于 PRIMARY 的快速进入世界参数。原玩家名、2560x1440、Vulkan、JVM 参数、测试模组版本/哈希与 mods 目录加载方式保留。

`run/instances/legacy/generallaymansaestheticspyingscreen-pre-clean-break-20260816` 是历史旧身份实例，原路径与内容保持不动，不接入活动任务。外部历史 Artifacts、签名证据、其他仓库和共享平台未迁移或改写。历史正文中的旧路径不做替换。

## 从仓库根运行的开发检查

```powershell
.\gradlew.bat projects tasks --all
.\gradlew.bat spotlessCheck verifyTargets testAll checkAll buildAll collectArtifacts
.\gradlew.bat :version:26.2-fabric:test :version:26.2-fabric:build
powershell -NoProfile -File eng/runtime/Verify-RepositoryProfiles.ps1
.\gradlew.bat :version:26.2-fabric:runClient '-Pruntime.profile=S1' --dry-run
```

构建 JVM 需要 JDK 25+；目标工具链仍按 target.properties 配置。产物仍为 `build/libs/8.0.0-alpha.1/glass-fabric-8.0.0-alpha.1+26.2.jar`，不安装到任何实例 mods。Java 单元测试保留在 Target 的 src/test，使用原 JUnit 入口。

各场景的验证说明见 [S1](../../validations/regression-clean-session/validation.md)、[S2](../../validations/compatibility-client-mods/validation.md)、[M1](../../validations/integration-multiplayer-session/validation.md)、[M2](../../validations/integration-dedicated-session/validation.md)。Profile 工具只检查运行前提，不能证明实际共同工作、多人同步或视觉正确。本次不启动游戏、不进行人工验收；历史 109 项测试 PASS 不作为本次结果。

本次临时构建日志、迁移前清单和内容校验放在 ignored `build/migration-20260909`。只有真实执行结果才记为当前 PASS；不会为未执行的产品验证创建 result 文件或声明 parity。

## 本次执行结果

- PASS：最终 Wrapper `projects tasks --all` 和 `spotlessCheck verifyTargets testAll checkAll buildAll collectArtifacts`；相同命令再次运行显示 `Configuration cache entry reused`。
- PASS：本轮实际执行 JUnit 109 项，36 个 suite，0 failures/errors/skipped；后续仅构建接线变更时任务复用。
- PASS：7/7 Profile 的目录、角色、测试模组和 SHA-512 检查。S1/M2 的三角色 run dry-run 及配置断言通过；所有游戏任务均 SKIPPED。`runtimeClasspath` 的 dependencyInsight 未发现 Mod Menu。
- PASS：4/4 planned Target 的显式 build 在加载器配置前被拒绝；不视作这四个 Target 构建通过。
- PASS：164 个源码/资源文件、786 个迁移场景文件、837 个原位 legacy 文件和 2 个缓存文件，共 1,789 个文件保留原内容；首次迁移核对大小、SHA-256、修改时间与属性全部一致。7 个 Profile 除路径之外的语义不变，当前文档相对链接有效。
- 生产 JAR 的类和资源条目与迁移前本地 JAR 一致。仅 Manifest 的 `Fabric-Loom-Version` 从 1.17.19 变为 1.17.20；配置仍是原有 `1.17-SNAPSHOT`，没有手动升级版本声明。该差异属于动态依赖解析，不能宣称整个 JAR 的 SHA-256 相同。
- 未执行：游戏启动、实际兼容/多人交互、视觉或人工验收、部署；未修改其他仓库、模板或全局工具配置。

调试期间的失败日志保留在同一 ignored 证据目录，包括 descriptor 注册、任务属性作用域、Windows PowerShell API/命令参数、格式检查以及临时运行接线探针的 Gradle 生命周期问题；修正后以上检查通过，没有将失败记录删除或改写为 PASS。签名验证采用临时 allowed-signers 文件，不修改全局 Git 配置。

自动审批拒绝了清理旧路径空目录的命令（仅返回 blocked by policy），因此旧 `run/profiles` 及 `run/instances/26.2-fabric` 下残留的空目录保留。它们无活动文件、不参与运行接线，不以强制删除处理。历史 legacy 数据也继续原位保留。

## 残留处理更新

在用户追加授权后，仅修复本仓实际存在的检查风险：布局检查现在包含隐藏项、空嵌套目录、验证项目和日常实例边界，正确区分普通文档与 ADR/Phase；不会把原生 task/result/instance 内容套入文档命名规则。未发现旧 IDE 配置或现行说明推荐不兼容历史门禁，因此没有为这些类别添加新入口或修改历史脚本。

当前直接检查入口为 `powershell -NoProfile -File eng/structure/Verify-RepositoryLayout.ps1`，Gradle `verifyTargets` 仍调用它。`ERRORS=0; REVIEW_REQUIRED=N` 只表示已实施规则未发现错误，明确不代表所有残留均已归属。旧 run 和 build 内迁移证据保持待决，不通过永久豁免伪装合规。保存在 `build/migration-20260909` 的 migrate.py、init.gradle 和失败日志属于历史过程材料，不是当前操作入口，不应重跑迁移程序。

历史任务“核对 GLASS 项目上下文”（`019ff1d0-45c4-73f0-9cce-c9929bc270b0`）中，2026-08-11 用户要求“继续操控同一个 MC 实例”；2026-08-12 用户说明自己调整过终端，2026-08-14 又说明保存了世界。2026-08-15 的迁移记录确认原客户端从 run/26.2-fabric/client 迁到 run/fabric-26.2/client 后继续共同测试；2026-08-24 将这一旧 port 实例整体保全到 legacy。固定测试装置建立在同一个连续使用的世界中，不据此拆分 region 文件。

本次归为 B 类，将 `run/instances/legacy/generallaymansaestheticspyingscreen-pre-clean-break-20260816/26.2-fabric/client` 整体移到 `instances/26.2-fabric/client`。移动前未发现仓库 JVM 占用，目标不存在；791 个文件与已有独立备份逐项哈希相同。移动后 791 文件、244 目录（含实例根）、103,302,964 字节，路径、内容哈希、大小、创建/修改时间及属性差异为 0，源客户端目录已不存在。未合并或覆盖任何世界/配置，ignored 策略已核对。

原客户端目录外的 46 个历史日志、原迁移证据的 38 个文件、本机缓存和旧空壳均未移动或清理。原迁移证据的哈希未变化。当前没有新增 A 类独立固定场景，B 类仅上述完整客户端；日志不强行认作另一实例。已有 S1/S2/M1/M2 验证环境保持不变。

本次结构回归使用 ignored build/structure-regression 中的临时目录样例，不编译或启动这些样例，不计作产品 Target。实际产品的源码测试仍留在 versions/26.2-fabric/src/test。本次不启动游戏、不进行人工验收，也不提交迁移或后续修复。
