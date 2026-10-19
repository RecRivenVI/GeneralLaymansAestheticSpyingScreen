# S1 验证

## 目标

保留 S1 独立场景，使用迁移后的 26.3 Fabric 验证 GLASS。历史配置和运行数据完整保留，历史 PASS 不代表当前 PASS。本场景不安装测试模组。

## 运行

从仓库根目录执行。启动前准备关闭全局声音，并检查 3 GiB 软限制、5 GiB 硬限制。

```powershell
.\gradlew.bat :runtime_validation:prepareS1
.\gradlew.bat :version:26.3-fabric:runValidationRegressionCleanSessionClient
```

多人场景分别启动各角色；服务端仍需使用者明确同意 EULA。不得以分别启动客户端判定 parity。

## 通过标准

进入世界后 GLASS 内容可用；无加载、网络或渲染异常。保存当前运行日志与人工视觉验收结果后才能判定通过。当前迁移中的结果为待验证。历史说明见 `result/migration_20261019/validation-20260909.md`。
