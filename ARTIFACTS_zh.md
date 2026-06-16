# 构建产物记录

[English](ARTIFACTS.md) | [简体中文](ARTIFACTS_zh.md)

本仓库将完成构建后的 Fabric addon 正式产物保存在 `artifacts/` 目录下。

后续维护规则：

1. 每次完成构建后，将该版本产出的 jar 上传到对应版本目录。
2. 在本文件中补充该版本的简短更新说明。
3. `build/`、`.gradle/`、`tmp/` 等工作目录不要入库，只保留最终发布用的 jar。

## 1.6.102

产物：

- `artifacts/1.6.102/entityinfo-addon-1.6.102.jar`

更新内容：

- 修复了鞘翅飞行时原版与 Epsilon 模块 UI 右键交互被错误拦截的问题。
- 保留了 `1.6.101` 中关于飞行吃食物、GUI Move 键位同步和 `NoSlow Only Ground` 的修复。

## 1.6.103

产物：

- `artifacts/1.6.103/entityinfo-addon-1.6.103.jar`

更新内容：

- 为 `Elytra Bounce` 添加了独立的 `Dive Pitch` 和 `Lift Pitch` 设置。
- 下降时使用俯冲角，上升时自动切换为抬升角。

## 1.6.104

产物：

- `artifacts/1.6.104/entityinfo-addon-1.6.104.jar`

更新内容：

- 将 addon 基线同步到上游 `Epsilon 2026.6.3`，对应 `26.1.x` 分支提交 `7792aca`。
- 删除了已经被上游覆盖的 `RotationUtils` 兼容性 mixin。

## 1.6.105

产物：

- `artifacts/1.6.105/entityinfo-addon-1.6.105.jar`

更新内容：

- 重写了 `Bed Render` 的扫描路径，改为按区块缓存并分批刷新，不再在单次渲染刷新里全量重扫整个范围。
- 为床渲染增加了基于视锥的过滤，屏幕外的缓存床不会继续承担模糊、填充和描边的每帧开销。
