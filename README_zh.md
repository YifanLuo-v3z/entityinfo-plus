# EntityInfo Plus

[English](README.md) | [简体中文](README_zh.md)

这是一个面向 [NekoyaHouse/Epsilon](https://github.com/NekoyaHouse/Epsilon) 的 Fabric 专用 addon，当前目标版本为 Minecraft `26.1.2`，上游 Epsilon `2026.6.3`。

## 功能概览

- `Armor HUD+`：可显示护甲、耐久、阴影、模糊和槽位背景的护甲 HUD
- `Bed Render`：带缓存与扫描延迟控制的床高亮渲染
- `Crystal Chams`：支持缩放、旋转、悬浮和范围控制的水晶填充渲染
- `Dropped Item HUD`：支持聚合、排序、过滤和刷新缓存的掉落物 HUD
- `Elytra Bounce`：带俯冲角、抬升角和调试锁视角选项的鞘翅弹跳辅助
- `Feet Trap Air Render`：用于围脚支撑空气位置的底面辅助渲染
- `Item HUD`：可自由增删追踪物品并显示数量的物品 HUD
- `Shulker Viewer`：支持潜影盒和末影箱预览、侧边列表、堆叠统计、排序和嵌套查看
- `XCarry`：保留 2x2 合成栏物品
- `Zoom`：平滑缩放与 FOV 钩子
- 以及对 `NameTags`、`KillAura`、`MaceAura`、`PacketEat`、`NoFall`、`NoSlow` 等 Epsilon 行为的兼容性修复

## 文档导航

- [Build Guide](BUILDING.md) | [构建说明](BUILDING_zh.md)
- [Project Structure](PROJECT_STRUCTURE.md) | [项目结构](PROJECT_STRUCTURE_zh.md)
- [Upstream Compatibility](UPSTREAM_COMPATIBILITY.md) | [上游兼容性说明](UPSTREAM_COMPATIBILITY_zh.md)
- [Build Artifacts](ARTIFACTS.md) | [构建产物记录](ARTIFACTS_zh.md)

## 兼容性

- Minecraft：`26.1.2`
- Fabric Loader：`0.19.2`
- Fabric API：`0.150.0+26.1.2`
- 上游 Epsilon：`2026.6.3`
- 需要放入本地 `libs` 的依赖：`epsilon-fabric-26.1.2-2026.6.3-7792aca.jar`

## 快速构建

1. 将 Epsilon Fabric 依赖放入 `libs/epsilon-fabric-26.1.2-2026.6.3-7792aca.jar`。
2. 如果文件名不同，请同步修改 `gradle.properties` 中的 `epsilon_jar_name`。
3. 运行 `.\gradlew.bat --no-daemon clean build`。
4. 如需游戏内验证，可运行 `.\gradlew.bat runClient`。

## 说明

- 该 addon 使用 Epsilon 自定义的 Fabric 入口 `epsilon:addon`。
- 本项目明确保持为 Fabric-only，不再默认维护 NeoForge 或多加载器拆分结构。
- 默认 `build` 任务只输出运行时 addon jar。
- 需要随仓库保留的正式构建产物会放在 `artifacts/` 的版本目录中。
