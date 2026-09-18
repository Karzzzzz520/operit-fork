package com.ai.assistance.operit.api.publicapi

interface OAuthBridge {
    fun begin(request: OAuthBeginRequest): DeveloperApiResult<OAuthSession>
    fun consumeCallback(packageId: String, state: String, code: String?, error: String?): DeveloperApiResult<OAuthCallback>
    fun cancel(packageId: String, sessionId: String): DeveloperApiResult<Unit>
}

data class OAuthBeginRequest(
    val packageId: String,
    val provider: String,
    val authorizationUrl: String,
    val redirectUri: String,
    val codeVerifier: String,
    val state: String
)

data class OAuthSession(val sessionId: String, val provider: String, val state: String, val redirectUri: String)

data class OAuthCallback(val provider: String, val code: String?, val error: String?, val stateVerified: Boolean)
