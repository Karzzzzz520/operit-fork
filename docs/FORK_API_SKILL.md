---
name: ForkAPI_DEV
description: 用于 operit-fork 开发者 API（OperitFork 宿主桥接能力）的包开发。
---

# ForkAPI_DEV

这个 skill 面向 operit-fork（开发者预览版），帮助你在 Sandbox Package / ToolPkg 中使用 Fork 新增的宿主桥接能力。
它与上游的 `SandboxPackage_DEV` 相互独立：通用包开发仍用上游 skill，需要 Fork 能力时用本 skill。

## 安装与更新

安装与更新都走同一个脚本 `install_or_update.js`（由宿主下载到 `scripts/` 后执行）：

- 安装位置：`/sdcard/Download/Operit/skills/ForkAPI_DEV/`
- `SKILL.md`：`docs/FORK_API_SKILL.md`
- `references/`：Fork 能力与桥接架构文档
- `types/`：完整类型声明，额外包含 `fork-api.d.ts`
- `examples/packages/`：内置示例包

宿主端入口见 `QuickPluginCreatorSetupSupport.kt` 的 Fork 安装函数。

## Fork 能力总览

Fork 通过全局命名空间 `OperitFork` 暴露 8 个宿主桥接能力：

| 能力标识 | 用途 |
| --- | --- |
| `secure_token_store` | 按包+账户读写 Token（Android Keystore 后端） |
| `oauth_callback` | OAuth 授权开始 / 回调消费 / 取消，PKCE + state 校验 |
| `model_config` | 模型配置的增删改查 |
| `ai_provider` | 注册 / 注销 / 列出 Provider，并执行一次对话 |
| `controlled_http` | 受策略约束的 HTTP 请求 |
| `event_bus` | 事件发布 / 订阅 / 退订 |
| `lifecycle_hook` | 包加载 / 启用 / 禁用 / 卸载回调 |
| `developer_diagnostics` | 诊断事件记录 / 查询 / 清理 |

权威契约参考：[references/FORK_CAPABILITIES.md](references/FORK_CAPABILITIES.md)。

## 设计边界

- 宿主只提供桥接原语；厂商 OAuth、PKCE、Token 交换、Provider 业务逻辑放在沙盒包里。
- 包通过 manifest 显式请求能力，宿主按包协商授权。
- Token 只在用户授权特定账户与操作时才返回给包，不向插件暴露密钥材料。

## 运行时注入状态

`OperitFork` 命名空间**已注入 JS 运行时**：

- 桥脚本：`core/tools/javascript/JsForkApiBridge.kt`，注册为 bootstrap 模块 `quickjs/init/fork-api-bridge.js`（`globals = ["OperitFork"]`）。
- 原生调度：`JsEngine.operitForkInvoke` → `api/publicapi/ForkApiBridge.kt` → publicapi 契约。
- 类型声明与注入签名一致，见 `types/fork-api.d.ts`。
- 仍建议做特性探测，以便在旧宿主上优雅降级。

> ToolPkg JS 解析器拒绝现代语法。包内请使用 `var` / 普通 `function`，不要用可选链 `?.`、箭头函数或模板字符串。

```js
if (typeof OperitFork !== "undefined" && OperitFork.isAvailable()) {
    OperitFork.negotiate({
        packageId: "com.example.mypkg",
        apiVersion: "1.0.0",
        requestedCapabilities: ["secure_token_store", "oauth_callback"],
    }).then(function (granted) {
        if (!granted.success) {
            complete("negotiate failed: " + granted.errorCode);
            return;
        }
        return OperitFork.secureTokenStore.get("com.example.mypkg", "account-1").then(function (token) {
            complete(token.success ? token.value : token.errorCode);
        });
    });
} else {
    complete("OperitFork is unavailable on this host");
}
```

所有能力方法返回统一的 `ApiResult<T>`（`success` / `value` / `errorCode` / `message`），不抛异常。

## 类型使用

引用 `types/index.d.ts` 即可获得全局类型（`index.d.ts` 会副作用导入 `fork-api.d.ts`）：

```ts
/// <reference path="./types/index.d.ts" />
```

## 相关文档

- [Fork 能力参考](references/FORK_CAPABILITIES.md)
- [Fork 架构](references/ARCHITECTURE.md)
- [OAuth 沙盒桥接](references/OAUTH_SANDBOX_BRIDGE.md)
- [包开发指南](references/SCRIPT_DEV_GUIDE.md)
- [ToolPkg 格式说明](references/TOOLPKG_FORMAT_GUIDE.md)
