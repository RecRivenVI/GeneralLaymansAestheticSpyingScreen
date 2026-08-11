# 开发入门

## 环境

1. 在 Windows 上安装 JDK 25；
2. 使用仓库自带的 Gradle Wrapper，不安装全局 Gradle；
3. 保持 Git 可执行文件可用，以便 Stonecutter 和格式门禁读取仓库信息；
4. Fabric 26.2 客户端需要支持 Vulkan 的显卡驱动。

Gradle 使用 UTF-8、`-Xmx5G`、无常驻 daemon、并行执行、构建缓存和 configuration cache。目标源码工具链由各 Loader 构建脚本声明。

## 常用流程

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat :fabric-26.2:test
.\gradlew.bat :fabric-26.2:check
.\gradlew.bat :fabric-26.2:build
```

完整验证：

```powershell
.\gradlew.bat spotlessCheck verifyTargets testAll checkAll buildAll collectArtifacts `
  --configuration-cache --configuration-cache-problems=fail `
  --no-daemon --stacktrace --console=plain
```

同一命令连续运行两次，第二次应复用 configuration cache。

规划集合是 `forge-1.20.1`、`neoforge-1.21.1`、`neoforge-26.1.2`、`fabric-26.2` 和 `neoforge-26.2`，当前实现集合只有 `fabric-26.2`。不要直接调用 planned 节点的编译、生成、归档或运行任务；这些入口会在 Loader 配置和依赖解析之前以 planned 状态错误拒绝。

## 运行实例

```powershell
.\gradlew.bat :fabric-26.2:runClient -Pruntime.world=<save-directory> -Pruntime.username=<profile>
```

Fabric 26.2 的 gameDir 是 `run/fabric-26.2/client`。`runtime.world` 是该实例 `saves/` 下的目录名。运行任务保留 Vulkan 后端和仓库定义的窗口参数。实例、世界、配置、日志、截图和 DebugBridge 数据全部被 Git 忽略。

不要直接调用 Stonecutter internal 任务，也不要增加仓库专属启动脚本。Target 切换、构建和运行都通过现有 Gradle 入口完成。
