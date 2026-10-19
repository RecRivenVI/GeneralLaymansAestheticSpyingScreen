# 参考端基本投影验证

## 目标

在独立的新世界证明 26.3 NeoForge 能启动、进入世界、经原生网络往返配置频道，并产生有线和无线投影帧。探针只在本验证加载，旧世界不参与。

## 运行

```powershell
.\gradlew.bat :runtime_validation:prepareReference
.\gradlew.bat :version:26.3-neoforge:runValidationSmokeReferenceProjectionClient
.\gradlew.bat :runtime_validation:reviewReference
```

## 通过标准

运行无注入错误和频道渲染异常；runtime.json 记录频道网络、表面和三个无线节点、Vulkan、1920×1080、全局声音为零。四张截图均存在，频道帧含场景颜色，主画面的对应投影表面可见该场景。工具只判定结构和像素条件，人工仍需检查投影方向、几何、动画和窗口视差。结果不得替代用户视觉验收。
