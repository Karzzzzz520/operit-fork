# operit-fork

[中文](#中文说明) | [English](#english)

## 中文说明

`operit-fork` 是 Operit 的开发者优先 Fork。目标不是复制一个封闭应用，而是在保持上游功能和数据兼容性的同时，把 Operit 发展为可扩展的 Android AI Agent 平台。

### 核心原则

- 保留上游功能、配置和数据兼容性。
- 厂商协议、OAuth 流程和业务能力优先放在 Sandbox Package / ToolPkg。
- 宿主只提供安全、稳定、版本化的桥接 API。
- Token 默认进入 Android Keystore，不向插件暴露密钥材料。
- 所有扩展能力都经过 capability 声明、权限控制和结构化诊断。
- 尽量保持对上游 Operit 的同步能力。

### 开发者 API

当前 API 契约位于 `app/src/main/java/com/ai/assistance/operit/api/publicapi/`，包括：

- 能力协商与 API 版本；
- 安全 Token 存储；
- OAuth 回调桥接；
- 动态 AI Provider；
- 事件总线；
- 受控 HTTP；
- 模型配置读写；
- 开发者诊断。

### OAuth Provider 设计

Google Gemini、Microsoft、Claude 等厂商逻辑由沙盒子包实现。宿主负责：

- Keystore；
- OAuth 回调；
- Token 生命周期；
- 请求权限；
- Provider 注册；
- 模型配置桥接。

ChatGPT/Codex 保留 Operit 原生 OAuth，不重复实现私有协议。

### 分支

- `main`：Fork 基线与可审阅的公开 API 基线；
- `develop`：持续开发分支；
- `api-v1`：开发者 API 契约、文档和实验实现。

### 文档

- `docs/developer-api/ROADMAP.md`：路线图；
- `docs/developer-api/ARCHITECTURE.md`：架构说明。

### 许可证

本项目基于 AAswordman/Operit Fork，遵循上游 LGPL-3.0 及适用的第三方许可证。

## English

`operit-fork` is a developer-first fork of Operit. It preserves upstream compatibility while opening versioned, permission-aware APIs for Sandbox Packages and ToolPkg extensions.

The host provides secure primitives such as Android Keystore storage, OAuth callback routing, controlled HTTP, model configuration, provider registration, events, lifecycle hooks, and diagnostics. Vendor-specific OAuth and provider behavior remains in sandbox packages whenever possible.

This repository follows the upstream LGPL-3.0 license and applicable third-party licenses.
