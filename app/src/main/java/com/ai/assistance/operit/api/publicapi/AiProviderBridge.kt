package com.ai.assistance.operit.api.publicapi

interface AiProviderBridge {
    fun register(packageId: String, descriptor: AiProviderDescriptor): DeveloperApiResult<Unit>
    fun unregister(packageId: String, providerId: String): DeveloperApiResult<Unit>
    fun list(packageId: String? = null): DeveloperApiResult<List<AiProviderDescriptor>>
    fun execute(request: AiProviderRequest): DeveloperApiResult<AiProviderResponse>
}

data class AiProviderDescriptor(
    val id: String,
    val displayName: String,
    val capabilities: Set<String> = emptySet(),
    val accountId: String? = null
)

data class AiProviderRequest(
    val packageId: String,
    val providerId: String,
    val accountId: String?,
    val model: String,
    val messagesJson: String,
    val parametersJson: String = "{}"
)

data class AiProviderResponse(val content: String, val rawJson: String? = null, val usageJson: String? = null)
