# 构建产物记录

[English](ARTIFACTS.md) | [简体中文](ARTIFACTS_zh.md)

本仓库将完成构建后的 Fabric addon 正式产物保存在 `artifacts/` 目录下。

后续维护规则：

1. 每次完成构建后，将该版本产出的 jar 上传到对应版本目录。
2. 在本文档中补充该版本的简短更新说明。
3. `build/`、`.gradle/`、`tmp/` 等工作目录不要入库，只保留最终发布用的 jar。

## 1.6.102

产品：

- `artifacts/1.6.102/entityinfo-addon-1.6.102.jar`

更新内容：

- 修复了鞘翅飞行时原版和 Epsilon 模块 UI 的右键交互被错误拦截的问题。
- 保留了 `1.6.101` 中关于飞行时吃食物、GUI Move 键位同步和 `NoSlow Only Ground` 的修复。

## 1.6.103

产品：

- `artifacts/1.6.103/entityinfo-addon-1.6.103.jar`

更新内容：

- 为 `Elytra Bounce` 增加了独立的 `Dive Pitch` 和 `Lift Pitch` 设置。
- 下降时使用俯冲角，上升时自动切换为抬升角。

## 1.6.104

产品：

- `artifacts/1.6.104/entityinfo-addon-1.6.104.jar`

更新内容：

- 将 addon 基线同步到上游 `Epsilon 2026.6.3`，对应 `26.1.x` 分支提交 `7792aca`。
- 删除了已被上游覆盖的 `RotationUtils` 兼容 mixin。

## 1.6.105

产品：

- `artifacts/1.6.105/entityinfo-addon-1.6.105.jar`

更新内容：

- 重写了 `Bed Render` 的扫描路径，改为按区块缓存并分批刷新，不再在单次渲染刷新里全量重扫整个范围。
- 为床渲染增加了基于视锥的过滤，屏幕外的缓存床不再持续承担模糊、填充和描边的每帧开销。

## 1.6.106

产品：

- `artifacts/1.6.106/entityinfo-addon-1.6.106.jar`

更新内容：

- 将 Alien-Nightly 的 `Criticals+` 移植为本 addon 的新 Combat 模块。
- 加入多种发包暴击模式，包括 `OldNCP`、`Strict`、`NCP`、`NewNCP`、`Packet`、`BBTT`、`LowPacket`、`Grim`、`GrimCC`、`GrimV2`、`GrimV3` 和 `New2b2t`。

## 1.6.107

产品：

- `artifacts/1.6.107/entityinfo-addon-1.6.107.jar`

更新内容：

- 参考 Alien 的缩放实现，重做了 `Zoom` 的定时缓动动画，新增 `Animation Mode`、`Animation Time` 和 `Ease`，同时保留原来的平滑模式。
- 修补了上游 `Block Highlight` 的方块框渲染，改为按真实 `VoxelShape` 绘制，半砖和其他不完整方块不再被当作整方块高亮。
- 为 `Crystal Chams` 增加了 `Spin Sync`、`Bounce Height`、`Y Offset` 控制，并给 `Item HUD` 补上了 `Corner Radius`。

## 1.6.108

产品：

- `artifacts/1.6.108/entityinfo-addon-1.6.108.jar`

更新内容：

- 将这轮 Alien 风格优化继续扩展到了原先几个渲染模块之外，补强了 `XCarry` 的关闭时序处理。
- `XCarry` 现在会在背包界面刚关闭后的短暂缓冲窗口内继续拦截合成栏关闭包，减少背包关闭后一拍才发包时的漏拦截。
