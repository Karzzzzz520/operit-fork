package com.ai.assistance.operit.api.publicapi

import android.content.Context
import com.ai.assistance.operit.core.tools.AIToolHandler
import com.ai.assistance.operit.core.tools.packTool.PackageManager
import com.ai.assistance.operit.data.model.AITool
import com.ai.assistance.operit.data.model.ToolParameter
import com.ai.assistance.operit.data.updates.UpdateManager
import com.ai.assistance.operit.data.updates.UpdateStatus
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * JSON dispatcher backing the OperitFork JavaScript namespace.
 *
 * The JS bridge (JsForkApiBridge.kt) calls NativeInterface.operitForkInvoke(requestJson) with
 * {"method":"...","params":{...}} and receives a DeveloperApiResult-shaped JSON object:
 * {"success":bool,"value":...,"errorCode":...,"message":...}.
 *
 * Every capability routes to the public contracts declared in this package. The implementations
 * are the developer-preview ones: no UI confirmation and no production policy enforcement.
 */
class ForkApiBridge(context: Context) {
    private val appContext: Context = context.applicationContext
    private val permissions: PluginPermissionStore = InMemoryPluginPermissionStore()
    private val registry: DeveloperApiRegistry = DefaultDeveloperApiRegistry(permissions)
    private val tokenStore: SecureTokenStore = PreviewTokenStore(appContext)
    private val httpClient: ControlledHttpClient = PreviewControlledHttpClient()
    private val eventBus: DeveloperEventBus = InMemoryDeveloperEventBus()
    private val diagnostics: DeveloperDiagnostics = InMemoryDeveloperDiagnostics()
    private val oauth: OAuthBridge = InMemoryOAuthBridge()
    private val modelConfigs: ModelConfigBridge = InMemoryModelConfigBridge()
    private val aiProviders: AiProviderBridge = InMemoryAiProviderBridge()
    private val lifecycle: PluginLifecycle = NoopPluginLifecycle()
    private val aiToolHandler: AIToolHandler by lazy { AIToolHandler.getInstance(appContext) }
    private val packageManager: PackageManager by lazy {
        PackageManager.getInstance(appContext, aiToolHandler)
    }

    fun invoke(requestJson: String): String {
        return try {
            val request = JSONObject(requestJson)
            val method = request.optString("method", "").trim()
            if (method.isEmpty()) {
                return errorResult("invalid_request", "method is required").toString()
            }
            val params = request.optJSONObject("params") ?: JSONObject()
            dispatch(method, params).toString()
        } catch (error: Exception) {
            errorResult("bridge_error", error.message ?: error.javaClass.simpleName).toString()
        }
    }

    private fun dispatch(method: String, params: JSONObject): JSONObject {
        return when (method) {
            "isAvailable" -> okResult(true)
            "getHostInfo" -> okResult(hostInfo())
            "listCapabilities" -> okResult(capabilityNames())
            "negotiate" -> negotiate(params)
            "isCapabilityGranted" ->
                okResult(
                    registry.isCapabilityGranted(
                        params.optString("packageId", ""),
                        params.optString("capability", "")
                    )
                )
            "secureTokenStore.put" ->
                resultJson(
                    tokenStore.put(
                        params.optString("packageId", ""),
                        params.optString("accountId", ""),
                        params.optString("tokenJson", "")
                    )
                )
            "secureTokenStore.get" ->
                resultJson(
                    tokenStore.get(params.optString("packageId", ""), params.optString("accountId", ""))
                )
            "secureTokenStore.delete" ->
                resultJson(
                    tokenStore.delete(params.optString("packageId", ""), params.optString("accountId", ""))
                )
            "secureTokenStore.listAccounts" ->
                resultJson(tokenStore.listAccounts(params.optString("packageId", "")))
            "secureTokenStore.status" ->
                resultJson(
                    tokenStore.status(params.optString("packageId", ""), params.optString("accountId", ""))
                )
            "oauthCallback.begin" -> resultJson(oauth.begin(readOAuthBeginRequest(params.optJSONObject("request"))))
            "oauthCallback.consumeCallback" ->
                resultJson(
                    oauth.consumeCallback(
                        params.optString("packageId", ""),
                        params.optString("state", ""),
                        params.optString("code", "").ifEmpty { null },
                        params.optString("error", "").ifEmpty { null }
                    )
                )
            "oauthCallback.cancel" ->
                resultJson(
                    oauth.cancel(params.optString("packageId", ""), params.optString("sessionId", ""))
                )
            "modelConfig.list" -> resultJson(modelConfigs.list(params.optString("packageId", "")))
            "modelConfig.create" -> resultJson(modelConfigs.create(params.optString("packageId", ""), readModelConfigCreate(params.optJSONObject("request"))))
            "modelConfig.update" -> resultJson(modelConfigs.update(params.optString("packageId", ""), params.optString("configId", ""), readModelConfigUpdate(params.optJSONObject("request"))))
            "modelConfig.delete" -> resultJson(modelConfigs.delete(params.optString("packageId", ""), params.optString("configId", "")))
            "aiProvider.register" -> resultJson(aiProviders.register(params.optString("packageId", ""), readAiProviderDescriptor(params.optJSONObject("descriptor"))))
            "aiProvider.unregister" -> resultJson(aiProviders.unregister(params.optString("packageId", ""), params.optString("providerId", "")))
            "aiProvider.list" -> resultJson(aiProviders.list(params.optString("packageId", "").ifEmpty { null }))
            "aiProvider.execute" -> resultJson(aiProviders.execute(readAiProviderRequest(params.optJSONObject("request"))))
            "controlledHttp.execute" -> resultJson(httpClient.execute(params.optString("packageId", ""), readHttpRequest(params.optJSONObject("request"))))
            "eventBus.publish" -> resultJson(eventBus.publish(params.optString("packageId", ""), readDeveloperEvent(params.optJSONObject("event"))))
            "lifecycleHook.onLoad" -> resultJson(lifecycle.onLoad(params.optString("packageId", "")))
            "lifecycleHook.onEnable" -> resultJson(lifecycle.onEnable(params.optString("packageId", "")))
            "lifecycleHook.onDisable" -> resultJson(lifecycle.onDisable(params.optString("packageId", "")))
            "lifecycleHook.onUnload" -> resultJson(lifecycle.onUnload(params.optString("packageId", "")))
            "developerDiagnostics.record" -> resultJson(diagnostics.record(params.optString("packageId", ""), readDiagnosticEvent(params.optJSONObject("event"))))
            "developerDiagnostics.query" ->
                resultJson(
                    diagnostics.query(
                        params.optString("packageId", "").ifEmpty { null },
                        if (params.has("limit")) params.optInt("limit", 100) else 100
                    )
                )
            "developerDiagnostics.clear" -> resultJson(diagnostics.clear(params.optString("packageId", "")))
            "tools.invoke" -> okResult(invokeHostTool(params))
            "packages.list" -> okResult(packageManager.getAvailablePackages().keys.sorted())
            "packages.enable" -> okResult(packageManager.enablePackage(params.optString("packageName", "")))
            "packages.disable" -> okResult(packageManager.disablePackage(params.optString("packageName", "")))
            "packages.isEnabled" -> okResult(packageManager.isPackageEnabled(params.optString("packageName", "")))
            "packages.reload" -> okResult(packageManager.refreshExternalPackagesForDebug())
            "host.appInfo" -> okResult(appInfo())
            "update.check" -> okResult(updateCheck(params))
            else -> errorResult("unknown_method", "unsupported OperitFork method: " + method)
        }
    }

    private fun negotiate(params: JSONObject): JSONObject {
        val requested = params.optJSONArray("requestedCapabilities")
        val capabilities = LinkedHashSet<String>()
        if (requested != null) {
            for (index in 0 until requested.length()) {
                capabilities.add(requested.optString(index))
            }
        }
        val manifest =
            DeveloperApiManifest(
                packageId = params.optString("packageId", ""),
                apiVersion = parseApiVersion(params.opt("apiVersion")),
                requestedCapabilities = capabilities,
                displayName = params.optString("displayName", "").ifEmpty { null }
            )
        return resultJson(registry.negotiate(manifest))
    }

    private fun hostInfo(): JSONObject =
        JSONObject()
            .put("host", "operit-fork")
            .put("apiVersion", SUPPORTED_API_VERSION)
            .put("platform", "android")
            .put("runtime", "quickjs")
            .put("injected", true)
            .put("applicationId", appContext.packageName)

    private fun capabilityNames(): List<String> = OPERIT_FORK_CAPABILITIES

    private fun parseApiVersion(value: Any?): DeveloperApiVersion {
        if (value is JSONObject) {
            return DeveloperApiVersion(
                major = value.optInt("major", 1),
                minor = value.optInt("minor", 0),
                patch = value.optInt("patch", 0)
            )
        }
        val text = value?.toString()?.trim().orEmpty()
        val parts = text.split(".")
        fun segment(index: Int, fallback: Int): Int = parts.getOrNull(index)?.toIntOrNull() ?: fallback
        return DeveloperApiVersion(segment(0, 1), segment(1, 0), segment(2, 0))
    }

    private fun readOAuthBeginRequest(source: JSONObject?): OAuthBeginRequest {
        val json = source ?: JSONObject()
        return OAuthBeginRequest(
            packageId = json.optString("packageId", ""),
            provider = json.optString("provider", ""),
            authorizationUrl = json.optString("authorizationUrl", ""),
            redirectUri = json.optString("redirectUri", ""),
            codeVerifier = json.optString("codeVerifier", ""),
            state = json.optString("state", "")
        )
    }

    private fun readModelConfigCreate(source: JSONObject?): ModelConfigCreateRequest {
        val json = source ?: JSONObject()
        return ModelConfigCreateRequest(
            name = json.optString("name", ""),
            providerType = json.optString("providerType", ""),
            endpoint = json.optString("endpoint", ""),
            modelNames = readStringList(json.optJSONArray("modelNames")),
            accountId = json.optString("accountId", "").ifEmpty { null },
            headersJson = json.optString("headersJson", "{}")
        )
    }

    private fun readModelConfigUpdate(source: JSONObject?): ModelConfigUpdateRequest {
        val json = source ?: JSONObject()
        return ModelConfigUpdateRequest(
            name = json.optString("name", "").ifEmpty { null },
            endpoint = json.optString("endpoint", "").ifEmpty { null },
            modelNames = if (json.has("modelNames")) readStringList(json.optJSONArray("modelNames")) else null,
            accountId = json.optString("accountId", "").ifEmpty { null },
            headersJson = json.optString("headersJson", "").ifEmpty { null }
        )
    }

    private fun readAiProviderDescriptor(source: JSONObject?): AiProviderDescriptor {
        val json = source ?: JSONObject()
        return AiProviderDescriptor(
            id = json.optString("id", ""),
            displayName = json.optString("displayName", ""),
            capabilities = readStringList(json.optJSONArray("capabilities")).toSet(),
            accountId = json.optString("accountId", "").ifEmpty { null }
        )
    }

    private fun readAiProviderRequest(source: JSONObject?): AiProviderRequest {
        val json = source ?: JSONObject()
        return AiProviderRequest(
            packageId = json.optString("packageId", ""),
            providerId = json.optString("providerId", ""),
            accountId = json.optString("accountId", "").ifEmpty { null },
            model = json.optString("model", ""),
            messagesJson = json.optString("messagesJson", "[]"),
            parametersJson = json.optString("parametersJson", "{}")
        )
    }

    private fun readHttpRequest(source: JSONObject?): HttpRequestSpec {
        val json = source ?: JSONObject()
        return HttpRequestSpec(
            method = json.optString("method", "GET"),
            url = json.optString("url", ""),
            headers = readStringMap(json.optJSONObject("headers")),
            body = if (json.has("body") && !json.isNull("body")) json.optString("body", "") else null,
            timeoutMs = if (json.has("timeoutMs")) json.optLong("timeoutMs", 30000L) else 30000L,
            tokenAccountId = json.optString("tokenAccountId", "").ifEmpty { null }
        )
    }

    private fun readDeveloperEvent(source: JSONObject?): DeveloperEvent {
        val json = source ?: JSONObject()
        return DeveloperEvent(
            type = json.optString("type", ""),
            payloadJson = json.optString("payloadJson", "{}"),
            correlationId = json.optString("correlationId", "").ifEmpty { null }
        )
    }

    private fun readDiagnosticEvent(source: JSONObject?): DiagnosticEvent {
        val json = source ?: JSONObject()
        return DiagnosticEvent(
            level = json.optString("level", "info"),
            area = json.optString("area", ""),
            message = json.optString("message", ""),
            detailsJson = json.optString("detailsJson", "{}")
        )
    }

    private fun readStringList(source: JSONArray?): List<String> {
        if (source == null) {
            return emptyList()
        }
        val output = ArrayList<String>(source.length())
        for (index in 0 until source.length()) {
            output.add(source.optString(index))
        }
        return output
    }

    private fun readStringMap(source: JSONObject?): Map<String, String> {
        if (source == null) {
            return emptyMap()
        }
        val output = LinkedHashMap<String, String>()
        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            output[key] = source.optString(key)
        }
        return output
    }

    private fun resultJson(result: DeveloperApiResult<*>): JSONObject =
        if (result.success) {
            okResult(jsonValue(result.value))
        } else {
            errorResult(result.errorCode ?: "error", result.message.orEmpty())
        }

    private fun okResult(value: Any?): JSONObject {
        val json = JSONObject()
        json.put("success", true)
        json.put("value", jsonValue(value))
        return json
    }

    private fun errorResult(code: String, message: String): JSONObject {
        val json = JSONObject()
        json.put("success", false)
        json.put("errorCode", code)
        json.put("message", message)
        return json
    }

    private fun jsonValue(value: Any?): Any {
        return when (value) {
            null -> JSONObject.NULL
            is Unit -> JSONObject.NULL
            is JSONObject, is JSONArray, is String, is Number, is Boolean -> value
            is TokenStatus ->
                JSONObject()
                    .put("exists", value.exists)
                    .put("expiresAtEpochMs", value.expiresAtEpochMs ?: JSONObject.NULL)
                    .put("refreshable", value.refreshable)
                    .put("provider", value.provider ?: JSONObject.NULL)
            is OAuthSession ->
                JSONObject()
                    .put("sessionId", value.sessionId)
                    .put("provider", value.provider)
                    .put("state", value.state)
                    .put("redirectUri", value.redirectUri)
            is OAuthCallback ->
                JSONObject()
                    .put("provider", value.provider)
                    .put("code", value.code ?: JSONObject.NULL)
                    .put("error", value.error ?: JSONObject.NULL)
                    .put("stateVerified", value.stateVerified)
            is ModelConfigView ->
                JSONObject()
                    .put("id", value.id)
                    .put("name", value.name)
                    .put("providerType", value.providerType)
                    .put("endpoint", value.endpoint)
                    .put("modelNames", JSONArray(value.modelNames))
                    .put("accountId", value.accountId ?: JSONObject.NULL)
            is AiProviderDescriptor ->
                JSONObject()
                    .put("id", value.id)
                    .put("displayName", value.displayName)
                    .put("capabilities", JSONArray(value.capabilities.toList()))
                    .put("accountId", value.accountId ?: JSONObject.NULL)
            is AiProviderResponse ->
                JSONObject()
                    .put("content", value.content)
                    .put("rawJson", value.rawJson ?: JSONObject.NULL)
                    .put("usageJson", value.usageJson ?: JSONObject.NULL)
            is HttpResponseSpec -> {
                val headersJson = JSONObject()
                value.headers.forEach { (key, item) -> headersJson.put(key, item) }
                JSONObject()
                    .put("statusCode", value.statusCode)
                    .put("headers", headersJson)
                    .put("body", value.body)
                    .put("requestId", value.requestId ?: JSONObject.NULL)
            }
            is DiagnosticEvent ->
                JSONObject()
                    .put("level", value.level)
                    .put("area", value.area)
                    .put("message", value.message)
                    .put("detailsJson", value.detailsJson)
                    .put("timestampEpochMs", value.timestampEpochMs)
            is Map<*, *> -> {
                val json = JSONObject()
                value.forEach { (key, item) -> json.put(key?.toString() ?: "null", jsonValue(item)) }
                json
            }
            is Collection<*> -> {
                val json = JSONArray()
                value.forEach { item -> json.put(jsonValue(item)) }
                json
            }
            else -> value.toString()
        }
    }

    /** Executes a host tool through AIToolHandler and returns its structured result. */
    private fun invokeHostTool(params: JSONObject): JSONObject {
        val toolName = params.optString("toolName", "").trim()
        if (toolName.isEmpty()) {
            return errorResult("invalid_request", "toolName is required")
        }
        val arguments = params.optJSONObject("params")
        val toolParameters = ArrayList<ToolParameter>()
        if (arguments != null) {
            val keys = arguments.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = arguments.opt(key)
                toolParameters.add(
                    ToolParameter(
                        key,
                        if (value == null || value == JSONObject.NULL) "" else value.toString()
                    )
                )
            }
        }
        val result = aiToolHandler.executeTool(AITool(name = toolName, parameters = toolParameters))
        return JSONObject()
            .put("success", result.success)
            .put("toolName", result.toolName)
            .put("result", result.result.toString())
            .put("error", result.error ?: JSONObject.NULL)
    }

    /** Application identity and version, as shown on the About page. */
    private fun appInfo(): JSONObject {
        val json = JSONObject()
        json.put("packageName", appContext.packageName)
        return try {
            val info = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            json.put("versionName", info.versionName ?: "")
            json.put(
                "versionCode",
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    info.versionCode.toLong()
                }
            )
            json
        } catch (error: Exception) {
            json.put("versionName", "")
            json.put("versionCode", 0L)
            json
        }
    }

    /** Checks the fork release channel for a newer version. */
    private fun updateCheck(params: JSONObject): JSONObject {
        val requestedVersion = params.optString("currentVersion", "")
        val currentVersion =
            requestedVersion.ifEmpty {
                runCatching {
                        appContext.packageManager
                            .getPackageInfo(appContext.packageName, 0)
                            .versionName
                    }
                    .getOrNull()
                    .orEmpty()
            }
        return try {
            val status = runBlocking { UpdateManager.checkForUpdates(appContext, currentVersion) }
            when (status) {
                is UpdateStatus.Available ->
                    JSONObject()
                        .put("status", "available")
                        .put("newVersion", status.newVersion)
                        .put("updateUrl", status.updateUrl)
                        .put("downloadUrl", status.downloadUrl)
                is UpdateStatus.PatchAvailable ->
                    JSONObject()
                        .put("status", "available")
                        .put("newVersion", status.newVersion)
                        .put("updateUrl", status.updateUrl)
                        .put("downloadUrl", "")
                is UpdateStatus.UpToDate -> JSONObject().put("status", "up_to_date")
                is UpdateStatus.Checking -> JSONObject().put("status", "checking")
                is UpdateStatus.Error ->
                    JSONObject().put("status", "error").put("message", status.message)
                else -> JSONObject().put("status", "unknown")
            }
        } catch (error: Exception) {
            JSONObject()
                .put("status", "error")
                .put("message", error.message ?: error.javaClass.simpleName)
        }
    }

    companion object {
        private const val SUPPORTED_API_VERSION = "1.0.0"
        private val OPERIT_FORK_CAPABILITIES = listOf(
            OperitDeveloperCapability.SECURE_TOKEN_STORE,
            OperitDeveloperCapability.OAUTH_CALLBACK,
            OperitDeveloperCapability.MODEL_CONFIG,
            OperitDeveloperCapability.AI_PROVIDER,
            OperitDeveloperCapability.CONTROLLED_HTTP,
            OperitDeveloperCapability.EVENT_BUS,
            OperitDeveloperCapability.LIFECYCLE_HOOK,
            OperitDeveloperCapability.DEVELOPER_DIAGNOSTICS
        )
    }
}
