# 项目布局

GLASS 使用 Stonecutter 单项目树。Target ID 采用 `<loader>-<minecraft-version>`，默认活动目标记录在 `versions/current`。

## Target

| Target | Minecraft | Loader | 状态 |
| --- | --- | --- | --- |
| `forge-1.20.1` | 1.20.1 | Forge | `planned` |
| `neoforge-1.21.1` | 1.21.1 | NeoForge | `planned` |
| `neoforge-26.1.2` | 26.1.2 | NeoForge | `planned` |
| `fabric-26.2` | 26.2 | Fabric | `implemented` |
| `neoforge-26.2` | 26.2 | NeoForge | `planned` |

状态、客户端能力和服务端能力来自 `stonecutter.properties.toml` 的 `target.*`。这是唯一状态源：settings 根据状态为 implemented 节点选择对应 Loader 构建脚本，为 planned 节点选择 `build.planned.gradle.kts`。Planned 节点参与注册、元数据与结构验证，但不应用 Loader 插件；其编译、源码生成、生命周期、归档与运行入口直接失败。

## 目录职责

```text
src/                         跨 Target 共享源码、资源与测试
versions/<target>/src/       Target 专属实现与资源
versions/current             默认活动 Target
gradle/                      Target 公共 Gradle 约定
build.<loader>.gradle.kts    Loader 构建和运行配置
build.planned.gradle.kts     Planned Target 轻量配置与快速失败门禁
stonecutter.gradle.kts       项目树、格式和仓库级聚合任务
stonecutter.properties.toml  mod、依赖和 Target 中央属性
run/<target>/<side>/         被忽略的隔离运行实例
build/artifacts/<target>/    被忽略的生产产物收集目录
docs/                        架构、开发、测试和参考文档
```

共享代码不得依赖加载器 API。少量映射差异使用最小范围的 Stonecutter 条件；完整加载器入口、注册、网络、Mixin 和版本渲染实现放在对应 Target。

## 任务和产物

每个 Target 都暴露一致的标准任务名，但只有 implemented Target 可以执行产生实现结果的任务。仓库级公共验证入口为：

- `spotlessCheck`；
- `verifyTargets`；
- `testAll`；
- `checkAll`；
- `buildAll`；
- `collectArtifacts`。

`testAll`、`checkAll`、`buildAll` 和 `collectArtifacts` 只选择 status 为 `implemented` 的 Target；当前只能选择 `fabric-26.2`。`verifyTargets` 检查全部五个 Target。产物名固定为 `<modid>-<mod-version>+<loader>-<minecraft-version>.jar`。
