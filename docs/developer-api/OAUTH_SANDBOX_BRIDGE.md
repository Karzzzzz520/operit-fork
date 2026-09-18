# OAuth 沙盒桥接约定

`operit-fork` 不实现厂商 OAuth 协议。Google、Claude、ChatGPT、Microsoft 等授权逻辑继续由 Sandbox Package/ToolPkg 负责。宿主只提供开发预览版回调入口。

## 回调入口

宿主注册 `OAuthCallbackActivity`，浏览器回调使用：

`operit-oauth://callback`

Activity 不解析厂商业务，只提取 `state`、`code`、`error`，并发送包内广播：

`com.ai.assistance.operit.api.OAUTH_CALLBACK`

## 沙盒包职责

1. 生成 PKCE、state 与授权 URL。
2. 打开系统浏览器。
3. 监听宿主广播并校验 state。
4. 使用自身 OAuth 工具完成 code/token 交换与刷新。
5. 通过 `SecureTokenStore`/预览版 Token 桥接保存凭证。

该设计保持宿主轻量，避免把厂商逻辑硬编码进本体；开发者预览版暂不加入额外确认 UI、URL 白名单或生产级安全策略。
