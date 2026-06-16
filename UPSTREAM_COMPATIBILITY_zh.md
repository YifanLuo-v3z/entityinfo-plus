# 上游兼容性说明

[English](UPSTREAM_COMPATIBILITY.md) | [简体中文](UPSTREAM_COMPATIBILITY_zh.md)

本文件是当前 Fabric-only 兼容性基线的中文概览，便于快速接手和维护。详细的历史审计条目仍保留在英文版 [UPSTREAM_COMPATIBILITY.md](UPSTREAM_COMPATIBILITY.md) 中。

## 当前基线

- Epsilon 分支：`26.1.x`
- 已核对上游提交：`7792aca`
- 目标上游版本：`2026.6.3`
- Minecraft：`26.1.2`
- Fabric Loader：`0.19.2`
- Fabric API：`0.150.0+26.1.2`
- 本地依赖 jar：`libs/epsilon-fabric-26.1.2-2026.6.3-7792aca.jar`

## 当前保留的 addon 模块

以下模块仍然保留，因为上游 Epsilon 还没有提供完全等价的实现：

- `Armor HUD+`
- `Bed Render`
- `Crystal Chams`
- `Dropped Item HUD`
- `Elytra Bounce`
- `Feet Trap Air Render`
- `Item HUD`
- `Shulker Viewer`
- `XCarry`
- `Zoom`

## 当前保留的 mixin

以下 mixin 仍然保留，因为它们要么扩展了上游行为，要么修复了上游尚未覆盖的问题：

- `MixinAbstractContainerScreen`
- `MixinAutoMend`
- `MixinCameraZoom`
- `MixinElytraFlightMode`
- `MixinEndCrystalRenderer`
- `MixinGUIMove`
- `MixinHandsView`
- `MixinKillAura`
- `MixinLocalPlayerXCarry`
- `MixinMaceAura`
- `MixinNameTags`
- `MixinNoFall`
- `MixinNoSlow`
- `MixinPacketEat`
- `MixinPacketMineCountdown`

## 已删除的重复内容

- `MixinRotationUtils` 已在 `1.6.104` 删除，因为上游 `RotationManager` 现在已经具备相同类别的空旋转状态保护与重置逻辑。

## 1.6.104 兼容性更新

- addon 已同步到上游 `Epsilon 2026.6.3`。
- 构建依赖切换为 `epsilon-fabric-26.1.2-2026.6.3-7792aca.jar`。
- 删除了一项已被上游覆盖的兼容性补丁，避免重复维护。

## 1.6.105 床渲染性能优化

- `Bed Render` 不再在单次刷新时全量扫描整个配置范围，而是改为按区块缓存并分批刷新，从而降低渲染线程的周期性卡顿峰值。
- 渲染阶段现在会跳过视锥外的床缓存，减少村庄或基地密集场景下的模糊、填充和描边开销。

## 后续维护原则

1. 只有在确认上游已完整覆盖相同行为后，才删除 addon 模块或 mixin。
2. 优先通过 Epsilon 的公共 `settings` 列表或公共 API 扩展功能，尽量避免对私有字段做脆弱绑定。
3. 保持项目为 Fabric-only，除非维护者明确要求恢复多加载器结构。
4. 每次包含源码或元数据改动并完成编译前，都要先更新 `mod_version`，避免产物混淆。
5. 如果需要完整历史变更上下文，请同时参考英文版的详细审计记录。
