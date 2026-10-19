# 模板结构迁移验证

## 目标

核对迁移前文件哈希、用户存档完整性、五个 Target 和不可修改的模板文件。

## 运行

```powershell
python components/runtime_validation/runtime_validation.py verify-migration
.\gradlew.bat :runtime_validation:check
.\gradlew.bat verifyCompliance
```

## 通过标准

迁移清单中的用户数据逐文件哈希一致；模板文件哈希一致；严格合规检查无失败。历史运行日志位于 `result/legacy_runtime/`，不代表本轮运行结果。
