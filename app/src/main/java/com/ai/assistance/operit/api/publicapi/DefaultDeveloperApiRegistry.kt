package com.ai.assistance.operit.api.publicapi

class DefaultDeveloperApiRegistry(
    private val permissions: PluginPermissionStore,
    private val supported: Set<String> = setOf(
        OperitDeveloperCapability.SECURE_TOKEN_STORE,
        OperitDeveloperCapability.OAUTH_CALLBACK,
        OperitDeveloperCapability.MODEL_CONFIG,
        OperitDeveloperCapability.AI_PROVIDER,
        OperitDeveloperCapability.CONTROLLED_HTTP,
        OperitDeveloperCapability.EVENT_BUS,
        OperitDeveloperCapability.LIFECYCLE_HOOK,
        OperitDeveloperCapability.DEVELOPER_DIAGNOSTICS
    )
) : DeveloperApiRegistry {
    override fun negotiate(manifest: DeveloperApiManifest): DeveloperApiResult<Set<String>> {
        if (manifest.packageId.isBlank()) return DeveloperApiResult.error("invalid_package", "packageId is blank")
        val unsupported = manifest.requestedCapabilities - supported
        if (unsupported.isNotEmpty()) return DeveloperApiResult.error("unsupported_capability", unsupported.joinToString())
        return permissions.request(manifest.packageId, manifest.requestedCapabilities)
    }

    override fun isCapabilityGranted(packageId: String, capability: String): Boolean =
        permissions.granted(packageId).value?.contains(capability) == true
}
