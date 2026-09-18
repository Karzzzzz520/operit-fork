# Extension architecture

`operit-fork` separates host primitives from feature implementations.

- Host: lifecycle, Keystore, callback routing, network policy, provider registry, model config repository.
- Sandbox: Google, Microsoft, Claude, and other vendor protocols; OAuth scopes; request payloads; model discovery.
- Shared contracts: versioned capability interfaces and structured result types.

A sandbox package should be able to request a capability, perform an operation, and receive a structured result without depending on private Kotlin classes.

The first implementation target is the OAuth provider suite already deployed on the Android device.
