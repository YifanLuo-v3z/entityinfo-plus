# Build Artifacts

[English](ARTIFACTS.md) | [简体中文](ARTIFACTS_zh.md)

This repository keeps completed Fabric addon build outputs under `artifacts/`.

Rules for future updates:

1. After every completed build, upload the produced jar for that version.
2. Add a short update note for that version in this file.
3. Keep generated working directories such as `build/`, `.gradle/`, and `tmp/`
   out of Git. Only the final released jar should be copied into `artifacts/`.

## 1.6.102

Artifact:

- `artifacts/1.6.102/entityinfo-addon-1.6.102.jar`

Update notes:

- Restored vanilla and Epsilon module UI right-click behavior during elytra
  flight by bypassing Elytra Fly's generic right-click cancellation while a
  client screen is open.
- Kept the earlier `1.6.101` food-use and movement fixes intact, including
  elytra-flight eating, GUI Move key sync, and NoSlow's `Only Ground` option.

## 1.6.103

Artifact:

- `artifacts/1.6.103/entityinfo-addon-1.6.103.jar`

Update notes:

- Added separate `Dive Pitch` and `Lift Pitch` settings to `Elytra Bounce`.
- `Elytra Bounce` now uses the dive angle while descending and automatically
  swaps to the lift angle while the player is rising during fall flying.

## 1.6.104

Artifact:

- `artifacts/1.6.104/entityinfo-addon-1.6.104.jar`

Update notes:

- Updated the addon baseline to upstream `Epsilon 2026.6.3` on branch
  `26.1.x` commit `7792aca`.
- Removed the addon `RotationUtils` compatibility mixin because the same null
  rotation-state hardening is now covered upstream.

## 1.6.105

Artifact:

- `artifacts/1.6.105/entityinfo-addon-1.6.105.jar`

Update notes:

- Reworked `Bed Render` scanning to cache beds by chunk and refresh them in
  small chunk batches instead of rescanning the full radius in one render pass.
- Added frustum-aware bed rendering so off-screen cached beds no longer keep
  paying blur/fill/outline render cost every frame.

## 1.6.106

Artifact:

- `artifacts/1.6.106/entityinfo-addon-1.6.106.jar`

Update notes:

- Ported `Criticals+` from Alien-Nightly into the addon as a new Fabric combat
  module.
- Added packet-based critical modes including `OldNCP`, `Strict`, `NCP`,
  `NewNCP`, `Packet`, `BBTT`, `LowPacket`, `Grim`, `GrimCC`, `GrimV2`,
  `GrimV3`, and `New2b2t`.

## 1.6.107

Artifact:

- `artifacts/1.6.107/entityinfo-addon-1.6.107.jar`

Update notes:

- Reworked `Zoom` with Alien-inspired timed easing, including new `Animation
  Mode`, `Animation Time`, and `Ease` controls while keeping the original smooth
  path available.
- Patched upstream `Block Highlight` to render real voxel shapes instead of
  full cubes, so slabs and other partial blocks no longer highlight as complete
  blocks.
- Added Alien-style crystal animation controls with `Spin Sync`,
  `Bounce Height`, and `Y Offset`, and exposed `Corner Radius` for `Item HUD`.

## 1.6.108

Artifact:

- `artifacts/1.6.108/entityinfo-addon-1.6.108.jar`

Update notes:

- Extended the Alien-inspired optimization pass beyond the original render
  modules by hardening `XCarry` around inventory-close timing.
- `XCarry` now keeps a short inventory-screen grace window so the crafting-slot
  close packet is still cancelled when the inventory UI closes and the packet is
  sent on the following tick.
