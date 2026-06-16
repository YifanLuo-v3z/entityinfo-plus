# EntityInfo Plus Project Structure

[English](PROJECT_STRUCTURE.md) | [简体中文](PROJECT_STRUCTURE_zh.md)

## Root

- `build.gradle`: Fabric Loom build for the runtime addon jar
- `gradle.properties`: addon version, Minecraft/Fabric versions, required Epsilon version, and local Epsilon jar name
- `settings.gradle`
- `README.md` / `README_zh.md`: repository landing pages
- `BUILDING.md` / `BUILDING_zh.md`: build and verification notes
- `ARTIFACTS.md` / `ARTIFACTS_zh.md`: versioned jar archive log
- `UPSTREAM_COMPATIBILITY.md` / `UPSTREAM_COMPATIBILITY_zh.md`: upstream audit notes
- `libs/epsilon-fabric-26.1.2-2026.6.3-7792aca.jar`: required local Epsilon dependency

## Java Sources

- `src/main/java/com/github/entityinfo/EntityInfoAddon.java`: addon registration and module list
- `src/main/java/com/github/entityinfo/fabric/EntityInfoFabricEntrypoint.java`: Fabric entrypoint bridge
- `src/main/java/com/github/entityinfo/modules/`: addon modules
- `src/main/java/com/github/entityinfo/gui/ShulkerPreviewScreen.java`: standalone container preview screen
- `src/main/java/com/github/entityinfo/mixin/`: Epsilon extension and compatibility mixins
- `src/main/java/com/github/entityinfo/utils/render/EntityRenderUtils.java`: dropped-item collection, formatting, caching, and frustum helpers

## Module Classes

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

## Resources

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
