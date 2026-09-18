package com.ai.assistance.operit.api.publicapi

import java.util.concurrent.ConcurrentHashMap

/** Default runtime permission store. Replace persistence with app settings when wired into production. */
class InMemoryPluginPermissionStore : PluginPermissionStore {
    private val grants = ConcurrentHashMap<String, MutableSet<String>>()

    override fun request(packageId: String, capabilities: Set<String>): DeveloperApiResult<Set<String>> {
        if (packageId.isBlank()) return DeveloperApiResult.error("invalid_package", "packageId is blank")
        val granted = grants.computeIfAbsent(packageId) { ConcurrentHashMap.newKeySet() }
        granted.addAll(capabilities)
        return DeveloperApiResult.ok(granted.toSet())
    }

    override fun granted(packageId: String): DeveloperApiResult<Set<String>> =
        DeveloperApiResult.ok(grants[packageId]?.toSet().orEmpty())

    override fun revoke(packageId: String, capabilities: Set<String>): DeveloperApiResult<Unit> {
        if (capabilities.isEmpty()) grants.remove(packageId)
        else grants[packageId]?.removeAll(capabilities)
        return DeveloperApiResult.ok()
    }
}
