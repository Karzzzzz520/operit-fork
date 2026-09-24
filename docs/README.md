# 文档总览

本仓库文档按主题分类维护，docs/doc-src/ 是长期维护的单一事实源（SSOT），其余为面向特定读者（包作者 / 贡献者 / 协作者）的入口文档。

## 面向包开发者的指南

| 文档 | 说明 |
| --- | --- |
| [包开发指南](SCRIPT_DEV_GUIDE.md) | 沙盒包 / ToolPkg 编写、构建、维护的完整教程（1087 行） |
| [ToolPkg 格式说明](TOOLPKG_FORMAT_GUIDE.md) | ToolPkg 包格式、Manifest、子包、IPC 的权威说明（1598 行） |
| [Sandbox Package 开发技能](SCRIPT_DEV_SKILL.md) | 安装更新 + Sandbox Package 撰写 + 发布市场（221 行） |
| [类型声明目录](../../examples/types/) | 包开发的完整 TS 类型系统，入口是 index.d.ts |

## Fork 专用能力（开发者预览）

| 文档 | 说明 |
| --- | --- |
| [Fork 能力参考](developer-api/FORK_CAPABILITIES.md) | 本 Fork 新增的宿主能力：8 个 capability 的契约与 JS 调用约定 |
| [Fork 架构](developer-api/ARCHITECTURE.md) | Fork 的架构定位与模块边界 |
| [OAuth 沙盒桥接](developer-api/OAUTH_SANDBOX_BRIDGE.md) | OAuth 协议落在沙盒包、宿主只提供桥接的约定 |
| [路线图](developer-api/ROADMAP.md) | Fork 开发者 API 的演进方向 |

## 单一事实源（SSOT）

- doc-src/architecture/：整体架构、核心模块和运行流程设计
- doc-src/dev-core/：构建、贡献指南和底层接口说明
- doc-src/feature-protocol/：意图触发、工具调用、聊天导入等功能协议
- doc-src/package-dev/：各功能包与业务模块的开发说明
- doc-src/research/：外部依赖 / API 的调研与验证
- doc-src/test-example/：测试示例、实验记录和问题分析

## 其他

- docs/TODO/：开发者协作用计划目录（[规范](TODO/README.md)）
- docs/assets/：文档使用的图片等资源
- docs/.META/：元数据目录
