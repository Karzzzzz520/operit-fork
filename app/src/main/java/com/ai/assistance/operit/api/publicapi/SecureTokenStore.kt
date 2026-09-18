package com.ai.assistance.operit.api.publicapi

/** Host-side contract. Implementations must use Android Keystore or an equivalent secure backend. */
interface SecureTokenStore {
    fun put(packageId: String, accountId: String, tokenJson: String): DeveloperApiResult<Unit>
    fun get(packageId: String, accountId: String): DeveloperApiResult<String>
    fun delete(packageId: String, accountId: String): DeveloperApiResult<Unit>
    fun listAccounts(packageId: String): DeveloperApiResult<List<String>>
    fun status(packageId: String, accountId: String): DeveloperApiResult<TokenStatus>
}

data class TokenStatus(
    val exists: Boolean,
    val expiresAtEpochMs: Long? = null,
    val refreshable: Boolean = false,
    val provider: String? = null
)
