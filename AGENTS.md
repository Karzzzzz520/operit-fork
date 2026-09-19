# operit-fork 开发上下文

本仓库是 Operit 的开发者预览版 Fork。上游兼容性重要，但 Fork 专用公开 API 优先于上游手册中没有覆盖的能力。

## API 优先级

1. 先阅读 `app/src/main/java/com/ai/assistance/operit/api/publicapi/` 下的 Kotlin 契约和实现。
2. 再阅读 `docs/developer-api/` 中的路线、架构和桥接约定。
3. 上游 `docs/SCRIPT_DEV_SKILL.md` 只作为 Sandbox Package/ToolPkg 兼容参考，不是 Fork 专用能力的完整规范。
4. 新增宿主能力时，必须同时更新 Kotlin API、开发者文档和沙盒包调用约定。

## 架构边界

- OAuth 协议、PKCE、厂商 Token 交换、Provider 业务逻辑放在 Sandbox Package/ToolPkg。
- 宿主提供 Token 存储、OAuth 回调、HTTP、事件、模型配置、生命周期和诊断等桥接能力。
- 不要把厂商 OAuth 逻辑硬编码进宿主。

## 构建身份

- `debug` 使用正式包名 `com.ai.assistance.operit`，用于开发者预览和真实设备联调。
- 因此 Debug APK 与 Release APK 不能同时安装。
- Debug 构建命令：`assembleDebug`。
- OAuth 实验功能位于 `oauth-preview` 分支。

## 修改要求

- 保持改动集中，避免覆盖完整 Manifest 或大型 Kotlin 文件。
- 优先使用增量 API/提交。
- 完成代码后使用 GitHub Actions 云编译验证，不把“接口已写入”当作“构建已通过”。
- 任何构建失败都应以 Runner 日志为依据修复。
