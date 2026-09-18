package com.ai.assistance.operit.api.publicapi

interface ControlledHttpClient {
    fun execute(packageId: String, request: HttpRequestSpec): DeveloperApiResult<HttpResponseSpec>
}

data class HttpRequestSpec(val method: String, val url: String, val headers: Map<String, String> = emptyMap(), val body: String? = null, val timeoutMs: Long = 30000, val tokenAccountId: String? = null)

data class HttpResponseSpec(val statusCode: Int, val headers: Map<String, String> = emptyMap(), val body: String = "", val requestId: String? = null)
