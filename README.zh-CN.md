# operit-fork

面向开发者的 [Operit](https://github.com/AAswordman/Operit) Fork —— 一个运行在 Android 上的开源 AI Agent 平台。

在完整保留上游兼容性（应用功能、模型配置、ToolPkg、普通沙盒包、脚本格式、数据目录、配置迁移）的同时，为 Sandbox Package / ToolPkg 开放**版本化、权限感知的宿主桥接 API**。目标不是复制一个封闭应用，而是把 Operit 发展为可扩展的 Android AI Agent 平台。

## 核心特性

- **宿主桥接 API 层**（Kotlin 契约位于 `app/src/main/java/com/ai/assistance/operit/api/publicapi/`）：能力协商与 API 版本协商、安全 Token 存储、OAuth 回调路由、受控 HTTP、模型配置桥接、AI Provider 注册、事件总线、生命周期钩子与结构化诊断。
- **`OperitFork` JavaScript 运行时**：桥接已注入沙盒 ToolPkg 运行时，包可直接调用宿主 API（见下）。
- **厂商逻辑留在宿主之外**：厂商专属 OAuth 与 Provider 行为放在 Sandbox Package / ToolPkg，宿主只做桥接，不重复实现私有协议。
- **权限感知 + 诊断优先**：扩展能力统一走 capability 声明与权限控制；Token 保留在宿主桥接之后，不向任意插件暴露。
- **云编译 CI**：GitHub Actions 工作流（`Android Build` / `Android Tests` / `PR Check`）支持 `workflow_dispatch` 手动触发，并发布已签名 Release APK。

## OperitFork JavaScript API

Sandbox Package 与 ToolPkg 扩展会拿到全局 `OperitFork`。它由 `core/tools/javascript/JsForkApiBridge.kt` 构建，
注册为 bootstrap 模块 `quickjs/init/fork-api-bridge.js`，并经 `NativeInterface.operitForkInvoke` 调度到
`api/publicapi/ForkApiBridge.kt`。类型声明在 `examples/types/fork-api.d.ts`。

```js
// 仅用保守语法：不要用可选链、箭头函数或模板字符串。
if (typeof OperitFork !== "undefined" && OperitFork.isAvailable()) {
    OperitFork.negotiate({
        packageId: "com.example.mypkg",
        apiVersion: "1.0.0",
        requestedCapabilities: ["secure_token_store", "controlled_http"],
    }).then(function (granted) {
        if (!granted.success) {
            complete("negotiate failed: " + granted.errorCode);
            return;
        }
        return OperitFork.secureTokenStore.get("com.example.mypkg", "account-1").then(function (token) {
            complete(token.success ? token.value : token.errorCode);
        });
    });
}
```

### 能力清单

| 能力标识 | JavaScript 命名空间 | 用途 |
| --- | --- | --- |
| `secure_token_store` | `OperitFork.secureTokenStore` | 按包+账户读写 Token |
| `oauth_callback` | `OperitFork.oauthCallback` | OAuth 会话开始 / 回调消费 / 取消，含 PKCE + state |
| `model_config` | `OperitFork.modelConfig` | 模型配置增删改查 |
| `ai_provider` | `OperitFork.aiProvider` | 注册 / 注销 / 列出 AI Provider |
| `controlled_http` | `OperitFork.controlledHttp` | 受策略约束的 HTTP 请求 |
| `event_bus` | `OperitFork.eventBus` | 事件发布 / 订阅 / 退订 |
| `lifecycle_hook` | `OperitFork.lifecycleHook` | 包加载 / 启用 / 禁用 / 卸载回调 |
| `developer_diagnostics` | `OperitFork.developerDiagnostics` | 诊断事件记录 / 查询 / 清理 |

辅助方法：`OperitFork.isAvailable()`、`OperitFork.getHostInfo()`、`OperitFork.listCapabilities()`、
`OperitFork.isCapabilityGranted(packageId, capability)`。

## 当前状态

- Kotlin 契约与 `OperitFork` JS 注入**均已落地**。
- 随附实现属于**开发者预览**：内存权限、明文偏好存储 Token、无 UI 确认、无生产级策略。
- `aiProvider.execute` 返回 `not_implemented`；Provider 的具体执行仍归沙盒侧实现。

## 开发者预览版构建

`debug` 变体有意使用与 release 相同的 applicationId：`com.ai.assistance.operit`。
这样预览版能复用已安装的 Sandbox Package、偏好设置、Token 记录与模型配置。由于 Android 视其为同一应用身份，
预览版 APK 无法与 release 版共存，二者只能装其一。

用 `assembleDebug` 构建预览版；运行时会在应用启动时初始化 `DeveloperApiRuntime`。
已签名的 Release APK 由 `Android Build` 工作流以 `:app:assembleRelease` 发布。
Release tag 采用 fork 方案 `fork-<官方版本>-f<N>`（例如 `fork-1.12.2-f1`）。
不要使用上游的 `1.12.2+N` 构建元数据命名，那是官方发布方案，跟随官方版本时会冲突。

## Fork 专用 Skill

Fork 专用的 `ForkAPI_DEV` skill 会把 Fork 能力文档与完整类型（含 `fork-api.d.ts`）安装到
`/sdcard/Download/Operit/skills/ForkAPI_DEV/`，位于「快速创作你的插件」对话框中，与上游 `SandboxPackage_DEV` 并列。

## 分支

- `main` —— fork 基线与可审阅的公开 API 基线
- `develop` —— 持续开发分支
- `api-v1` —— 开发者 API 契约、文档与实验实现

## 文档

- `docs/developer-api/FORK_CAPABILITIES.md` —— 能力参考与注入链路
- `docs/developer-api/ARCHITECTURE.md` —— 架构说明
- `docs/developer-api/ROADMAP.md` —— 路线图
- `docs/FORK_API_SKILL.md` —— ForkAPI_DEV skill 说明
- `examples/fork-api-test/` —— 针对注入 API 的自包含探针包

## 许可证

基于 [AAswordman/Operit](https://github.com/AAswordman/Operit) Fork，遵循上游 LGPL-3.0 及适用的第三方许可证。
