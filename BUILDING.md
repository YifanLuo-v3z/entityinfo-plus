# Build And Verify

[English](BUILDING.md) | [简体中文](BUILDING_zh.md)

## 1. Prepare Dependency

Put the Epsilon 2026.6.3 Fabric jar at:

`libs\epsilon-fabric-26.1.2-2026.6.3-7792aca.jar`

If the filename differs, update `epsilon_jar_name` in [gradle.properties](C:\Users\Administrator\Desktop\EntityInfo-Addon-Handoff\source\entityinfo-addon-1.5.4-project\gradle.properties).

## 2. Build Commands

Windows PowerShell:

```powershell
cd C:\Users\Administrator\Desktop\EntityInfo-Addon-Handoff\source\entityinfo-addon-1.5.4-project
.\gradlew.bat --no-daemon clean build
```

Run a dev client:

```powershell
.\gradlew.bat runClient
```

## 3. Build Output

Main Fabric jar:

`build\libs\entityinfo-addon-1.6.104.jar`

The default `build` task only produces the runtime Fabric addon jar. Sources
jar generation, root-level `artifacts`, and desktop handoff staging folders are
no longer maintained by default.

## 4. Verification Steps

1. Confirm Gradle resolves Minecraft, Fabric, Fabric API, and the local Epsilon jar without errors.
2. Start `runClient` and verify Epsilon loads the addon through the `epsilon:addon` entrypoint.
3. In the module list, verify `Armor HUD+`, `Bed Render`, `Crystal Chams`, `Dropped Item HUD`, `Feet Trap Air Render`, `Item HUD`, `Elytra Bounce`, `Shulker Viewer`, `XCarry`, and `Zoom` appear.
4. Verify `NameTags` shows the added dropped-item-related options and renders dropped item labels correctly.
5. Verify `Zoom` changes FOV immediately when enabled, respects `Require Key`, and also works when the zoom bind is set to a mouse button.
6. Verify `Shulker Viewer` previews both shulker boxes and ender chests, reads both component-backed and NBT-backed contents, skips empty containers instead of opening blank previews, exposes independent preview options in the module including `Preview Background`, `Preview Slot Frames`, `Draw Empty Slots`, `Container Sidebar`, `Sidebar Background`, `Sidebar Slot Frames`, `Sidebar Sort`, `Compact Uniform Preview`, `Sidebar X`, and `Sidebar Y`, renders edge-pinned sidebar cards with a full-width accent title bar and dark body, hides duplicate sidebar cards for the same container item+contents, merges repeated same-item same-NBT entries into counted slots, can sort merged sidebar items by original order, name, or count, keeps per-slot items at full size for mixed contents, shrinks merged sidebar cards to the minimum needed 1-3 visible rows, left-aligns compact stacked entries, and when `Compact Uniform Preview` is enabled collapses any same-item same-NBT sidebar contents into a single rendered 1x1 slot without drawing the unused empty slots.
7. Verify `Kill Aura` with `Only With Weapon` enabled does not rotate or attack unless the player is holding a weapon, no longer interferes with surround placement when holding non-weapons, and does not keep stale attack buildup or switch-target state after the player spends time holding a non-weapon.
8. Verify `XCarry` preserves the 2x2 crafting grid without breaking normal non-inventory container flows.
9. Verify `Crystal Chams` renders filled crystals correctly and that disabling `Filled` falls back to vanilla crystal rendering instead of hiding the crystal.
10. Verify the creative inventory item tabs render normally with addon HUDs enabled; `Shulker Viewer` should not open sidebars/tooltips inside `CreativeModeInventoryScreen`, and addon HUDs should stay hidden in normal screens unless `Render In Screens` is enabled.
11. Verify the `Shulker Viewer` middle-click nested hint only appears when the shown preview actually contains at least one nested container that can be previewed.
12. Verify `Item HUD` counts matching items stored in the 2x2 crafting grid as well when `Include Crafting Slots` is enabled, so XCarry-preserved items are reflected in the HUD total.
13. Verify `NameTags` dropped-item overlays keep merge-only child settings hidden when `Show Dropped Items` is off, and merged dropped-item labels stay centered over the whole nearby item cluster instead of drifting toward only one representative stack.
14. Verify `Elytra Bounce` no longer leaves the player stuck in a forced sprint state after losing elytra control, unequipping the elytra, or disabling the module.
15. Verify `Item HUD` shows the `Include Crafting Slots` source toggle localized in both English and Chinese.
16. Verify `Elytra Bounce` no longer forces jump input while the module is enabled but the player is not wearing a glideable elytra.
17. Parse all bundled `en_us.json` and `zh_cn.json` language files as JSON to verify they are syntactically valid after edits.
18. Verify `Elytra Bounce` releases its control state while the player is in water, underwater, or lava instead of continuing to hold movement inputs there.
19. Verify golden apples can be eaten during vanilla elytra gliding and while Epsilon `Elytra Fly` is enabled; food right-click should start normally, `PacketEat` should still cancel always-edible food `RELEASE_USE_ITEM` packets, and automatic Elytra Fly fireworks/unbreaking swaps should pause while food is being used.
20. Verify `Elytra Bounce` does not keep a redundant liquid-only `START_FALL_FLYING` guard that disagrees with its control-boundary check.
21. Verify `Packet Mine` shows the new `Mine Countdown` render overlay while mining, updates the countdown/percent for both primary and double-break targets, respects `Countdown Mode`, `Countdown Scale`, `Countdown Y Offset`, `Countdown Background`, and color settings, and hides the overlay once the target block is air or replaceable.
22. Verify `Bed Render` highlights nearby beds as one combined bed-shaped box, does not duplicate head/foot halves, respects `Range`, `Vertical Range`, fill/outline/blur settings, and refreshes cached beds according to `Scan Delay`.
23. Verify `No Fall` in `GrimSimulation` mode exposes `Auto Fall Distance`; when enabled it keeps the saved `Fall Distance` slider unchanged while automatically choosing a conservative threshold from the predicted fall height.
24. Verify `Feet Trap Air Render` is an independent addon module, no longer adds settings into upstream `Feet Trap`, renders only the bottom face of planned Feet Trap block positions when `Only Below Air` is enabled and the block below is replaceable air, and uses `Refresh Delay` to cache target calculations instead of recomputing placement candidates every frame.
25. Verify `Bed Render` still refreshes nearby beds on `Scan Delay`, while moving around no longer forces every cached bed to perform a block-state validation every rendered frame.
26. Verify `Item HUD` still updates counts correctly after changing inventory contents, while tracked item IDs are cached until the configured list changes and enabled inventory sources are counted in one pass.
27. Verify `Dropped Item HUD` respects `Refresh Delay`, keeps text responsive during movement, and no longer forces a full dropped-item aggregation every rendered frame.
28. Inspect the generated jar and verify empty resource directories such as `assets/addon_example` and `assets/entityinfo_addon/textures` are not packaged.
29. Verify `Feet Trap Air Render` no longer calls Epsilon side-specific render helpers at runtime; it should render the planned block bottom face as a very thin box using the stable filled/outline box helpers.
30. Verify `Feet Trap Air Render` keeps its original behavior when `Require Feet Trap Blocks` is disabled, and when enabled only renders after the detected surrounding feet-trap block count reaches `Min Feet Trap Blocks`.
31. Verify the Chinese module list shows the module as `围脚悬空`, so it does not duplicate Epsilon's own `围脚` module name, and with `Require Feet Trap Blocks` enabled it only marks existing surrounding feet-trap blocks whose block below is air.
32. Verify `Feet Trap Air Render` `Only Ground` defaults off, and when enabled clears its cached markers and renders nothing while the player is airborne.
33. Verify `Feet Trap Air Render` does not render while the player is supported by slabs or other non-full collision blocks, and does not count non-full blocks as valid existing feet-trap blocks.
34. Verify `Mace Aura` attacks again after enabling it: with a valid target in range and a mace available, the module should rotate, perform its configured mace VClip sequence, and send the attack instead of staying idle.
35. Verify `GUI Move` keeps elytra flight responsive while an inventory screen is open, including custom movement keybinds.
36. Verify `No Slow` exposes `Only Ground`; when enabled, slowdown bypass and GrimC0F state are skipped/reset while the player is airborne.
37. Verify while `Elytra Fly` is active, opening Epsilon module UI screens still allows right-click expansion and other UI right-click interactions instead of having them cancelled by the flight module.
38. Verify `Elytra Bounce` now exposes separate `Dive Pitch` and `Lift Pitch` settings, uses `Dive Pitch` while descending, and switches to `Lift Pitch` while the player is rising during fall flying.
39. Verify the addon loads correctly against upstream `Epsilon 2026.6.3`, and that removing `MixinRotationUtils` does not reintroduce rotation-related NaN or null-state crashes during login, respawn, or combat module rotation smoothing.

## 5. Mixin Note

This project uses Mixins for camera FOV hooks, Epsilon module extensions, and gameplay fixes. See [entityinfo_addon.mixins.json](C:\Users\Administrator\Desktop\EntityInfo-Addon-Handoff\source\entityinfo-addon-1.5.4-project\src\main\resources\entityinfo_addon.mixins.json) for the active list.
