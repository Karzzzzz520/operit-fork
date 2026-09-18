package com.ai.assistance.operit.api.publicapi

import android.content.Context

/** Developer-preview token store. Plain preferences are intentional for local preview builds. */
class PreviewTokenStore(context: Context) : SecureTokenStore {
    private val prefs = context.getSharedPreferences("developer_preview_tokens", Context.MODE_PRIVATE)
    private fun key(packageId: String, accountId: String) = "$packageId::$accountId"

    override fun put(packageId: String, accountId: String, tokenJson: String): DeveloperApiResult<Unit> {
        prefs.edit().putString(key(packageId, accountId), tokenJson).apply()
        return DeveloperApiResult.ok()
    }

    override fun get(packageId: String, accountId: String): DeveloperApiResult<String> =
        prefs.getString(key(packageId, accountId), null)?.let { DeveloperApiResult.ok(it) }
            ?: DeveloperApiResult.error("not_found", "token account not found")

    override fun delete(packageId: String, accountId: String): DeveloperApiResult<Unit> {
        prefs.edit().remove(key(packageId, accountId)).apply()
        return DeveloperApiResult.ok()
    }

    override fun listAccounts(packageId: String): DeveloperApiResult<List<String>> {
        val prefix = "$packageId::"
        return DeveloperApiResult.ok(prefs.all.keys.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) })
    }

    override fun status(packageId: String, accountId: String): DeveloperApiResult<TokenStatus> =
        DeveloperApiResult.ok(TokenStatus(exists = prefs.contains(key(packageId, accountId))))
}
