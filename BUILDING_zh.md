# 构建与验证

[English](BUILDING.md) | [简体中文](BUILDING_zh.md)

## 1. 准备依赖

请将 Epsilon `2026.6.3` 的 Fabric jar 放到：

`libs\epsilon-fabric-26.1.2-2026.6.3-7792aca.jar`

如果文件名不同，请同步修改 [gradle.properties](gradle.properties) 中的 `epsilon_jar_name`。

## 2. 构建命令

Windows PowerShell：

```powershell
cd C:\Users\Administrator\Desktop\EntityInfo-Addon-Handoff\source\entityinfo-addon-1.5.4-project
.\gradlew.bat --no-daemon clean build
```

运行开发客户端：

```powershell
.\gradlew.bat runClient
```

## 3. 构建输出

主运行时 Fabric jar：

`build\libs\entityinfo-addon-1.6.106.jar`

默认 `build` 任务只产出运行时 Fabric addon jar，不再默认维护 sources jar、桌面交接包或其他额外打包目录。

## 4. 推荐验证重点

1. 确认 Gradle 能正确解析 Minecraft、Fabric、Fabric API 和本地 Epsilon 依赖 jar。
2. 启动 `runClient`，确认 Epsilon 通过 `epsilon:addon` 成功加载本 addon。
3. 检查模块列表中是否存在 `Armor HUD+`、`Bed Render`、`Crystal Chams`、`Dropped Item HUD`、`Feet Trap Air Render`、`Item HUD`、`Elytra Bounce`、`Shulker Viewer`、`XCarry`、`Zoom`。
4. 验证 `Shulker Viewer` 可正确预览潜影盒与末影箱，支持组件/NBT 容器内容、侧边列表、相同物品堆叠统计、排序和中键嵌套查看。
5. 验证 `Item HUD` 支持自由添加或删除追踪物品，渲染为“物品图标 + 数量”，并在启用 `Include Crafting Slots` 时统计 2x2 合成栏中的物品。
6. 验证 `Kill Aura` 的 `Only With Weapon` 开启时，只有手持武器才会转头和攻击，不会在手持非武器时干扰围脚等其他模块。
7. 验证 `Mace Aura` 重新可以正常攻击，`GUI Move` 在打开界面时仍能保持鞘翅飞行中的移动响应。
8. 验证金苹果和其他食物在原版鞘翅滑翔与 `Elytra Fly` 开启时都可正常食用，同时 `PacketEat` 仍保留原本针对总可食食物的保护逻辑。
9. 验证 `No Slow` 的 `Only Ground` 与 `No Fall` 的 `Auto Fall Distance` 行为正常。
10. 验证 `Bed Render`、`Crystal Chams`、`Feet Trap Air Render`、`Packet Mine` 倒计时渲染等渲染功能均正常工作，没有明显卡顿或错位。
11. 验证 `CreativeModeInventoryScreen` 中不会被 addon HUD 或 `Shulker Viewer` 破坏原版创造物品栏交互。
12. 验证中英文语言文件都能被正常解析，新增设置与模块名称在两种语言下都能正确显示。
13. 验证当前版本已适配上游 `Epsilon 2026.6.3`，移除 `MixinRotationUtils` 后不会重新出现旋转空状态、NaN 或登录/重生后的旋转异常。

## 5. Mixin 说明

本项目使用 Mixins 处理相机 FOV、Epsilon 模块扩展和若干兼容性修复。当前启用的 mixin 列表见 [entityinfo_addon.mixins.json](src/main/resources/entityinfo_addon.mixins.json)。
