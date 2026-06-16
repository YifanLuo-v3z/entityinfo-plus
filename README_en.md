# EntityInfo Plus

[简体中文](README.md) | [English](README_en.md)

Fabric-only addon for [NekoyaHouse/Epsilon](https://github.com/NekoyaHouse/Epsilon), targeting Minecraft `26.1.2` and upstream Epsilon `2026.6.3`.

## Highlights

- `Armor HUD+`: armor HUD with blur, shadows, slot backgrounds, and durability text
- `Bed Render`: cached nearby bed rendering with fill, outline, blur, and scan controls
- `Crystal Chams`: filled crystal rendering with scale, spin, float, and range controls
- `Dropped Item HUD`: aggregated dropped-item HUD with sorting, filtering, and refresh caching
- `Elytra Bounce`: elytra relaunch helper with dive/lift pitch tuning and debug lock options
- `Feet Trap Air Render`: bottom-face helper render for Feet Trap support air
- `Item HUD`: configurable tracked-item HUD with add/remove item settings and count overlays
- `Shulker Viewer`: shulker and ender chest preview with sidebar, grouping, sorting, and nested inspection
- `XCarry`: keeps the 2x2 crafting slots available
- `Zoom`: smooth zoom with key requirement and camera FOV hooks
- `NameTags` extension and compatibility mixins for `KillAura`, `MaceAura`, `PacketEat`, `NoFall`, `NoSlow`, and related Epsilon behavior fixes

## Documentation

- [中文主页](README.md)
- [Build Guide](BUILDING.md) | [构建说明](BUILDING_zh.md)
- [Project Structure](PROJECT_STRUCTURE.md) | [项目结构](PROJECT_STRUCTURE_zh.md)
- [Upstream Compatibility](UPSTREAM_COMPATIBILITY.md) | [上游兼容性说明](UPSTREAM_COMPATIBILITY_zh.md)
- [Build Artifacts](ARTIFACTS.md) | [构建产物记录](ARTIFACTS_zh.md)

## Compatibility

- Minecraft: `26.1.2`
- Fabric Loader: `0.19.2`
- Fabric API: `0.150.0+26.1.2`
- Upstream Epsilon: `2026.6.3`
- Required local dependency jar: `libs/epsilon-fabric-26.1.2-2026.6.3-7792aca.jar`

## Quick Build

1. Put the Epsilon Fabric jar in `libs/epsilon-fabric-26.1.2-2026.6.3-7792aca.jar`.
2. If the filename differs, update `epsilon_jar_name` in `gradle.properties`.
3. Run `.\gradlew.bat --no-daemon clean build`.
4. Use `.\gradlew.bat runClient` for in-game verification when needed.

## Notes

- The addon uses Epsilon's custom Fabric entrypoint `epsilon:addon`.
- The project is intentionally Fabric-only. Do not reintroduce NeoForge or split-loader packaging unless explicitly required.
- The default `build` task only produces the runtime addon jar.
- Released jars tracked in Git are stored under versioned folders in `artifacts/`.
