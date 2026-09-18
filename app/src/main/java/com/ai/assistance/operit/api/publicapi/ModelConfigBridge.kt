package com.ai.assistance.operit.api.publicapi

interface ModelConfigBridge {
    fun list(packageId: String): DeveloperApiResult<List<ModelConfigView>>
    fun create(packageId: String, request: ModelConfigCreateRequest): DeveloperApiResult<ModelConfigView>
    fun update(packageId: String, configId: String, request: ModelConfigUpdateRequest): DeveloperApiResult<ModelConfigView>
    fun delete(packageId: String, configId: String): DeveloperApiResult<Unit>
}

data class ModelConfigView(val id: String, val name: String, val providerType: String, val endpoint: String, val modelNames: List<String>, val accountId: String? = null)
data class ModelConfigCreateRequest(val name: String, val providerType: String, val endpoint: String, val modelNames: List<String>, val accountId: String? = null, val headersJson: String = "{}")
data class ModelConfigUpdateRequest(val name: String? = null, val endpoint: String? = null, val modelNames: List<String>? = null, val accountId: String? = null, val headersJson: String? = null)
