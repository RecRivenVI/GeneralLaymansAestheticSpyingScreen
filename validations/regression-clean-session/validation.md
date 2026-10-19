# 干净客户端回归

确认不安装 CSL/Mod Menu 的原 PRIMARY S1 实例可作为干净基线；功能及视觉结果由实际客户端验收确定。

## 输入和边界

- Target：`26.2-fabric`，唯一 implemented 版本。
- 配置位于本目录 `task/profiles/26.2-fabric`，为角色对应的 JSON；运行数据位于 `instance/26.2-fabric`，默认 ignored。
- PRIMARY 为 RecRivenVI，SECONDARY 为 CNN_MOYAN；客户端 2560x1440、Vulkan。S1/server 不安装测试模组，其他客户端需要 JSON 中固定哈希的 CSL 与 Mod Menu。
- 共用 staging/verifier 位于仓库 `eng/runtime`，不复制工具、不安装 GLASS 产品 JAR。以下所有命令以仓库根为起点。

## 无游戏的前提检查

```powershell
powershell -NoProfile -File eng/runtime/Verify-RepositoryProfiles.ps1
.\gradlew.bat :version:26.2-fabric:runClient '-Pruntime.profile=S1' --dry-run
```

如需要为已有场景准备测试模组，可明确执行 `powershell -NoProfile -File eng/runtime/Stage-RepositoryProfile.ps1 -Role PRIMARY -Profile S1`。该命令可能下载固定版本并写实例 mods；它不启动游戏，不能代替验收。M1/M2 的 SECONDARY 需单独指定角色，M2 的 SERVER 亦需单独指定。

## 实际验收

在获得游戏运行授权后，去掉对应 run 任务的 `--dry-run` 执行；M1/M2 的第二客户端使用 `:version:26.2-fabric:runClientMultiplayer`，M2 server 使用 `:version:26.2-fabric:runServer`，均传相同 Profile。按场景目的检查 GLASS 的频道选择、投影显示及连接状态；记录具体步骤、环境、观察结果和失败项。

只将可复现且值得长期保留的结果放到 `result`，历史证据保持原文、原哈希与时间语境。测试前提通过、构建通过、不同客户端分别启动，都不构成上述行为验收。

## 当前验收状态

本次仅执行目录迁移和不启动游戏的自动检查。真实运行、渲染/视觉与交互验收未执行，不创建虚构 PASS 或空 result 目录。
