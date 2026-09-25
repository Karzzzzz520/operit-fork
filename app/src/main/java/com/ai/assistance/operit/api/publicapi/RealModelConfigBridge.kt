package com.ai.assistance.operit.api.publicapi

import android.content.Context
import com.ai.assistance.operit.data.model.ApiProviderType
import com.ai.assistance.operit.data.model.ModelConfigData
import com.ai.assistance.operit.data.model.getModelList
import com.ai.assistance.operit.data.preferences.ModelConfigManager
import com.ai.assistance.operit.util.AppLogger
import kotlinx.coroutines.runBlocking

/**
 * Real ModelConfigBridge backed by ModelConfigManager and the shared model_configs DataStore.
 *
 * The developer-preview in-memory bridge could not persist anything: ForkApiBridge is created per
 * JsEngine, so a create() was gone by the next list(). This implementation reads and writes the
 * same DataStore that the in-app model configuration screen uses, so sandbox packages can create
 * or update real model configurations.
 *
 * ModelConfigManager is suspend-only and ForkApiBridge.invoke is synchronous. The JS bridge runs
 * on the QuickJS execution thread rather than the main thread, so each call is wrapped in
 * runBlocking with a short, bounded body.
 */
class RealModelConfigBridge(
    context: Context,
    private val diagnostics: DeveloperDiagnostics? = null
) : ModelConfigBridge {
    private val appContext: Context = context.applicationContext
    private val manager = ModelConfigManager(appContext)

    override fun list(packageId: String): DeveloperApiResult<List<ModelConfigView>> =
        blocking("list") {
            DeveloperApiResult.ok(
                manager.getAllConfigSummaries().map { summary ->
                    ModelConfigView(
                        id = summary.id,
                        name = summary.name,
                        providerType = summary.apiProviderTypeId,
                        endpoint = summary.apiEndpoint,
                        modelNames = getModelList(summary.modelName)
                    )
                }
            )
        }

    override fun create(
        packageId: String,
        request: ModelConfigCreateRequest
    ): DeveloperApiResult<ModelConfigView> =
        blocking("create") {
            if (request.name.isBlank()) {
                return@blocking DeveloperApiResult.error<ModelConfigView>(
                    "invalid_request",
                    "name is required"
                )
            }
            val providerType = resolveProviderType(request.providerType)
            val configId = manager.createConfig(request.name)
            manager.updateModelConfig(
                configId = configId,
                apiKey = "",
                apiEndpoint = request.endpoint,
                modelName = request.modelNames.joinToString(","),
                apiProviderType = providerType,
                apiProviderTypeId = providerType.name
            )
            if (request.headersJson.isNotBlank()) {
                manager.updateCustomHeaders(configId, request.headersJson)
            }
            val view = viewOf(configId)
            if (view == null) {
                return@blocking DeveloperApiResult.error<ModelConfigView>(
                    "write_failed",
                    "model config could not be read back after create"
                )
            }
            record(packageId, "modelConfig.create", "created model config " + configId)
            DeveloperApiResult.ok(view)
        }

    override fun update(
        packageId: String,
        configId: String,
        request: ModelConfigUpdateRequest
    ): DeveloperApiResult<ModelConfigView> =
        blocking("update") {
            val current = currentIfExists(configId)
            if (current == null) {
                return@blocking DeveloperApiResult.error<ModelConfigView>(
                    "not_found",
                    "model config not found"
                )
            }
            if (request.name != null) {
                manager.updateConfigBase(configId, request.name)
            }
            if (request.endpoint != null || request.modelNames != null) {
                manager.updateModelConfig(
                    configId = configId,
                    apiKey = current.apiKey,
                    apiEndpoint = request.endpoint ?: current.apiEndpoint,
                    modelName = request.modelNames?.joinToString(",") ?: current.modelName,
                    apiProviderType = current.apiProviderType,
                    apiProviderTypeId = current.apiProviderTypeId
                )
            }
            if (request.headersJson != null) {
                manager.updateCustomHeaders(configId, request.headersJson)
            }
            val view = viewOf(configId)
            if (view == null) {
                return@blocking DeveloperApiResult.error<ModelConfigView>(
                    "write_failed",
                    "model config could not be read back after update"
                )
            }
            record(packageId, "modelConfig.update", "updated model config " + configId)
            DeveloperApiResult.ok(view)
        }

    override fun delete(packageId: String, configId: String): DeveloperApiResult<Unit> =
        blocking("delete") {
            if (configId == ModelConfigManager.DEFAULT_CONFIG_ID) {
                return@blocking DeveloperApiResult.error<Unit>(
                    "forbidden",
                    "the default model config cannot be deleted"
                )
            }
            if (configExists(configId) == false) {
                return@blocking DeveloperApiResult.error<Unit>(
                    "not_found",
                    "model config not found"
                )
            }
            manager.deleteConfig(configId)
            record(packageId, "modelConfig.delete", "deleted model config " + configId)
            DeveloperApiResult.ok<Unit>()
        }

    private suspend fun configExists(configId: String): Boolean =
        manager.getAllConfigSummaries().any { summary -> summary.id == configId }

    private suspend fun currentIfExists(configId: String): ModelConfigData? =
        if (configExists(configId)) manager.getModelConfig(configId) else null

    private suspend fun viewOf(configId: String): ModelConfigView? =
        manager.getModelConfig(configId)?.let { config ->
            ModelConfigView(
                id = config.id,
                name = config.name,
                providerType = config.apiProviderTypeId,
                endpoint = config.apiEndpoint,
                modelNames = getModelList(config.modelName)
            )
        }

    private fun resolveProviderType(providerTypeId: String): ApiProviderType {
        val resolved = ApiProviderType.fromProviderTypeId(providerTypeId)
        if (resolved == null) {
            AppLogger.w(TAG, "unknown providerType " + providerTypeId + ", falling back to OTHER")
            return ApiProviderType.OTHER
        }
        return resolved
    }

    private fun record(packageId: String, area: String, message: String) {
        AppLogger.i(TAG, "[" + packageId + "] " + message)
        diagnostics?.record(
            packageId,
            DiagnosticEvent(level = "info", area = area, message = message)
        )
    }

    private fun <T> blocking(
        operation: String,
        block: suspend () -> DeveloperApiResult<T>
    ): DeveloperApiResult<T> {
        return try {
            runBlocking { block() }
        } catch (error: Exception) {
            AppLogger.e(TAG, "model config " + operation + " failed", error)
            DeveloperApiResult.error<T>(
                "bridge_error",
                error.message ?: error.javaClass.simpleName
            )
        }
    }

    companion object {
        private const val TAG = "RealModelConfigBridge"
    }
}
