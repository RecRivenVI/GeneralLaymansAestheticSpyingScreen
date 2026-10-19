# 投影与动画数学组件

`projection_math` 保存不依赖 Minecraft 或加载器的频道名称规则、投影矩阵、动画时间线、可见性策略、预览相机值和拓扑更新队列。组件使用独立的 `io.github.recrivenvi.glass.model` 包，避免 FML 模块的 split package；各 Target 通过显式公共接口调用，组件类随产品 JAR 合并发布。

使用 Java 17 字节码；JOML 仅作编译依赖，运行时由 Minecraft 提供，不内嵌重复库。测试使用 JUnit 6，并由组件的 `check` 执行。MC 与加载器相关类型留在各 Target。
