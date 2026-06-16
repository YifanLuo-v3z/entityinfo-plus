# Upstream Compatibility Notes

[English](UPSTREAM_COMPATIBILITY.md) | [简体中文](UPSTREAM_COMPATIBILITY_zh.md)

This project is intentionally Fabric-only. Do not reintroduce NeoForge,
Architectury, split-loader source sets, or desktop handoff packaging unless the
maintainer explicitly asks for them again.

## Baseline

- Epsilon branch: `26.1.x`
- Checked upstream commit: `7792aca`
- Required Epsilon version: `2026.6.3`
- Minecraft version: `26.1.2`
- Fabric Loader: `0.19.2`
- Fabric API: `0.150.0+26.1.2`

The local dependency is expected at:

`libs/epsilon-fabric-26.1.2-2026.6.3-7792aca.jar`

## Retained Addon Modules

The current addon modules were kept because the checked Epsilon baseline does
not provide exact replacements for their behavior:

- `Armor HUD+`: more configurable armor HUD than upstream inventory HUD.
- `Crystal Chams`: custom filled crystal rendering controls.
- `Dropped Item HUD`: aggregated dropped item HUD.
- `Elytra Bounce`: elytra relaunch helper distinct from upstream elytra modules.
- `Feet Trap Air Render`: render-only planned Feet Trap bottom-face markers for
  unsupported air-below placements.
- `Item HUD`: tracked item icon/count HUD with add/remove settings and normalized valid item IDs.
- `Shulker Viewer`: shulker and ender chest preview with container list rendering.
- `XCarry`: preserves 2x2 crafting slots.
- `Zoom`: smooth zoom and camera FOV hooks.

The obsolete `EntityInfo` and `DroppedItemName` modules remain removed.

## Retained Mixins

These mixins are still required for addon-specific behavior or bug fixes:

- `MixinAutoMend`: adds disable-on-full-durability behavior without shadowing
  upstream private setting fields.
- `MixinHandsView`: suppresses spear blocking animation conflicts without
  shadowing upstream private setting fields.
- `MixinKillAura`: keeps only-with-weapon rotation/attack gating while avoiding
  shadows for upstream private setting fields.
- `MixinNameTags`: extends upstream name tags with dropped item labels. The
  dropped-item renderer no longer reflects Epsilon's private `TagDrawData`
  record, renderer suppliers, name color, private perspective-scale helper, or
  private setting fields; it reads shared settings through the public settings
  list with safe defaults.
- `MixinPacketEat`: replaces Epsilon's stale cached item lookup for use-release
  packets, preventing invalid food-use state from breaking vanilla use without
  shadowing upstream private fields.
- `MixinNoFall`: adds an automatic Grim fall-distance threshold while keeping
  the upstream manual `Fall Distance` setting intact.
- `MixinNoSlow`: clears stale GrimC0F packet state during login/common ping
  handling so a previous world session cannot cancel the login pong or send a
  movement packet before the client connection is available.
- `MixinAbstractContainerScreen`: drives container hover/sidebar previews.
- `MixinCameraZoom`: applies zoom FOV changes.
- `MixinEndCrystalRenderer`: supports crystal chams rendering. It cancels the
  crystal `submit` method only when the addon replaces the vanilla crystal body,
  which avoids binding to Minecraft's internal `SubmitNodeCollector.submitModel`
  descriptor while preserving beam and parent renderer submission.
- `MixinLocalPlayerXCarry`: preserves xcarry slots.

## 1.6.50 Fabric-Only Hardening

- Dropped item HUD aggregation now uses an `ItemStack` equality key instead of
  a raw hash integer, so rare item/component hash collisions cannot split or
  overwrite counted entries.
- Shulker sidebar merge keys now keep `equals` and `hashCode` aligned for
  same-item same-NBT stacks with different counts, making counted sidebar
  slots stable.
- Crystal Chams now collects and renders crystals by camera range, matching the
  range used when suppressing the vanilla crystal body and avoiding third-person
  or detached-camera invisibility edges.
- Item HUD bounds now include the bottom-right count label so the Fabric HUD
  editor/drag area matches the rendered item-plus-number layout.

## 1.6.51 Compatibility Audit

- Rechecked `origin/26.1.x` at `fbd2c0a`; no new upstream commits were present
  after refresh.
- Confirmed retained addon modules are still not exact duplicates of upstream
  modules. Upstream `InventoryHUD`, `Chams`, `ElytraFly`, `ElytraSwap`, and
  `CameraClip` remain adjacent features rather than replacements.
- Dropped-item `NameTags` extension now uses an `ItemStack` equality key for
  merged label entries, matching the dropped item HUD hardening and preventing
  rare component hash collisions from splitting counted labels.

## 1.6.52 Fabric-Only Polish

- Kept the project output Fabric-only: no NeoForge, Architectury, split-loader
  source sets, or desktop handoff package were reintroduced.
- Repaired Chinese language resource JSON files as UTF-8 and validated the
  English/Chinese key sets for both `epsilon` and `entityinfo_addon`.
- `MixinPacketEat` still replaces Epsilon's stale cached food stack lookup, but
  now lets vanilla release-use packets pass while the player is fall flying so
  normal elytra-flight eating is not blocked.

## 1.6.53 Detail Hardening

- Rechecked `origin/26.1.x` at `fbd2c0a`; no upstream module replacements were
  added for the retained addon modules.
- `Shulker Viewer` now treats ender chests as ender-chest previews only, so the
  `Allow Ender Chest` switch fully controls them and the container sidebar
  never lists nested ender chests.
- `Crystal Chams` restores the pose stack after submitting crystal beams, which
  avoids leaking the beam translation into the parent renderer submission.
- `Item HUD` grid bounds now use each entry's real row origin, keeping the HUD
  editor/drag area aligned with rendered item-plus-number entries.

## 1.6.54 Zoom Cleanup

- `Camera#getFov` was removed from the zoom injection path to avoid double
  applying zoom on render paths that already read the camera's cached FOV.
- The addon still updates the primary `calculateFov` and `calculateHudFov`
  hooks, which is enough to keep the world and HUD projection FOVs aligned.

## 1.6.55 Creative Inventory Guard

- `Shulker Viewer` now skips its container-screen mouse, scroll, sidebar, and
  tooltip hooks inside `CreativeModeInventoryScreen`, keeping the creative item
  tabs on the vanilla rendering/interaction path.
- `Armor HUD+`, `Dropped Item HUD`, and `Item HUD` now expose `Render In
  Screens`, defaulting off, so addon HUD rendering no longer overlays normal
  inventory or creative screens unless explicitly enabled. HUD editor rendering
  remains available.

## 1.6.56 Screen Boundary Tightening

- `Armor HUD+`, `Dropped Item HUD`, and `Item HUD` now always skip rendering in
  `CreativeModeInventoryScreen`, even if `Render In Screens` is later enabled,
  preventing creative tab overlays from reappearing.
- Chinese translations were added for the new `Render In Screens` setting so
  the menu stays aligned across locales.

## 1.6.57 Localization Polish

- `ShulkerPreviewScreen` no longer hardcodes the nested-container middle-click
  hint in English; it now reads from the addon language files.
- Entity item-slot labels now localize the slot prefix and empty-slot text,
  so dropped-item and equipment text overlays stay consistent in Chinese.

## 1.6.58 Addon UI And Sidebar Hover Polish

- The addon entry in `Client Settings -> Addons` now resolves its display name
  and description from the existing addon language keys at runtime, so the
  Addons panel no longer stays hardcoded in English when the rest of the addon
  is localized.
- `Shulker Viewer` now consumes empty sidebar hover space too, preventing
  underlying container tooltips from bleeding through when the cursor is inside
  the sidebar body but not over a non-empty preview slot.

## 1.6.59 Sidebar Interaction And Item HUD Polish

- `Shulker Viewer` sidebar cards now distinguish between empty slot hover and
  whole-card hover, so empty grid slots no longer reopen previews or leak
  clicks through to the underlying container, while hovering or clicking the
  card header/body can once again act on the container itself as originally
  intended by the sidebar fallback logic.
- `Item HUD` `Use Held Item` now falls back to the offhand item when the main
  hand is empty, avoiding the previous no-op behavior when tracking an item
  held only in the offhand.

## 1.6.60 Nested Preview Hint Consistency

- `Shulker Viewer` now only shows the nested-container middle-click hint when
  the currently rendered preview actually contains at least one nested
  previewable container, so the header hint once again matches what middle
  click can really do in tooltip, sidebar, and standalone preview contexts.

## 1.6.61 Item HUD XCarry Count Fix

- `Item HUD` now optionally counts matching items stored in the player's 2x2
  crafting grid too, so XCarry-preserved stacks no longer disappear from the
  HUD total just because they are being held outside the main inventory rows.

## 1.6.62 Kill Aura Weapon-Gate State Reset

- `Kill Aura` now clears its pending attack accumulator and switch-target index
  too while `Only With Weapon` is blocking the module, so re-equipping a weapon
  no longer resumes with stale click buildup or an old switch cycle.

## 1.6.63 NameTags Dropped-Item Cluster Polish

- `NameTags` dropped-item merge-only child settings now stay hidden whenever
  `Show Dropped Items` itself is off, so the injected Epsilon settings no
  longer leak half-relevant controls into the panel.
- Merged dropped-item labels now use the whole cluster's interpolated bounds
  and average position instead of anchoring to only the first representative
  entity, which keeps multi-stack labels centered over the actual item pile.

## 1.6.64 Elytra Bounce Sprint Cleanup

- `Elytra Bounce` now releases the sprint state it forced once elytra control
  stops, the chest item is no longer glideable, or the module disables, so the
  player no longer stays sprinting just because the module had previously
  toggled sprint during flight control.

## 1.6.65 Item HUD Crafting Slot Localization

- `Item HUD` now localizes the `Include Crafting Slots` source toggle in the
  addon language files too, so the XCarry-aware count option appears correctly
  in both English and Chinese module UIs.

## 1.6.66 Elytra Bounce Auto-Jump Guard

- `Elytra Bounce` now waits until the player is actually wearing a glideable
  elytra before it starts forcing the jump key, so simply leaving the module
  enabled no longer causes constant hopping while walking around without an
  elytra equipped.

## 1.6.67 Chinese Lang JSON Repair

- The addon Chinese language file is now valid JSON again and includes both the
  shared `include_crafting_slots` setting key and the `Item HUD` module key, so
  Chinese localization can be parsed reliably instead of depending on resource
  loaders tolerating malformed files.

## 1.6.68 Elytra Bounce Liquid Guard

- `Elytra Bounce` now treats water, underwater states, and lava as non-control
  environments too, so the module releases its forced movement and sprint input
  there instead of continuing to drive the player while gliding is impossible.

## 1.6.69 PacketEat Scope Narrowing

- `PacketEat` no longer cancels every `RELEASE_USE_ITEM` packet head-on; it only
  intercepts the specific always-edible food case it is meant to protect, which
  keeps unrelated use-release packets on the vanilla/Epsilon path.

## 1.6.70 Elytra Bounce Start-Check Alignment

- `Elytra Bounce` now uses the same control-boundary check for both movement
  control and the relaunch packet path, so the module no longer carries a stale
  liquid-only guard on the start-fall-flying send path.

## 1.6.81 Creative HUD Screen Guard

- `Armor HUD+`, `Dropped Item HUD`, and `Item HUD` now always skip
  `CreativeModeInventoryScreen`, even if `Render In Screens` is enabled, so the
  creative item tabs keep their vanilla rendering and interaction path.

## 1.6.83 NoSlow Login Pong Guard

- Added a narrow `NoSlow` GrimC0F state reset before login/common pong packets
  are handled when the client is not fully in-world yet. This prevents stale
  NoSlow state from cancelling the required pong packet or trying to send an
  offhand-swap packet while `Minecraft#getConnection()` is still unavailable,
  which could disconnect the client at the loading-terrain stage with a network
  protocol error.

## 1.6.84 Packet Mine Countdown Prediction

- Added an addon mixin for Epsilon `Packet Mine` that injects a render-only
  mining countdown overlay. It reads Packet Mine's existing primary and
  double-break target positions, progress counters, selected tool prediction,
  ground-check slowdown, and `Damage` setting to show a predicted remaining
  time/tick count plus progress percent without changing the packet mine action
  path.
- Added `Mine Countdown`, `Countdown Mode`, `Countdown Scale`, `Countdown Y
  Offset`, `Countdown Background`, and countdown color settings to Packet Mine
  so the overlay can be tuned or disabled independently from the existing 3D
  block render.

## 1.6.85 Bed Render Module

- Added a Fabric-only `Bed Render` addon module that scans nearby loaded blocks
  on a configurable delay and renders cached beds as a single combined bed
  shape. It normalizes rendering to the bed head so the head and foot halves do
  not duplicate each other.
- Added range, vertical range, fill, outline, blur, line width, color, scan
  delay, and max-bed settings so bed highlighting can be tuned without touching
  upstream Epsilon render modules.

## 1.6.86 NoFall Grim Auto Fall Distance

- Added `Auto Fall Distance` to upstream Epsilon `No Fall` through an addon
  mixin. The setting is only available for `GrimSimulation` and leaves the
  saved manual `Fall Distance` slider value unchanged when toggled off.
- When enabled, the Grim branch estimates remaining fall height by scanning
  loaded collision shapes below the player's footprint, combines that with the
  current accumulated fall distance, and maps the result back into the existing
  `3-16` slider range with a safety margin before landing.

## 1.6.87 Feet Trap Below-Air Render

- Added a render-only Feet Trap extension that draws replaceable air blocks
  directly below planned trap positions while the module is active. This makes
  unsupported side placements visible without changing Feet Trap's placement,
  rotation, switch, or helper-block behavior.
- Added `Render Below Air`, `Below Air Side Color`, and `Below Air Line Color`
  settings with English and Chinese localization.

## 1.6.88 Feet Trap Below-Air Setting Check

- Rechecked the new Feet Trap below-air render settings and fixed the `Render
  Below Air` visibility dependency so disabling it does not hide the toggle
  itself. The toggle now follows upstream `Render`; the color settings remain
  hidden unless below-air rendering is enabled.

## 1.6.89 Feet Trap Air Render Module

- Replaced the upstream `Feet Trap` setting injection with an independent
  Fabric addon module named `Feet Trap Air Render`, so the visual helper can be
  toggled separately and no longer shadows Feet Trap internals.
- Changed rendering from a full air-block box below the planned placement to
  only the bottom face of each planned Feet Trap block. With `Only Below Air`
  enabled, the face is drawn only when the block directly below is replaceable
  air and not another planned Feet Trap placement.

## 1.6.90 Render Optimization Pass

- `Feet Trap Air Render` now caches computed target faces behind a configurable
  `Refresh Delay`, defaulting to 50 ms, so entity collision checks and planned
  placement calculation no longer run every rendered frame.
- `Bed Render` now trusts its scan cache during render frames and only performs
  cheap distance culling while drawing. Bed existence/state validation remains
  tied to the existing `Scan Delay` refresh path.
- `Packet Mine` countdown now checks that the target chunk is loaded before
  reading block state for overlay validity.

## 1.6.92 Runtime And Package Optimization Pass

- `Item HUD` now caches the normalized tracked item list until the setting or
  invalid-item behavior changes, avoiding per-frame ID parsing and registry
  lookups.
- `Item HUD` now counts all enabled inventory sources in one pass and maps only
  tracked items, instead of scanning the inventory once per tracked item.
- `Dropped Item HUD` now refreshes dropped-item aggregation behind a
  configurable `Refresh Delay`, defaulting to 100 ms, while render frames reuse
  cached text lines and bounds.
- Dropped item collection for HUDs now avoids building unused item label lists,
  and `NameTags` dropped-item label collection filters invalid item entities in
  the level query before sorting.
- Removed obsolete generic entity-HUD helpers and unused record fields left
  over from deleted modules, shrinking the generated class set to the current
  Fabric-only feature surface.
- Gradle copy tasks now skip empty directories, and the default build no
  longer emits a sources jar so `build/libs` contains only the runtime Fabric
  addon jar after a clean build.

## 1.6.93 Feet Trap Air Render ABI Fix

- Crash report `错误报告-2026-6-15_19.28.05.zip` showed a runtime
  `NoSuchMethodError` for `Render3DUtils.drawFilledSide(AABB, Color,
  Direction)` from `FeetTrapAirRender`.
- `Feet Trap Air Render` now avoids Epsilon's side-specific render helper
  methods and renders the target bottom face as a very thin AABB through the
  older filled/outline box helpers. This keeps the "bottom face only" visual
  intent while staying compatible with the user's current Epsilon runtime.

## 1.6.94 Feet Trap Air Render Block Gate

- Added `Require Feet Trap Blocks` to `Feet Trap Air Render`. When disabled,
  target collection and rendering keep the previous behavior.
- Added `Min Feet Trap Blocks`, visible only when the new gate is enabled. The
  module counts non-replaceable blocks in the same surrounding feet-trap ring
  used for target discovery, and suppresses air-face rendering until the count
  reaches the configured minimum.

## 1.6.95 Feet Trap Air Render Existing-Block Gate

- The Chinese module list now keeps this module's display name in English as
  `Feet Trap Air Render`, matching the Java registration name and English
  language file.
- With `Require Feet Trap Blocks` enabled, `Feet Trap Air Render` now targets
  existing surrounding feet-trap blocks instead of planned placements, and only
  renders their bottom face when the block directly below is air. The previous
  planned-placement behavior is preserved when the gate is disabled.

## 1.6.96 Feet Trap Air Render Only Ground

- Added an `Only Ground` toggle to `Feet Trap Air Render`. It defaults off to
  preserve existing behavior; when enabled the module clears cached markers and
  skips rendering while the player is not on the ground.
- The Chinese display name for this addon module now uses Epsilon's own `Feet
  Trap` translation, `围脚`, instead of showing the English addon-internal name.

## 1.6.97 Feet Trap Air Render Functional Name

- Renamed the Chinese display name from `围脚` to `围脚悬空`, avoiding a
  duplicate-looking entry next to Epsilon's own `Feet Trap` module while keeping
  the name tied to this module's below-air bottom-face render behavior.

## 1.6.98 Feet Trap Air Render Full-Block Guard

- `Feet Trap Air Render` now requires existing surrounding trap blocks to have
  a full collision shape before they can be counted or rendered, so slabs,
  walls, fences, and other incomplete blocks do not produce markers.
- While the player is on the ground, the module also suppresses rendering when
  the player's current support is not a full collision block, preventing stale
  or misleading markers while standing on slabs and other partial blocks.

## 1.6.99 PacketEat Elytra Release Bypass

- Superseded by `1.6.101`: the broad elytra release bypass was too permissive
  for always-edible foods and is now replaced with a stale-cache cleanup that
  preserves `Packet Eat`'s intended food release cancellation.

## 1.6.100 Mace Aura Tick Bridge

- `MixinMaceAura` adds a `PlayerTickEvent.Pre` listener that delegates to
  Epsilon's existing `MaceAura` tick logic. Upstream registers the handler with
  the base `PlayerTickEvent` type, while the event bus posts exact `Pre`/`Post`
  event classes, so the original module could stay idle and never attack.

## 1.6.101 Elytra Food And Ground NoSlow

- `MixinElytraFlightMode` lets Epsilon `Elytra Fly` food right-clicks pass
  through instead of cancelling them as generic right-clicks. While the player
  is actively eating food, automatic Elytra Fly fireworks and unbreaking armor
  swaps are skipped so they do not interrupt apples or golden apples mid-use.
- `MixinPacketEat` again cancels `RELEASE_USE_ITEM` for cached always-edible
  food stacks while clearing stale cached stacks when the player is no longer
  using an item.
- `MixinGUIMove` replaces Epsilon `GUI Move` keyboard handling with bound-key
  aware state sync. Inventory movement now updates both `KeyboardInputEvent`
  and the underlying movement `KeyMapping` states, keeping elytra control
  modules responsive while screens are open.
- `MixinNoSlow` adds `Only Ground`, defaulting off. When enabled, airborne
  slowdown handling is skipped and GrimC0F transient state is reset instead of
  continuing to affect elytra flight or airborne item use.

## 1.6.102 Elytra UI Right-Click Pass-Through

- `MixinElytraFlightMode` now also disables Elytra Fly's generic right-click
  cancellation while any client screen is open. This keeps Epsilon module UIs
  and other screen right-click interactions usable during elytra flight without
  changing in-world flight behavior.

## 1.6.103 Elytra Bounce Dual Pitch Settings

- `Elytra Bounce` now exposes separate `Dive Pitch` and `Lift Pitch` settings
  instead of a single shared pitch slider.
- While fall flying, the module keeps the downward dive angle during descent
  and automatically switches to the upward lift angle whenever the player is
  rising, making bounce tuning more controllable without changing relaunch,
  sprint, or other control logic.

## 1.6.104 Epsilon 2026.6.3 Baseline Refresh

- Rebased the addon against upstream `Epsilon` `26.1.x` commit `7792aca`,
  which corresponds to release version `2026.6.3`.
- Updated the local Fabric dependency baseline to
  `epsilon-fabric-26.1.2-2026.6.3-7792aca.jar`.
- Removed `MixinRotationUtils` because upstream `RotationManager` now
  initializes and resets its rotation state defensively, covering the null
  rotation smoothing guard that previously required the addon patch.

## Future Audit Checklist

- Recheck upstream `NameTags` before changing `MixinNameTags`.
- Remove or gate a mixin only after confirming upstream provides the same option
  and the addon setting no longer needs to patch it.
- Prefer Epsilon's public `settings` list for setting placement or visibility
  instead of shadowing upstream private setting fields.
- Keep remaining shadows limited to fields the addon actually reads or writes.
- Keep the generated output Fabric-only: runtime main jar under `build/libs`;
  do not recreate sources jars, root-level artifact folders, or desktop handoff
  staging folders by default.
- Bump `mod_version` before every completed compile that follows source or
  metadata changes.
