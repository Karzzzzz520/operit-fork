# operit-fork

Developer-first fork of [Operit](https://github.com/AAswordman/Operit) — an open-source AI Agent platform for Android.

It keeps full upstream compatibility (app features, model configuration, ToolPkg, sandbox packages, script formats, data directory, config migration) while opening **versioned, permission-aware host bridge APIs** for Sandbox Packages and ToolPkg extensions. The goal is not to duplicate a closed app, but to grow Operit into an extensible Android AI Agent platform.

## Highlights

- **Host bridge API layer** (Kotlin contracts under `app/src/main/java/com/ai/assistance/operit/api/publicapi/`): capability negotiation & API versioning, secure token storage (Android Keystore), OAuth callback routing, controlled HTTP, model configuration bridge, AI provider registration, event bus, lifecycle / message / prompt hooks, tool registration, and structured diagnostics.

- **Vendor logic stays out of the host**: vendor-specific OAuth and provider behavior lives in Sandbox Packages / ToolPkg. The host only bridges; it does not reimplement private protocols. ChatGPT/Codex keeps Operit's native OAuth.

- **Permission-aware & diagnostics-first**: extensions go through capability declarations and permission control. Tokens are stored in Keystore and never exposed to plug-ins.

- **Cloud CI**: GitHub Actions workflows (`Android Build` / `Android Tests` / `PR Check`) support manual `workflow_dispatch` triggers.

## Status note

The fork host API is currently landed on the **Kotlin contract layer**. The corresponding JS side (an `OperitFork` global injected into the ToolPkg JavaScript runtime) and the type declarations are **not yet wired** — see the open items under `docs/developer-api/`. Preview declarations for the target call shapes live in `examples/types/fork-api.d.ts`. This is intentional in-progress work, not a regression.

## Developer Preview Build

The `debug` variant intentionally uses the same application ID as release: `com.ai.assistance.operit`. This lets the preview reuse the same installed Sandbox Packages, preferences, token records, and model configurations. Because Android treats them as the same application identity, the preview APK cannot be installed alongside the release APK; use one or the other.

Build the preview with `assembleDebug`. The preview runtime initializes `DeveloperApiRuntime` during application startup. Sandbox Package and ToolPkg code should prefer the public bridge APIs under `app/src/main/java/com/ai/assistance/operit/api/publicapi/`.

## Branches

- `main` — fork baseline + reviewable public API baseline
- `develop` — ongoing development
- `api-v1` — developer API contracts, docs, and experimental implementations

## Docs

- `docs/developer-api/ROADMAP.md` — roadmap
- `docs/developer-api/ARCHITECTURE.md` — architecture
- `docs/developer-api/FORK_CAPABILITIES.md` — Fork capabilities reference (8 host bridge capabilities and `OperitFork` call shapes)

## License

Fork of [AAswordman/Operit](https://github.com/AAswordman/Operit), follows upstream LGPL-3.0 and applicable third-party licenses.
