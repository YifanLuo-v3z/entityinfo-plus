# Build Artifacts

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
