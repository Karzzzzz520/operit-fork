# operit-fork

Developer-first fork of Operit. The project preserves upstream compatibility while opening stable extension APIs for Sandbox Packages, ToolPkg, OAuth, AI providers, storage, events, HTTP, lifecycle hooks, and developer tooling.

## Principles

- Keep upstream features and data compatibility.
- Put vendor logic in sandbox packages whenever possible.
- Keep host APIs small, explicit, versioned, and permission-aware.
- Never expose secrets or unrestricted host access to plugins by default.

## API roadmap

- `api-v1`: public developer API contracts and bridge interfaces.
- OAuth callback and secure token storage bridge.
- Dynamic provider registration and request execution bridge.
- Event bus, lifecycle hooks, diagnostics, and developer mode.

This repository is an independent fork of AAswordman/Operit and remains subject to the upstream LGPL-3.0 license and applicable third-party licenses.
