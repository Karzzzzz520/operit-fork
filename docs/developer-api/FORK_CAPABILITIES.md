# Fork 能力参考（Developer API）

本 Fork（operit-fork）在保持与上游 Operit 兼容的基础上，为沙盒包 / ToolPkg 扩展新增了一组宿主能力（capabilities）。本文档是这组能力的权威参考：契约来源、JS 侧调用约定、当前注入状态。

## 设计原则

- 宿主只提供桥接原语：Token 存储、OAuth 回调、受控 HTTP、模型配置、AI Provider 注册、事件总线、生命周期、诊断。
- 厂商 OAuth 协议、PKCE、Token 交换、Provider 业务逻辑一律放在 Sandbox Package / ToolPkg 中，不得硬编码进宿主。
- 包通过 manifest 显式请求能力，宿主按包协商授权；Token 只在用户授权特定账户与操作时才返回给包。

## 能力清单（8 个）

能力标识定义在 OperitDeveloperCapability（app/src/main/java/com/ai/assistance/operit/api/publicapi/OperitDeveloperApi.kt）：

| 标识 | Kotlin 契约 | 用途 |
| --- | --- | --- |
| secure_token_store | SecureTokenStore | 按包+账户读写 Token（Android Keystore 后端） |
| oauth_callback | OAuthBridge | OAuth 授权开始 / 回调消费 / 取消，PKCE + state 校验 |
| model_config | ModelConfigBridge | 模型配置的增删改查（endpoint / modelNames / headers） |
| ai_provider | AiProviderBridge | 注册 / 注销 / 列出 Provider，并执行一次对话 |
| controlled_http | ControlledHttpClient | 受策略约束的 HTTP 请求（超时 / 头 / body） |
| event_bus | DeveloperEventBus | 事件发布 / 订阅 / 退订，跨包通信 |
| lifecycle_hook | PluginLifecycle | 包加载 / 启用 / 禁用 / 卸载的生命周期回调 |
| developer_diagnostics | DeveloperDiagnostics | 诊断事件记录 / 查询 / 清理 |

## JS 侧调用约定

### 现状（注入状态）

截至 2026-09，OperitFork 命名空间已注入 JS 运行时。实现链路：

- JS 桥：`core/tools/javascript/JsForkApiBridge.kt`（`buildForkApiBridgeScript()`），作为 bootstrap 模块 `quickjs/init/fork-api-bridge.js` 注册，`globals = ["OperitFork"]`。
- 执行预导入：`JsExecutionScriptBuilder.kt` 增加 `var OperitFork = globalThis.OperitFork;`（上游 21 个全局保持不变）。
- 原生调度：`JsEngine` 新增 `@JavascriptInterface fun operitForkInvoke(requestJson: String)`，路由到 `api/publicapi/ForkApiBridge.kt`。
- 能力实现：`ForkApiBridge` 装配 `DefaultDeveloperApiRegistry`、`PreviewTokenStore`、`PreviewControlledHttpClient`、`InMemoryDeveloperEventBus`、`InMemoryDeveloperDiagnostics`，以及 `InMemoryOAuthBridge` / `InMemoryModelConfigBridge` / `InMemoryAiProviderBridge` / `NoopPluginLifecycle`。
- 类型：`examples/types/fork-api.d.ts`，由类型入口 `index.d.ts` 副作用导入。

注入链路可概括为：Kotlin 契约 → ForkApiBridge → NativeInterface.operitForkInvoke → fork-api-bridge.js → OperitFork 全局 → fork-api.d.ts → 探针包。

包内仍建议做特性探测，以便在旧宿主上优雅降级。调用形态如下：

```ts
// 通过全局命名空间访问（现已可用）
const result = await OperitFork.secureTokenStore.get('my-package', 'account-1');

// 或通过 capability 协商
const granted = await OperitFork.negotiate({
  packageId: 'com.example.mypkg',
  apiVersion: '1.0.0',
  requestedCapabilities: ['secure_token_store', 'oauth_callback'],
});
```

### Kotlin 契约签名速查

以下接口全部位于 app/src/main/java/com/ai/assistance/operit/api/publicapi/，返回统一 DeveloperApiResult<T>（success / value / errorCode / message）：

- SecureTokenStore: put / get / delete / listAccounts / status
- OAuthBridge: begin / consumeCallback / cancel
- ModelConfigBridge: list / create / update / delete
- AiProviderBridge: register / unregister / list / execute
- ControlledHttpClient: execute(packageId, HttpRequestSpec) -> HttpResponseSpec
- DeveloperEventBus: publish / subscribe / unsubscribe
- PluginLifecycle: onLoad / onEnable / onDisable / onUnload
- DeveloperDiagnostics: record / query / clear

## 版本与权限

- API 版本：DeveloperApiVersion(major, minor, patch)，包 manifest 请求 apiVersion，宿主 negotiate 返回实际授予的能力集。
- 权限存储：PluginPermissionStore（内存 InMemoryPluginPermissionStore / 持久化 PersistentPluginPermissionStore），按包隔离。
- 运行时组装：DeveloperApiRuntime（DeveloperApiRuntime.kt）统一装配 registry / events / permissions。

## 扩展宿主接口

除 8 个 capability 外，OperitFork 还暴露一组直连宿主既有功能的接口：

| 命名空间 | 方法 | 说明 |
| --- | --- | --- |
| `OperitFork.tools` | `invoke(toolName, params)` | 调用宿主内置工具（`AIToolHandler.executeTool`） |
| `OperitFork.packages` | `list` / `enable` / `disable` / `isEnabled` | 沙盒包管理（`PackageManager`） |
| `OperitFork.host` | `getAppInfo()` | 包名 / versionName / versionCode |
| `OperitFork.update` | `check(currentVersion?)` | 检查 fork 发布渠道 |

### 沙盒包刷新（按需，非自动）

这是**显式触发**的刷新，不是常驻监听：放入新包后不会自动注册，必须调用一次刷新入口。

主入口（agent 调用）：

- 内置工具 `reload_sandbox_packages`，执行 `PackageManager.refreshExternalPackagesForDebug()`。

薄封装（沙盒包 / 脚本调用）：

- `OperitFork.packages.reload()`，内部调用同一个刷新动作，行为与内置工具完全一致。

刷新动作会重新扫描 `getExternalFilesDir/packages`、销毁旧执行引擎、重注册并重新激活活跃包，无需重启应用。

上游原有的广播通路保持不变：`PackageDebugRefreshReceiver`（action = `com.ai.assistance.operit.DEBUG_REFRESH_PACKAGES`）仍可由工具链触发。

## 相关文档

- [Fork 架构](ARCHITECTURE.md)
- [OAuth 沙盒桥接](OAUTH_SANDBOX_BRIDGE.md)
- [路线图](ROADMAP.md)
- [包开发指南](../SCRIPT_DEV_GUIDE.md)
- [ToolPkg 格式说明](../TOOLPKG_FORMAT_GUIDE.md)
