# G.L.A.S.S

G.L.A.S.S. 是一个实时远端画面投影模组：终端选择画面源，投影器与有线、无线扩展构成显示网络。项目处于 `8.0.0-alpha.1` 开发阶段。

## Target

| Target | 加载器版本 | Java |
| --- | --- | --- |
| `1.20.1-forge` | 47.4.28 | 17 |
| `1.21.1-neoforge` | 21.1.264 | 21 |
| `26.1.2-neoforge` | 26.1.2.122 | 25 |
| `26.3-neoforge` | 26.3.0.94 | 25 |
| `26.3-fabric` | 0.20.0 | 25 |

五个 Target 为本轮迁移目标；当前构建与运行状态见[迁移说明](documents/development/porting-template_migration.md)，不能把登记视作已完成支持。参考端为 26.3 NeoForge。

## 快速开始

玩家请从[安装与操作说明](documents/usage/guide-first_projection.md)开始。开发者使用 JDK 25 启动 Wrapper：

```powershell
.\gradlew.bat spotlessApply
.\gradlew.bat check verifyRelease
```

## 文档

- [项目规范](documents/AGENTS.md)
- [构建与 API 迁移](documents/development/porting-template_migration.md)
- [运行验证工具](documents/design/architecture-runtime_validation.md)
- [依赖说明](documents/reference/dependency-runtime.md)
- [移植状态审计](documents/research/audit-port_status.md)
- [更新日志](documents/release/changelog-glass.md)

## 许可

产品使用 LGPL-3.0-or-later，见 [COPYING](COPYING) 与 [COPYING.LESSER](COPYING.LESSER)。构建基础设施与第三方归属见 [NOTICE](NOTICE)。
