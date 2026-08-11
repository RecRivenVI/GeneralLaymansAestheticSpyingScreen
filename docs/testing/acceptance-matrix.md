# 验收矩阵

## 自动门禁

| 范围 | 入口 | 通过条件 |
| --- | --- | --- |
| 格式 | `spotlessCheck` | 所有受管源码与文本符合统一格式 |
| Target 契约 | `verifyTargets` | Loader-first ID、状态、运行侧和活动目标有效 |
| 单元测试 | `testAll` | 所有 implemented Target 测试通过，无跳过 |
| 检查 | `checkAll` | Target 检查及 Spotless 通过 |
| 构建 | `buildAll` | 所有 implemented Target 生产构建成功 |
| 产物 | `collectArtifacts` | 只收集 implemented Target 的命名规范 JAR |
| Configuration cache | 完整命令连续两次 | 第二次显示 reused，问题等级为 fail |
| Planned 门禁 | 单独调用 planned Target 的生命周期、运行、编译、生成与归档入口 | Settings 任务选择阶段立即报告 Target ID 和 `planned`，不执行节点任务，且无 Loader 配置、依赖解析或 build 树变化 |

## Fabric 26.2 运行验收

| 范围 | 证据 |
| --- | --- |
| 启动 | `:fabric-26.2:runClient` 使用 `run/fabric-26.2/client` 和 Vulkan |
| 世界 | 玩家、维度、位置、世界状态与兼容存档可读 |
| 画面 | DebugBridge framebuffer 显示稳定世界而非黑屏、标题或崩溃界面 |
| 内容 | 四类 GLASS 方块、频道 GUI、配置 GUI、无线排序和红石交互可用 |
| 投影 | 六向视差、远端区块、实体、粒子、天气、透明合成和出口裁切正确 |
| 传播 | 普通表面、拐角、拓扑热更新、无线顺序、启停及热切换动画正确 |
| 生命周期 | 进入世界、区块加载、保存退出和重新进入不会丢失活动投影 |
| 日志 | `latest.log` 无致命异常，正常退出后 Gradle 任务结束 |

## 当前支持边界

规划集合恰好包含 `forge-1.20.1`、`neoforge-1.21.1`、`neoforge-26.1.2`、`fabric-26.2` 和 `neoforge-26.2`。当前实现集合只有 `fabric-26.2`；其他 Target 保持 planned，直到源码、资源、自动测试和真实运行证据全部齐备。静态源码、兼容资料或空元数据不能提升状态。
