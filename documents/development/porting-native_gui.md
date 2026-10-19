# 原版 GUI 控件与主题

频道、投影仪和无线节点列表采用 `ObjectSelectionList` 的真实 Entry；选择、滚动、焦点与条目朗读进入原版机制。GLASS 仅绘制面板、行、徽标、选中强调与滚动条。按钮仍继承原版 Button，终端可见性采用 CycleButton，底部操作采用原版布局容器。无线空间预览保持自绘。

## 版本差异

| Target | 原版接口 |
| --- | --- |
| 26.3 NeoForge 与 Fabric | `GuiGraphicsExtractor`、Entry 的 `extractContent`、`KeyEvent` 与 `MouseButtonEvent`；LinearLayout 排列按钮 |
| 26.1.2 NeoForge | 相同的列表结构和绘制回调，使用该版的 Screen 切换入口与 GLFW 键值 |
| 1.21.1 NeoForge | Entry 的 `render` 与旧鼠标、键盘回调；LinearLayout 排列按钮 |
| 1.20.1 Forge | 列表构造传入上下边界，使用 `setLeftPos`；GridLayout 排列按钮，Entry 使用旧回调 |

各 Target 保留自身网络入口，不共享 Minecraft 源码。完整的名称、坐标和状态徽标组成条目朗读文本；视觉裁剪不截断该文本。空列表提示与列表标题分开，条目存在时不会把空提示作为列表语义标签。移动或恢复选择后滚动到相应条目。

## 操作与验证

Tab 使用原版焦点导航。页面不再全局截获列表方向键或 Enter；当前条目处理确认，按钮保留自身键盘激活。Delete 只在无线节点列表持有焦点时删除选中项。终端文本框保留 Enter 保存，可见性通过 CycleButton 调整，主题覆盖仅绘制其外观。

[原版 GUI 控件验收](../../validations/conformance-native_gui/validation.md)使用固定 MCP JAR，通过实时 UI 树中的条目名称、坐标与状态操作，不预先计算行号或使用 DEBUG 写入。读取受 MCP USER 可见区域约束，长列表通过原版导航滚动。CycleButton 的状态由其当前标签读取；滑块的 `value` 是归一化位置，实际数值同时由标签核对。

参考端的游戏验收与其他 Target 的构建验证分别记录，不把它们扩张为五端视觉一致性。用户体验实例继承混合展示世界，自动测试与用户体验使用独立副本。
