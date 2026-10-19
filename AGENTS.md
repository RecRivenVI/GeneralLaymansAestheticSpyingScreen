# 仓库目录与命名规范

本文件适用于整个仓库，定义 Minecraft Multi-Target Mod、非 Mod 组件、运行实例、文档和产品验证的目录职责与命名方式。

## 1. 总体结构

```text
Project/
├── versions/<target>/
│   ├── src/
│   ├── build.gradle.kts
│   └── target.properties
├── instances/<target>/
│   ├── client/
│   ├── client-multiplayer/
│   └── server/
├── components/<component>/
├── documents/
│   ├── usage/
│   ├── development/
│   ├── design/
│   ├── project/
│   ├── reference/
│   └── release/
├── validations/<type>-<topic>/
│   ├── task/
│   ├── instance/
│   ├── result/
│   ├── validation.md
│   └── validation-guide.md
├── README.md
├── AGENTS.md
├── instance.properties
├── local.properties
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .git/
└── .gitignore
```

目录树表示内容的归属和允许的位置，不要求为展示结构而创建空目录。`<target>`、`<component>`、`<type>`、`<topic>` 和 `<number>` 是命名占位符，使用时替换为实际名称或编号。

## 2. Minecraft Target：versions/

`versions/` 保存 Minecraft Multi-Target Mod 的源码与构建配置。每个 `versions/<target>/` 等价于一个 Minecraft Mod 源码根目录，其内部源码、资源、构建配置和代码风格遵循对应生态。

### Target 命名

格式为 `<minecraft>-<loader>`：

- `<minecraft>` 使用 Minecraft: Java Edition 版本号。
- `<loader>` 使用小写加载器名称，仅允许 `neoforge`、`forge`、`fabric`、`quilt`。
- 示例：`1.20.1-forge`、`1.21.1-neoforge`、`26.1.2-neoforge`、`26.2-fabric`、`26.2-neoforge`。

### Target 文件职责

| 位置 | 职责 |
| --- | --- |
| `src/` | 对应生态的原生源码与资源布局 |
| `build.gradle.kts` | Target 插件、依赖、构建与运行适配 |
| `target.properties` | Minecraft、Loader、Java 等 Target Facts |

Gradle project path 使用单数命名空间：

```text
versions/<target>/ → :version:<target>
```

物理目录仍为复数 `versions/`，由根 `settings.gradle.kts` 显式注册并映射到相应的 Gradle project path。

## 3. Minecraft 实例：instances/

`instances/` 保存 Minecraft Multi-Target Mod 的运行实例。`instances/<target>/` 中的 `<target>` 与对应源码 Target 的名称一致。

| 目录 | 职责与内部布局 |
| --- | --- |
| `client/` | 客户端实例；本目录等价于 `.minecraft/`，直接存放 Minecraft 实例数据 |
| `client-multiplayer/` | 多人游戏客户端实例；本目录等价于 `.minecraft/`，直接存放 Minecraft 实例数据 |
| `server/` | 服务端实例；本目录等价于 NeoForge、Forge、Fabric、Quilt 服务端安装目录 |

客户端实例内部不再套一层 `.minecraft/`；服务端保持相应生态的原生安装目录布局。实例目录是运行数据的位置，不是独立的 Mod 源码模块。

## 4. 非 Mod 组件：components/

`components/` 保存非 Minecraft Mod 的产品组件。每个 `components/<component>/` 是对应生态的传统源码根目录，内部结构与代码风格遵循该生态。

### Component 命名

- 按组件职责命名，使用小写英文字母、数字和单词间的连字符 `-`。
- 连字符用于连接单词，不放在名称首尾，也不连续使用。
- 同一项目内组件名不得重复。
- 避免 `common`、`utils`、`misc` 等职责不明确的名称。
- 示例：`protocol-schema`、`runtime-safety`、`companion`、`web-console`、`config-editor`。

参与 Gradle 构建的组件使用单数命名空间：

```text
components/<component>/ → :component:<component>
```

物理目录仍为复数 `components/`。其他生态组件保留自己的原生构建入口，不因位于 `components/` 就必须成为 Gradle 模块。

## 5. 项目文档：documents/

`documents/` 仅使用以下六个一级分类。各分类下直接放文档，不再创建分类子目录；根目录不放普通项目文档，没有内容的分类不要求创建空目录。

| 分类 | 主要职责 | 推荐的 `<type>` |
| --- | --- | --- |
| `usage/` | 产品安装、配置、操作与使用问题处理 | `installation`、`configuration`、`guide`、`troubleshooting`、`compatibility`、`limitation`、`faq` |
| `development/` | 开发环境、构建、协作、测试、调试与移植 | `setup`、`build`、`workflow`、`testing`、`debugging`、`porting`、`contribution`、`compatibility` |
| `design/` | 系统概念、架构、机制、模型、流程、生命周期与安全设计 | `overview`、`concept`、`architecture`、`mechanism`、`model`、`flow`、`lifecycle`、`security` |
| `project/` | 产品范围、提案、计划、状态、里程碑、技术债与阶段记录 | `overview`、`scope`、`proposal`、`plan`、`status`、`milestone`、`debt` |
| `reference/` | 接口、协议、结构、命令、配置、属性、事件、格式与术语的查阅资料 | `api`、`protocol`、`schema`、`command`、`configuration`、`property`、`event`、`format`、`version`、`glossary`、`example` |
| `release/` | 发布说明、变更、升级、迁移、支持与弃用 | `release`、`changelog`、`upgrade`、`migration`、`support`、`deprecation` |

普通文档统一使用 `<type>-<topic>.md`。另有两种专用格式：

```text
documents/design/adr-<number>-<topic>.md
documents/project/phase-<number>-<topic>.md
```

- `adr`：架构或设计决策记录。
- `phase`：阶段目标、实施与历史记录。

### 文档命名要素

- `<type>` 使用单个简短的小写英文词，表示整份文档的主要形式或职责。
- 推荐词只是优先词汇，不构成白名单；没有合适推荐词时，允许根据实际职责选择新的 `<type>`。
- 同一个 `<type>` 可以出现在不同分类中，不要求全局唯一；分类决定主要职责，`<type>` 只在所在分类语境内解释。
- `<topic>` 使用小写英文字母、数字和连字符；不允许首尾连字符或连续连字符，可以由字母或数字开头、结尾。
- `<number>` 使用自然数编号，允许 `0`；可在编号后使用点号连接子编号，区分同编号下的子文档，例如 `1`、`2`、`2.1`、`2.10`。它表示编号层级，不按数学小数合并不同编号。

这些规则约束文档位置和文件名，不规定正文的固定章节或格式。按整份文档的主要职责选择分类与 `<type>`，允许正文同时涉及多个相关主题；不要为了追求分类纯度或覆盖推荐类型而拆分、创建文档。

### 容易混淆的职责边界

| 文档位置 | 主要职责 |
| --- | --- |
| `usage/configuration-*` / `reference/configuration-*` | 前者讲用户如何配置；后者提供配置项的准确、完整定义 |
| `usage/compatibility-*` / `development/compatibility-*` | 前者面向用户说明环境、Mod、硬件等能否正常使用；后者面向开发和集成过程说明兼容要求 |
| `design/overview-*` / `project/overview-*` | 前者说明系统内部如何组成和运行；后者说明产品或项目整体是什么、范围与方向是什么 |
| `development/porting-*` | 源码跨 Minecraft 版本、Loader、平台或 API 的移植 |
| `release/upgrade-*` | 用户或部署方升级版本的说明 |
| `release/migration-*` | 版本变化引起的数据、配置、接口或部署迁移 |
| `project/plan-*` | 已确定方向的实施计划，也可包含尚未实施的内部迁移方案 |

### 标准文件与现有内容

`documents/` 的治理规则不禁止 GitHub、许可证、构建工具或其他生态约定要求放在仓库根目录的标准文件，例如 `README.md`、`LICENSE`、`CONTRIBUTING.md`、`SECURITY.md`、`CODE_OF_CONDUCT.md`。这些文件保留其标准名称和位置，不因文档目录规范而擅自移动，也不要求套用 `<type>-<topic>.md`。

规则允许的位置与类型不等于必须创建的文件清单。规范调整本身不授权创建、拆分、移动现有文档或清理旧目录；实际整理应根据用户明确的任务范围处理。

## 6. 产品验证：validations/

`validations/` 用于产品级验收、回归、集成、兼容、跨 Target 一致性、协议符合性、性能、稳定性、安全性以及长期机器证据。它不等同于普通单元测试、JUnit 或 GameTest；普通源码测试仍属于对应源码和构建体系，不为统一目录而搬入这里。

每项产品验证按 `validations/<type>-<topic>/` 集中组织，将任务、运行实例、结果与说明放在同一个验证项目下。

### 验证项目命名

- `<type>` 表示验证的主要目的，使用单个简短的小写英文词。
- 推荐：`feature`、`regression`、`integration`、`compatibility`、`parity`、`conformance`、`performance`、`stability`、`migration`、`security`。
- 推荐词只是优先词汇，不构成白名单；没有合适推荐词时，允许根据验证的实际目的选择新的 `<type>`。
- `<topic>` 表示被验证的能力、问题或边界，使用与文档主题相同的字符规则。

以下三种验证目的应明确区分，不混用：

| `<type>` | 要证明什么 |
| --- | --- |
| `compatibility` | 不同环境、Mod、组件等能否共同工作 |
| `parity` | 不同 Target / Loader 的行为是否一致 |
| `conformance` | 是否符合明确的协议、Schema 或合同 |

### 验证项目内容

| 位置 | 职责 |
| --- | --- |
| `task/` | 验证程序、断言、输入与辅助工具；不限制内部目录结构 |
| `instance/` | 可选的验证专用实例；保存实例运行内容，以及不值得长期保存的验证结果与机器证据 |
| `result/` | 从 `instance/` 中提取的值得长期保存的验证结果与机器证据；不限制内部目录结构 |
| `validation.md` | 面向开发者和 Agent 的完整测试目标、测试内容、操作说明与验收说明 |
| `validation-guide.md` | 可选的简易版说明，面向一般用户介绍测试目标、测试内容、操作与验收；尽可能简洁易懂，并提供可直接从仓库根目录运行的命令 |

`validation.md` 和 `validation-guide.md` 直接放在验证项目根目录，不放进 `task/`，也不额外套一层 `documents/`。两份说明有不同的读者定位，不要求固定正文模板。

### 验证实例的 Git 策略

`validations/<type>-<topic>/instance/` 默认忽略运行内容，同时允许 Git 遍历子目录；通过 `.gitignore` 的显式 negation 白名单放行指定文件。

不要将父目录整体排除到无法重新包含指定文件。长期结果放入 `result/`，运行数据和临时证据留在默认忽略的 `instance/`。

## 7. 根文件与基础设施

| 文件或目录 | 职责 |
| --- | --- |
| `README.md` | 面向人的模板说明与使用入口 |
| `AGENTS.md` | Coding Agent 的仓库开发规范 |
| `instance.properties` | 仓库共享的实例运行默认值 |
| `local.properties` | 本机工具路径与实例覆盖；Git ignored |
| `gradle/` | Gradle 基础设施 |
| `gradle/wrapper/` | 官方 Gradle Wrapper |
| `gradle/wrapper/gradle-wrapper.jar` | Wrapper 启动与发行包下载程序 |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 版本、下载地址与校验值 |
| `gradlew` | Unix Gradle Wrapper 入口 |
| `gradlew.bat` | Windows Gradle Wrapper 入口 |
| `build.gradle.kts` | 仓库共同构建协调 |
| `settings.gradle.kts` | 显式注册分层 Gradle 模块及其目录映射 |
| `gradle.properties` | 项目身份、版本与运行环境 Profile |
| `.git/` | Git 元数据 |
| `.gitignore` | 构建输出、本机配置与实例内容的忽略规则 |

## 8. GLASS 项目约束

- 本项目是旧 port GLASS，mod ID 为 `glass`，包名为 `io.github.recrivenvi.glass`，发布版本保持 `8.0.0-alpha.1`。不要操作 Second Implementation、其他仓库或共享模板。
- 本次规范来源是 Ravens-mod-template 的 AGENTS.md，SHA-256 为 `5527638e1fc256cdd2c4594e1d3021de6665257673e7cc092452db1fa490c6b8`。
- 当前仅 `26.2-fabric` implemented。其他四个已注册 Target 保留 planned 状态、Facts 和构建适配，禁止把生成目录或维护任务成功视作版本支持。
- Stonecutter 0.9.7 继续管理版本拓扑；`:version` 映射到 `versions`，控制器保留在根 `stonecutter.gradle.kts`。各 Target 自己的 src 是编译输入，不使用根共享 src，也不创建占位组件。
- Wrapper 固定 Gradle 9.7.1，运行构建需要 JDK 25+；依赖版本及 Target Java Facts 按现有配置保留。项目身份在 gradle.properties，版本事实在各 target.properties，验证实例配置仅在对应 task/profiles JSON 中。
- `eng/structure` 是仓库规范检查；`eng/runtime` 是多个验证场景共享的 Profile 工具，不是产品组件或独立验证结果。
- 布局检查必须包含隐藏项及空嵌套目录，但只在受治理的目录边界实施命名规则；不得将 Minecraft 实例、验证 task/result 内部或原生构建缓存按普通文档规则检查。推荐 type 不是白名单，标准根文件和原生构建布局不因未画在示意树中而违规。
- S1/S2/M1/M2 保持独立验证场景。默认 PRIMARY=S2；SECONDARY 在请求 M2 时使用 M2，否则为 M1；server 固定 M2。客户端为 Vulkan、2560x1440，PRIMARY=RecRivenVI，SECONDARY=CNN_MOYAN。命令参数包含空格时仍按独立参数传递。
- CSL 15.0.1-Universal 是测试模组；Mod Menu 20.0.1 是兼容目标，编译使用 modCompileOnly，仅非 clean 客户端物理加载。S1 和 server 不安装这些测试模组，Zume 不适用。
- 本体使用 Loader development classpath；buildAndCollect 只归集构建产物，不向实例 mods 安装产品 JAR。
- 原旧身份客户端已依据连续共同测试的历史记录，整体归入 `instances/26.2-fabric/client`；这是原数据归位，不是存档身份转换。现行 Fabric run 入口仍使用既定验证 Profile，不默认加载这个旧身份世界。`run/instances/legacy` 剩余历史诊断日志继续保护，外部历史证据保持原位。
- 对上述 legacy 的保护不是永久旧布局豁免；需以明确历史依据判定固定场景、共同连续使用的日常实例或归属不明数据。证据不足时保留并报告 `REVIEW_REQUIRED`，不得把检查器退出码为 0 或历史构建通过解释为所有布局均已合规。`build/migration-20260909` 含不可重建的迁移前材料，在用户决定保存方案前不得作为普通缓存清理。
- 构建、单元测试、Profile 检查不能替代真实客户端或视觉验收。历史 PASS 不代表当前 PASS，多客户端分别运行不代表 parity；用户视觉验收仍待完成。
- 不自行恢复旧 README/docs，不新增 GitHub Workflow。开发入口与本次路径映射见 documents/development/migration-repository-layout.md。
- 当前结构迁移成果只留在 working tree；本轮不得启动游戏、部署、提交、推送或创建分支/tag。迁移前的 Update 提交授权已经用完。
