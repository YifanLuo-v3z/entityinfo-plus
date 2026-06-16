# EntityInfo Plus 项目结构

[English](PROJECT_STRUCTURE.md) | [简体中文](PROJECT_STRUCTURE_zh.md)

## 根目录

- `build.gradle`：运行时 Fabric addon jar 的 Fabric Loom 构建脚本
- `gradle.properties`：addon 版本、Minecraft/Fabric 版本、所需 Epsilon 版本和本地依赖 jar 名称
- `settings.gradle`
- `README.md` / `README_zh.md`：仓库主页
- `BUILDING.md` / `BUILDING_zh.md`：构建与验证说明
- `ARTIFACTS.md` / `ARTIFACTS_zh.md`：版本产物归档记录
- `UPSTREAM_COMPATIBILITY.md` / `UPSTREAM_COMPATIBILITY_zh.md`：上游兼容性审计说明
- `libs/epsilon-fabric-26.1.2-2026.6.3-7792aca.jar`：必需的本地 Epsilon 依赖

## Java 源码

- `src/main/java/com/github/entityinfo/EntityInfoAddon.java`：addon 注册与模块列表
- `src/main/java/com/github/entityinfo/fabric/EntityInfoFabricEntrypoint.java`：Fabric 入口桥接
- `src/main/java/com/github/entityinfo/modules/`：addon 模块目录
- `src/main/java/com/github/entityinfo/gui/ShulkerPreviewScreen.java`：独立容器预览界面
- `src/main/java/com/github/entityinfo/mixin/`：Epsilon 扩展与兼容性修复 mixin
- `src/main/java/com/github/entityinfo/utils/render/EntityRenderUtils.java`：掉落物收集、格式化、缓存和视锥辅助

## 模块类

- `ArmorHudPlus.java`
- `BedRender.java`
- `CrystalChams.java`
- `DroppedItemHUD.java`
- `ElytraBounce.java`
- `FeetTrapAirRender.java`
- `ItemHud.java`
- `ShulkerViewer.java`
- `XCarry.java`
- `Zoom.java`

## Mixins

- `MixinAbstractContainerScreen.java`
- `MixinAutoMend.java`
- `MixinCameraZoom.java`
- `MixinElytraFlightMode.java`
- `MixinEndCrystalRenderer.java`
- `MixinGUIMove.java`
- `MixinHandsView.java`
- `MixinKillAura.java`
- `MixinLocalPlayerXCarry.java`
- `MixinMaceAura.java`
- `MixinNameTags.java`
- `MixinNoFall.java`
- `MixinNoSlow.java`
- `MixinPacketEat.java`
- `MixinPacketMineCountdown.java`

## 资源文件

- `src/main/resources/fabric.mod.json`
- `src/main/resources/entityinfo_addon.mixins.json`
- `src/main/resources/assets/entityinfo_addon/lang/en_us.json`
- `src/main/resources/assets/entityinfo_addon/lang/zh_cn.json`
- `src/main/resources/assets/epsilon/lang/en_us.json`
- `src/main/resources/assets/epsilon/lang/zh_cn.json`

## Wrapper

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
