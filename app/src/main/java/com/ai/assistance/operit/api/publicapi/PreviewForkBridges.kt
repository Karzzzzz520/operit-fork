package com.ai.assistance.operit.api.publicapi

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Developer-preview implementations for the public contracts that have no host impl yet.
 *
 * These are intentionally minimal and in-memory: the fork is a developer preview, so there is
 * no UI confirmation and no production policy enforcement. They exist so the OperitFork JS
 * namespace can be exercised end to end instead of returning not_implemented.
 */

class InMemoryOAuthBridge : OAuthBridge {
    private data class Pending(val request: OAuthBeginRequest)

    private val pending = ConcurrentHashMap<String, Pending>()

    override fun begin(request: OAuthBeginRequest): DeveloperApiResult<OAuthSession> {
        if (request.packageId.isBlank() || request.state.isBlank()) {
            return DeveloperApiResult.error("invalid_request", "packageId and state are required")
        }
        val sessionId = UUID.randomUUID().toString()
        pending[sessionId] = Pending(request)
        return DeveloperApiResult.ok(
            OAuthSession(
                sessionId = sessionId,
                provider = request.provider,
                state = request.state,
                redirectUri = request.redirectUri
            )
        )
    }

    override fun consumeCallback(
        packageId: String,
        state: String,
        code: String?,
        error: String?
    ): DeveloperApiResult<OAuthCallback> {
        val entry = pending.entries.firstOrNull { it.value.request.packageId == packageId && it.value.request.state == state }
            ?: return DeveloperApiResult.error("state_mismatch", "no pending oauth session for the given state")
        pending.remove(entry.key)
        return DeveloperApiResult.ok(
            OAuthCallback(
                provider = entry.value.request.provider,
                code = code,
                error = error,
                stateVerified = true
            )
        )
    }

    override fun cancel(packageId: String, sessionId: String): DeveloperApiResult<Unit> {
        val entry = pending[sessionId]
        if (entry != null && entry.request.packageId != packageId) {
            return DeveloperApiResult.error("forbidden", "oauth session belongs to another package")
        }
        pending.remove(sessionId)
        return DeveloperApiResult.ok<Unit>()
    }
}

class InMemoryModelConfigBridge : ModelConfigBridge {
    private data class Entry(val packageId: String, val view: ModelConfigView)

    private val configs = ConcurrentHashMap<String, Entry>()

    override fun list(packageId: String): DeveloperApiResult<List<ModelConfigView>> =
        DeveloperApiResult.ok(configs.values.filter { it.packageId == packageId }.map { it.view })

    override fun create(
        packageId: String,
        request: ModelConfigCreateRequest
    ): DeveloperApiResult<ModelConfigView> {
        if (request.name.isBlank() || request.endpoint.isBlank()) {
            return DeveloperApiResult.error("invalid_request", "name and endpoint are required")
        }
        val id = UUID.randomUUID().toString()
        val view =
            ModelConfigView(
                id = id,
                name = request.name,
                providerType = request.providerType,
                endpoint = request.endpoint,
                modelNames = request.modelNames,
                accountId = request.accountId
            )
        configs[id] = Entry(packageId, view)
        return DeveloperApiResult.ok(view)
    }

    override fun update(
        packageId: String,
        configId: String,
        request: ModelConfigUpdateRequest
    ): DeveloperApiResult<ModelConfigView> {
        val entry = configs[configId]
        if (entry == null || entry.packageId != packageId) {
            return DeveloperApiResult.error("not_found", "model config not found for this package")
        }
        val current = entry.view
        val updated =
            ModelConfigView(
                id = current.id,
                name = request.name ?: current.name,
                providerType = current.providerType,
                endpoint = request.endpoint ?: current.endpoint,
                modelNames = request.modelNames ?: current.modelNames,
                accountId = request.accountId ?: current.accountId
            )
        configs[configId] = Entry(packageId, updated)
        return DeveloperApiResult.ok(updated)
    }

    override fun delete(packageId: String, configId: String): DeveloperApiResult<Unit> {
        val entry = configs[configId]
        if (entry != null && entry.packageId != packageId) {
            return DeveloperApiResult.error("forbidden", "model config belongs to another package")
        }
        configs.remove(configId)
        return DeveloperApiResult.ok<Unit>()
    }
}

class InMemoryAiProviderBridge : AiProviderBridge {
    private data class Entry(val packageId: String, val descriptor: AiProviderDescriptor)

    private val providers = ConcurrentHashMap<String, Entry>()

    override fun register(packageId: String, descriptor: AiProviderDescriptor): DeveloperApiResult<Unit> {
        if (packageId.isBlank() || descriptor.id.isBlank()) {
            return DeveloperApiResult.error("invalid_request", "packageId and descriptor.id are required")
        }
        providers[descriptor.id] = Entry(packageId, descriptor)
        return DeveloperApiResult.ok<Unit>()
    }

    override fun unregister(packageId: String, providerId: String): DeveloperApiResult<Unit> {
        val entry = providers[providerId]
        if (entry != null && entry.packageId != packageId) {
            return DeveloperApiResult.error("forbidden", "provider belongs to another package")
        }
        providers.remove(providerId)
        return DeveloperApiResult.ok<Unit>()
    }

    override fun list(packageId: String?): DeveloperApiResult<List<AiProviderDescriptor>> =
        DeveloperApiResult.ok(
            providers.values
                .filter { packageId == null || it.packageId == packageId }
                .map { it.descriptor }
        )

    override fun execute(request: AiProviderRequest): DeveloperApiResult<AiProviderResponse> =
        DeveloperApiResult.error(
            "not_implemented",
            "provider execution requires a sandbox-side provider and is not wired in the developer preview"
        )
}

class NoopPluginLifecycle : PluginLifecycle {
    override fun onLoad(packageId: String): DeveloperApiResult<Unit> = DeveloperApiResult.ok<Unit>()
    override fun onEnable(packageId: String): DeveloperApiResult<Unit> = DeveloperApiResult.ok<Unit>()
    override fun onDisable(packageId: String): DeveloperApiResult<Unit> = DeveloperApiResult.ok<Unit>()
    override fun onUnload(packageId: String): DeveloperApiResult<Unit> = DeveloperApiResult.ok<Unit>()
}
