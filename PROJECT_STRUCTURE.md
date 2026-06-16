# EntityInfo Plus Fabric Project Structure

## Root

- `build.gradle`: Fabric Loom build for the runtime Fabric addon jar
- `gradle.properties`: addon version, Minecraft/Fabric versions, required Epsilon version, and local Epsilon jar name
- `settings.gradle`
- `UPSTREAM_COMPATIBILITY.md`: Fabric-only upstream audit and retention notes
- `libs/epsilon-fabric-26.1.2.jar`: required local dependency

## Java Sources

- `src/main/java/com/github/entityinfo/EntityInfoAddon.java`: addon registration and module list
- `src/main/java/com/github/entityinfo/fabric/EntityInfoFabricEntrypoint.java`: Fabric entrypoint bridge
- `src/main/java/com/github/entityinfo/modules/`: addon modules
- `src/main/java/com/github/entityinfo/gui/ShulkerPreviewScreen.java`: preview screen for shulker and ender chest contents
- `src/main/java/com/github/entityinfo/mixin/`: mixins for Epsilon extensions and behavior fixes
- `src/main/java/com/github/entityinfo/utils/render/EntityRenderUtils.java`: dropped-item collection, formatting, caching, and frustum helpers

## Module Classes

- `ArmorHudPlus.java`
- `CrystalChams.java`
- `DroppedItemHUD.java`
- `ElytraBounce.java`
- `ItemHud.java`
- `ShulkerViewer.java`
- `XCarry.java`
- `Zoom.java`

## Mixins

- `MixinAbstractContainerScreen.java`
- `MixinCameraZoom.java`
- `MixinElytraFlightMode.java`
- `MixinEndCrystalRenderer.java`
- `MixinAutoMend.java`
- `MixinGUIMove.java`
- `MixinHandsView.java`
- `MixinKillAura.java`
- `MixinLocalPlayerXCarry.java`
- `MixinNameTags.java`
- `MixinNoFall.java`
- `MixinNoSlow.java`
- `MixinPacketEat.java`
- `MixinPacketMineCountdown.java`
- `MixinRotationUtils.java`

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
