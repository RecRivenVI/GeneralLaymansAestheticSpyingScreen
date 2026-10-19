# 构建与运行依赖

加载器版本与 Java 版本唯一来源为各 Target 的 `target.properties`。2026-10-19 从官方 Maven 与 Fabric Meta 核对版本；工具版本沿用 2.0.2 的 `gradle/libs.versions.toml`。

| 依赖 | 来源 | 用途 |
| --- | --- | --- |
| Ruff | [官方发行](https://pypi.org/project/ruff/) | 工具组件的 Python 格式检查；版本固定在版本目录，安装到组件自己的 build 虚拟环境 |
| Forge | [官方 Maven](https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml) | 1.20.1 加载器 |
| NeoForge | [官方 Maven](https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml) | 三个 NeoForge Target |
| Fabric Loader | [Fabric Meta](https://meta.fabricmc.net/v2/versions/loader/26.3) | Fabric 26.3 加载器 |
| Fabric API | [Fabric Maven](https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml) | Fabric 26.3 注册、生命周期与网络 |
| Mod Menu | [上游发布](https://github.com/TerraformersMC/ModMenu) | 21.0.0，Fabric 编译时 `compileOnly`，只在 S2 物理加载 |
| CSL Universal | [上游发布](https://github.com/xfl03/MCCustomSkinLoader/releases/tag/v15.0.1) | 15.0.1 Universal，只在 S2 物理加载 |
| MCP alpha | 固定分发快照 `20261019-1245-bfdda69c` | 参考端自动测试；完整发行 JAR 原样安装，不增加依赖 |
| 验收 CSL Universal | [Modrinth 精确版本](https://modrinth.com/mod/customskinloader/version/yyYvYfZI) | 15.1 Universal，26.3 NeoForge 用户验收实例 |
| Zume | [Modrinth 项目](https://modrinth.com/mod/zume) | 2026-10-19 未找到 26.3 NeoForge 的精确版本，验收实例跳过 |

Mod Menu 与 CSL 的版本、元数据身份及允许哈希见 `inputs.toml`，本机路径在 ignored `local.toml`。CSL 的 Fabric 元数据 ID 是 `customskinloader-bootstrap`、版本为 `15.0.1`，文件发行名为 Universal。S1 与服务端不加载测试模组。产品由加载器 development classpath 提供，不复制自身 JAR 到实例。

MCP 及验收 CSL 的本地输入独立登记，均不进入 GLASS 发行包；MCP 复制自已登记且未撤回的同批固定快照。单 JAR 最小实例与后续参考实例均使用正常 Loader 安装；这里的发行安装与模板开发运行各自隔离。Modrinth 查询与下载哈希保存在本项目产物目录，精确 Minecraft 与 Loader 匹配之外不回退安装。
