# EntityInfo Plus

Fabric-only 26.1.2 addon for Epsilon 2026.6.2 on Minecraft 26.1.2.

## Features

- `Armor HUD+`: HUD armor display with optional blur, shadows, slot background, and durability text
- `Crystal Chams`: custom filled end crystal rendering with scale, spin, float, and range controls
- `Dropped Item HUD`: aggregated dropped-item HUD list with sorting and distance filters
- `Item HUD`: tracked-item HUD that renders item icons with overlaid counts, keeps valid tracked IDs normalized, and lets you add or remove tracked items from the module settings using full IDs or vanilla shorthand IDs
- `Elytra Bounce`: elytra relaunch helper with control, safety, speed-cap, and debug lock options
- `Shulker Viewer`: shulker and ender chest preview with component/NBT container support, an Epsilon-style tooltip panel, screenshot-style edge sidebar cards, and middle-click nested container inspection
- `XCarry`: keeps the 2x2 crafting slots available by preventing the normal inventory close path
- `Zoom`: smooth zoom with optional require-key behavior and camera FOV hooks
- `NameTags` extension: adds dropped-item labels and related translation strings to Epsilon's built-in module
- Additional Epsilon fixes via mixins for `KillAura`, `HandsView`, `PacketEat`, and rotation smoothing

## Build

1. Put the Epsilon 2026.6.2 Fabric jar in `libs/epsilon-fabric-26.1.2.jar`.
2. If the filename differs, update `epsilon_jar_name` in `gradle.properties`.
3. Run `.\gradlew.bat --no-daemon clean build`.
4. Use `.\gradlew.bat runClient` for in-game verification.

## Notes

- The addon uses Epsilon's custom Fabric entrypoint `epsilon:addon`.
- This project now depends on Mixins in addition to Epsilon render events.
- The default `build` task only produces the runtime Fabric addon jar. Handoff packaging, sources jar generation, and root-level artifact staging are no longer maintained by default.
