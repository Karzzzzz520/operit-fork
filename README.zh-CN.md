# operit-fork

面向开发者的 [Operit](https://github.com/AAswordman/Operit) Fork —— 一个运行在 Android 上的开源 AI Agent 平台。

在完整保留上游兼容性（应用功能、模型配置、ToolPkg、普通沙盒包、脚本格式、数据目录、配置迁移）的同时，为 Sandbox Package / ToolPkg 开放**版本化、权限感知的宿主桥接 API**。目标不是复制一个封闭应用，而是把 Operit 发展为可扩展的 Android AI Agent 平台。

## 核心特性

- **宿主桥接 API 层**（Kotlin 契约位于 `app/src/main/java/com/ai/assistance/operit/api/publicapi/`）：能力协商与 API 版本协商、安全 Token 存储（Android Keystore）、OAuth 回调路由、受控 HTTP、模型配置桥接、AI Provider 注册、事件总线、生命周期 / 消息 / Prompt Hook、工具注册与结构化诊断。

- **厂商逻辑留在宿主之外**：厂商专属 OAuth 与 Provider 行为放在 Sandbox Package / ToolPkg，宿主只做桥接，不重复实现私有协议。ChatGPT/Codex 保留 Operit 原生 OAuth。

- **权限感知 + 诊断优先**：扩展能力统一走 capability 声明与权限控制；Token 存于 Keystore，不向插件暴露密钥材料。

- **云编译 CI**：GitHub Actions 工作流（`Android Build` / `Android Tests` / `PR Check`）支持 `workflow_dispatch` 手动触发。

## 当前状态说明

fork 宿主 API 目前落在 **Kotlin 契约层**。对应的 JS 侧（注入 ToolPkg JavaScript 运行时的 `OperitFork` 全局对象）与 `toolpkg.d.ts` 类型声明**尚未接通** —— 相关待办见 `docs/developer-api/`。这是进行中的工作，而非回退。

## 开发者预览版构建

`debug` 变体有意使用与 release 相同的 applicationId：`com.ai.assistance.operit`。这样预览版能复用已安装的 Sandbox Package、偏好设置、Token 记录与模型配置。由于 Android 视其为同一应用身份，预览版 APK 无法与 release 版共存，二者只能装其一。

用 `assembleDebug` 构建预览版；运行时会在应用启动时初始化 `DeveloperApiRuntime`。Sandbox Package 与 ToolPkg 代码应优先使用 `app/src/main/java/com/ai/assistance/operit/api/publicapi/` 下的公开桥接 API。

## 分支

- `main` —— fork 基线与可审阅的公开 API 基线
- `develop` —— 持续开发分支
- `api-v1` —— 开发者 API 契约、文档与实验实现

## 文档

- `docs/developer-api/ROADMAP.md` —— 路线图
- `docs/developer-api/ARCHITECTURE.md` —— 架构说明

## 许可证

基于 [AAswordman/Operit](https://github.com/AAswordman/Operit) Fork，遵循上游 LGPL-3.0 及适用的第三方许可证。
