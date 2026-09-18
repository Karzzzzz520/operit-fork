package com.ai.assistance.operit.api.publicapi

import android.content.Context
import org.json.JSONArray

/** Lightweight preview persistence; deliberately avoids UI confirmation and policy enforcement. */
class PersistentPluginPermissionStore(context: Context) : PluginPermissionStore {
    private val prefs = context.getSharedPreferences("developer_api_permissions", Context.MODE_PRIVATE)

    override fun request(packageId: String, capabilities: Set<String>): DeveloperApiResult<Set<String>> {
        val merged = granted(packageId).value.orEmpty() + capabilities
        prefs.edit().putString(packageId, JSONArray(merged.toList()).toString()).apply()
        return DeveloperApiResult.ok(merged)
    }

    override fun granted(packageId: String): DeveloperApiResult<Set<String>> {
        val raw = prefs.getString(packageId, null) ?: return DeveloperApiResult.ok(emptySet())
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return DeveloperApiResult.ok(emptySet())
        return DeveloperApiResult.ok(buildSet { for (i in 0 until array.length()) add(array.optString(i)) })
    }

    override fun revoke(packageId: String, capabilities: Set<String>): DeveloperApiResult<Unit> {
        if (capabilities.isEmpty()) prefs.edit().remove(packageId).apply()
        else request(packageId, emptySet()).value.orEmpty().minus(capabilities).let {
            prefs.edit().putString(packageId, JSONArray(it.toList()).toString()).apply()
        }
        return DeveloperApiResult.ok()
    }
}
