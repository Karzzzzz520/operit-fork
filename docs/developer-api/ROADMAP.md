# Developer API roadmap

## Phase 1: stable bridge contracts

- Plugin identity and API version negotiation
- Secure token store backed by Android Keystore
- OAuth callback handoff with state and PKCE verifier
- Model configuration bridge
- Dynamic AI provider registration
- Structured diagnostics and audit events

## Phase 2: extension surface

- Lifecycle hooks
- Message and prompt hooks
- Event bus
- Controlled HTTP client
- Background task scheduler
- ToolPkg UI and resource APIs

## Phase 3: developer experience

- TypeScript declarations for sandbox packages
- Kotlin and Java examples
- Developer settings panel
- API compatibility checker
- Sample OAuth provider suite
- CI builds and release artifacts

## Security rules

Plugins receive capabilities explicitly. Tokens are never returned to arbitrary plugins unless the user grants access to a specific account and operation.
