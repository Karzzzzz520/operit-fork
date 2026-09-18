package com.ai.assistance.operit.api.publicapi

interface PluginPermissionStore {
    fun request(packageId: String, capabilities: Set<String>): DeveloperApiResult<Set<String>>
    fun granted(packageId: String): DeveloperApiResult<Set<String>>
    fun revoke(packageId: String, capabilities: Set<String> = emptySet()): DeveloperApiResult<Unit>
}

data class PermissionRequest(val packageId: String, val capabilities: Set<String>, val reason: String? = null)
