# operit-fork

Developer-first fork of [Operit](https://github.com/AAswordman/Operit) — an open-source AI Agent platform for Android.

It keeps full upstream compatibility (app features, model configuration, ToolPkg, sandbox packages, script formats, data directory, config migration) while opening **versioned, permission-aware host bridge APIs** for Sandbox Packages and ToolPkg extensions. The goal is not to duplicate a closed app, but to grow Operit into an extensible Android AI Agent platform.

## Highlights

- **Host bridge API layer** (Kotlin contracts under `app/src/main/java/com/ai/assistance/operit/api/publicapi/`): capability negotiation and API versioning, secure token storage, OAuth callback routing, controlled HTTP, model configuration bridge, AI provider registration, event bus, lifecycle hooks, and structured diagnostics.
- **`OperitFork` JavaScript runtime**: the bridge is injected into the sandbox ToolPkg runtime, so packages can call the host API directly. See below.
- **Vendor logic stays out of the host**: vendor-specific OAuth and provider behavior lives in Sandbox Packages / ToolPkg. The host only bridges; it does not reimplement private protocols.
- **Permission-aware & diagnostics-first**: extensions go through capability declarations and permission control. Tokens stay behind the host bridge and are never handed to arbitrary plug-ins.
- **Cloud CI**: GitHub Actions workflows (`Android Build` / `Android Tests` / `PR Check`) support manual `workflow_dispatch` triggers and publish signed release APKs.

## OperitFork JavaScript API

Sandbox packages and ToolPkg extensions get an `OperitFork` global. It is built by
`core/tools/javascript/JsForkApiBridge.kt`, registered as the bootstrap module
`quickjs/init/fork-api-bridge.js`, and dispatched to `api/publicapi/ForkApiBridge.kt` through
`NativeInterface.operitForkInvoke`. Types live in `examples/types/fork-api.d.ts`.

```js
// Conservative JS only: no optional chaining, arrow functions or template literals.
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

### Capabilities

| Capability | JavaScript namespace | Purpose |
| --- | --- | --- |
| `secure_token_store` | `OperitFork.secureTokenStore` | Read/write tokens per package and account |
| `oauth_callback` | `OperitFork.oauthCallback` | Begin/consume/cancel OAuth sessions with PKCE and state |
| `model_config` | `OperitFork.modelConfig` | List/create/update/delete model configurations |
| `ai_provider` | `OperitFork.aiProvider` | Register/unregister/list AI providers |
| `controlled_http` | `OperitFork.controlledHttp` | Policy-bounded HTTP requests |
| `event_bus` | `OperitFork.eventBus` | Publish/subscribe/unsubscribe events |
| `lifecycle_hook` | `OperitFork.lifecycleHook` | Package load/enable/disable/unload callbacks |
| `developer_diagnostics` | `OperitFork.developerDiagnostics` | Record/query/clear diagnostics |

Helpers: `OperitFork.isAvailable()`, `OperitFork.getHostInfo()`, `OperitFork.listCapabilities()`,
and `OperitFork.isCapabilityGranted(packageId, capability)`.

## Current status

- The Kotlin contracts and the `OperitFork` JS injection are **in place**.
- The shipped implementations are **developer-preview** implementations: in-memory permissions,
  plain-preferences token storage, no UI confirmation and no production policy enforcement.
- `aiProvider.execute` returns `not_implemented`; provider execution still belongs to the
  sandbox-side provider implementation.

## Developer Preview Build

The `debug` variant intentionally uses the same application ID as release: `com.ai.assistance.operit`.
This lets the preview reuse the same installed Sandbox Packages, preferences, token records, and model
configurations. Because Android treats them as the same application identity, the preview APK cannot be
installed alongside the release APK; use one or the other.

Build the preview with `assembleDebug`. The runtime initializes `DeveloperApiRuntime` during application
startup. Signed release APKs are published by the `Android Build` workflow with `:app:assembleRelease`.
Release tags use the fork scheme `fork-<upstream-version>-f<N>` (for example `fork-1.12.2-f1`).
Do not use upstream build-metadata tags such as `1.12.2+N`; that naming belongs to the upstream
release scheme and collides when the fork follows upstream versions.

## Fork skill

The fork-only `ForkAPI_DEV` skill installs the Fork capability docs and the full type set
(including `fork-api.d.ts`) into `/sdcard/Download/Operit/skills/ForkAPI_DEV/`. It is offered next to
the upstream `SandboxPackage_DEV` skill in the quick plugin creator dialog.

## Branches

- `main` — fork baseline + reviewable public API baseline
- `develop` — ongoing development
- `api-v1` — developer API contracts, docs, and experimental implementations

## Docs

- `docs/developer-api/FORK_CAPABILITIES.md` — capability reference and injection chain
- `docs/developer-api/ARCHITECTURE.md` — architecture
- `docs/developer-api/ROADMAP.md` — roadmap
- `docs/FORK_API_SKILL.md` — ForkAPI_DEV skill overview
- `examples/fork-api-test/` — self-contained probe package for the injected API

## License

Fork of [AAswordman/Operit](https://github.com/AAswordman/Operit), follows upstream LGPL-3.0 and applicable third-party licenses.
