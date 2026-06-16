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
