package com.ai.assistance.operit.api.publicapi

/** Stable capability identifiers exposed to sandbox packages and ToolPkg extensions. */
object OperitDeveloperCapability {
    const val SECURE_TOKEN_STORE = "secure_token_store"
    const val OAUTH_CALLBACK = "oauth_callback"
    const val MODEL_CONFIG = "model_config"
    const val AI_PROVIDER = "ai_provider"
    const val CONTROLLED_HTTP = "controlled_http"
    const val EVENT_BUS = "event_bus"
    const val LIFECYCLE_HOOK = "lifecycle_hook"
    const val DEVELOPER_DIAGNOSTICS = "developer_diagnostics"
}

data class DeveloperApiVersion(val major: Int, val minor: Int, val patch: Int) {
    override fun toString(): String = "$major.$minor.$patch"
}

data class DeveloperApiManifest(
    val packageId: String,
    val apiVersion: DeveloperApiVersion,
    val requestedCapabilities: Set<String>,
    val displayName: String? = null
)

data class DeveloperApiResult<T>(
    val success: Boolean,
    val value: T? = null,
    val errorCode: String? = null,
    val message: String? = null
) {
    companion object {
        fun <T> ok(value: T? = null, message: String? = null) = DeveloperApiResult(true, value, message = message)
        fun <T> error(code: String, message: String) = DeveloperApiResult<T>(false, errorCode = code, message = message)
    }
}

interface DeveloperApiRegistry {
    fun negotiate(manifest: DeveloperApiManifest): DeveloperApiResult<Set<String>>
    fun isCapabilityGranted(packageId: String, capability: String): Boolean
}
