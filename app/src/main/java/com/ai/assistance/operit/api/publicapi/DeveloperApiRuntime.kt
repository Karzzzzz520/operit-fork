package com.ai.assistance.operit.api.publicapi

/**
 * Developer-preview runtime assembly. Production policy gates are intentionally minimal.
 */
class DeveloperApiRuntime(
    val permissions: PluginPermissionStore = InMemoryPluginPermissionStore(),
    val events: DeveloperEventBus = InMemoryDeveloperEventBus(),
    val registry: DeveloperApiRegistry = DefaultDeveloperApiRegistry(permissions)
) {
    fun initialize(manifest: DeveloperApiManifest): DeveloperApiResult<Set<String>> =
        registry.negotiate(manifest)

    fun shutdown(packageId: String): DeveloperApiResult<Unit> {
        permissions.revoke(packageId)
        return DeveloperApiResult.ok()
    }
}
