# General Layman's Aesthetic Spying Screen

GLASS 是由 RecRivenVI 独立维护的多版本 Minecraft Java 模组。它把 Terminal 所在位置的实时世界画面投影到由 Projector、Extension 和 Wireless Extension 组成的显示表面，并保留视差、远端实体与世界效果。

## 当前支持矩阵

| Target | Minecraft | Loader | 状态 |
| --- | --- | --- | --- |
| `forge-1.20.1` | 1.20.1 | Forge | Planned |
| `neoforge-1.21.1` | 1.21.1 | NeoForge | Planned |
| `neoforge-26.1.2` | 26.1.2 | NeoForge | Planned |
| `fabric-26.2` | 26.2 | Fabric | Implemented |
| `neoforge-26.2` | 26.2 | NeoForge | Planned |

`versions/current` 指向默认活动目标 `fabric-26.2`。这五项是规划集合；当前实现集合只有 `fabric-26.2`。Planned Target 保留在项目树和结构检查中，使用不应用 Loader 插件的轻量脚本，并受编译、生成、归档与运行快速失败门禁保护，不代表可用、可运行或可发布。

## 主要功能

- 通过公开或私有频道连接 Terminal 与 Projector；
- 在六个方向上投影实时世界画面并保留玩家视差；
- Projector 与 Extension 组成可绕角传播的普通显示表面；
- Wireless Extension 组成可排序的无线三角带；
- 红石传播、动态拓扑重算、热切换频道和可配置显示动画；
- 独立远端世界渲染、实体、粒子、天气、天空与出口面裁切；
- Mod Menu 配置入口，以及中文和英文界面。

## 开发环境

- Windows；
- JDK 25 用于 Gradle；
- Gradle Wrapper 9.7.0；
- Stonecutter 0.10；
- Fabric 26.2 的目标工具链和 Vulkan 后端依赖由 Gradle 自动解析。

详情见 [开发环境](docs/development/getting-started.md)。

## 构建、测试和运行命令

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat spotlessCheck verifyTargets testAll checkAll buildAll collectArtifacts `
  --configuration-cache --configuration-cache-problems=fail `
  --no-daemon --stacktrace --console=plain
.\gradlew.bat :fabric-26.2:runClient -Pruntime.world=<save-directory> -Pruntime.username=<profile>
```

生产 JAR 收集到 `build/artifacts/<target>/`，文件名为 `<modid>-<mod-version>+<loader>-<minecraft-version>.jar`。运行实例位于 `run/<target>/<side>/`。

## 源码结构

- `src/`：跨 Target 的共享逻辑、资源与测试；
- `versions/<target>/src/`：加载器或版本专属实现；
- `gradle/` 与三个 `build.<loader>.gradle.kts`：统一 Target 和加载器约定；
- `stonecutter.properties.toml`：集中管理 `mod.*`、`deps.*`、`target.*`；
- `docs/`：架构、开发、测试和上游行为参考。

完整说明见 [项目布局](docs/architecture/project-layout.md)。

## 当前已知限制

- 规划集合包含五个 Target，目前只有 `fabric-26.2` 达到 implemented 状态；
- Fabric 26.2 客户端以 Vulkan 后端作为受支持和验收路径；
- GLASS 不强制加载任意远距离或跨维度区块，画面可用性受客户端已经获得的世界数据约束；
- GLASS 只提供观察投影，不提供传送、碰撞或递归投影；
- 多频道、高视距与大型无线表面会增加 CPU、GPU 和显存开销。

持续验证范围见 [验收矩阵](docs/testing/acceptance-matrix.md)。

## Credits

原始 GLASS 由 iChun 创作。本项目保留原始许可、版权信息、公开 mod ID、资源命名空间和必要兼容标识，并在独立维护中以原仓库作为只读行为参考。完整归属见 [CREDITS.md](CREDITS.md)。

## License

本项目依照仓库中的 [COPYING](COPYING) 与 [COPYING.LESSER](COPYING.LESSER) 分发。除非文件另有声明，源码遵循 GNU Lesser General Public License 3.0 或更高版本。
