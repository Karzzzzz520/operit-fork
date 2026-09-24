/**
 * Fork 专用开发者 API 类型声明（开发者预览）。
 *
 * 对应 Kotlin 契约：app/src/main/java/com/ai/assistance/operit/api/publicapi/
 *
 * 实现：api/publicapi/ForkApiBridge.kt + quickjs/init/fork-api-bridge.js，
 * 经 NativeInterface.operitForkInvoke 调度。
 *
 * 该命名空间已注入 JS 运行时（JsLibraries 的 fork-api-bridge 模块，globals = ["OperitFork"]）。
 * 仍建议在包内做特性探测（typeof OperitFork !== 'undefined'），以便在旧宿主上优雅降级。
 *
 * @since Fork API 1.0.0 (developer preview)
 */

declare global {
    namespace OperitFork {
        /** 稳定能力标识，对应 Kotlin OperitDeveloperCapability */
        type Capability =
            | 'secure_token_store'
            | 'oauth_callback'
            | 'model_config'
            | 'ai_provider'
            | 'controlled_http'
            | 'event_bus'
            | 'lifecycle_hook'
            | 'developer_diagnostics';

        type JsonPrimitive = string | number | boolean | null;
        type JsonValue = JsonPrimitive | JsonValue[] | { [key: string]: JsonValue };

        /** 对应 Kotlin DeveloperApiResult<T> */
        interface ApiResult<T = unknown> {
            success: boolean;
            value?: T;
            errorCode?: string;
            message?: string;
        }

        /** 对应 Kotlin DeveloperApiVersion */
        interface ApiVersion {
            major: number;
            minor: number;
            patch: number;
        }

        /** 对应 Kotlin DeveloperApiManifest */
        interface ApiManifest {
            packageId: string;
            /** 语义化版本，如 '1.0.0'；也可传 ApiVersion 对象 */
            apiVersion: string | ApiVersion;
            requestedCapabilities: Capability[];
            displayName?: string;
        }

        /** 对应 Kotlin TokenStatus */
        interface TokenStatus {
            exists: boolean;
            expiresAtEpochMs?: number;
            refreshable: boolean;
            provider?: string;
        }

        /** 对应 Kotlin HttpRequestSpec */
        interface HttpRequestSpec {
            method: string;
            url: string;
            headers?: Record<string, string>;
            body?: string;
            timeoutMs?: number;
            tokenAccountId?: string;
        }

        /** 对应 Kotlin HttpResponseSpec */
        interface HttpResponseSpec {
            statusCode: number;
            headers: Record<string, string>;
            body: string;
            requestId?: string;
        }

        /** 对应 Kotlin OAuthBeginRequest */
        interface OAuthBeginRequest {
            packageId: string;
            provider: string;
            authorizationUrl: string;
            redirectUri: string;
            codeVerifier: string;
            state: string;
        }

        interface OAuthSession {
            sessionId: string;
            provider: string;
            state: string;
            redirectUri: string;
        }

        interface OAuthCallback {
            provider: string;
            code?: string;
            error?: string;
            stateVerified: boolean;
        }

        interface ModelConfigView {
            id: string;
            name: string;
            providerType: string;
            endpoint: string;
            modelNames: string[];
            accountId?: string;
        }

        interface ModelConfigCreateRequest {
            name: string;
            providerType: string;
            endpoint: string;
            modelNames: string[];
            accountId?: string;
            headersJson?: string;
        }

        interface ModelConfigUpdateRequest {
            name?: string;
            endpoint?: string;
            modelNames?: string[];
            accountId?: string;
            headersJson?: string;
        }

        interface AiProviderDescriptor {
            id: string;
            displayName: string;
            capabilities?: string[];
            accountId?: string;
        }

        interface AiProviderRequest {
            packageId: string;
            providerId: string;
            accountId?: string;
            model: string;
            messagesJson: string;
            parametersJson?: string;
        }

        interface AiProviderResponse {
            content: string;
            rawJson?: string;
            usageJson?: string;
        }

        interface DeveloperEvent {
            type: string;
            payloadJson?: string;
            timestampEpochMs?: number;
            correlationId?: string;
        }

        interface DiagnosticEvent {
            level: string;
            area: string;
            message: string;
            detailsJson?: string;
            timestampEpochMs?: number;
        }

        interface LifecycleContext {
            packageId: string;
            apiVersion: ApiVersion;
            grantedCapabilities: Capability[];
        }

        /** 对应 Kotlin SecureTokenStore */
        interface SecureTokenStoreApi {
            put(packageId: string, accountId: string, tokenJson: string): Promise<ApiResult<void>>;
            get(packageId: string, accountId: string): Promise<ApiResult<string>>;
            delete(packageId: string, accountId: string): Promise<ApiResult<void>>;
            listAccounts(packageId: string): Promise<ApiResult<string[]>>;
            status(packageId: string, accountId: string): Promise<ApiResult<TokenStatus>>;
        }

        /** 对应 Kotlin OAuthBridge */
        interface OAuthApi {
            begin(request: OAuthBeginRequest): Promise<ApiResult<OAuthSession>>;
            consumeCallback(packageId: string, state: string, code?: string, error?: string): Promise<ApiResult<OAuthCallback>>;
            cancel(packageId: string, sessionId: string): Promise<ApiResult<void>>;
        }

        /** 对应 Kotlin ModelConfigBridge */
        interface ModelConfigApi {
            list(packageId: string): Promise<ApiResult<ModelConfigView[]>>;
            create(packageId: string, request: ModelConfigCreateRequest): Promise<ApiResult<ModelConfigView>>;
            update(packageId: string, configId: string, request: ModelConfigUpdateRequest): Promise<ApiResult<ModelConfigView>>;
            delete(packageId: string, configId: string): Promise<ApiResult<void>>;
        }

        /** 对应 Kotlin AiProviderBridge */
        interface AiProviderApi {
            register(packageId: string, descriptor: AiProviderDescriptor): Promise<ApiResult<void>>;
            unregister(packageId: string, providerId: string): Promise<ApiResult<void>>;
            list(packageId?: string): Promise<ApiResult<AiProviderDescriptor[]>>;
            execute(request: AiProviderRequest): Promise<ApiResult<AiProviderResponse>>;
        }

        /** 对应 Kotlin ControlledHttpClient */
        interface ControlledHttpApi {
            execute(packageId: string, request: HttpRequestSpec): Promise<ApiResult<HttpResponseSpec>>;
        }

        /** 对应 Kotlin DeveloperEventBus */
        interface EventBusApi {
            publish(packageId: string, event: DeveloperEvent): Promise<ApiResult<void>>;
            subscribe(packageId: string, eventType: string, handler: (event: DeveloperEvent) => void): Promise<ApiResult<string>>;
            unsubscribe(packageId: string, subscriptionId: string): Promise<ApiResult<void>>;
        }

        /** 对应 Kotlin PluginLifecycle */
        interface LifecycleApi {
            onLoad(packageId: string): Promise<ApiResult<void>>;
            onEnable(packageId: string): Promise<ApiResult<void>>;
            onDisable(packageId: string): Promise<ApiResult<void>>;
            onUnload(packageId: string): Promise<ApiResult<void>>;
        }

        /** 对应 Kotlin DeveloperDiagnostics */
        interface DiagnosticsApi {
            record(packageId: string, event: DiagnosticEvent): Promise<ApiResult<void>>;
            query(packageId?: string, limit?: number): Promise<ApiResult<DiagnosticEvent[]>>;
            clear(packageId: string): Promise<ApiResult<void>>;
        }

        /** 宿主应用信息 */
        interface AppInfo {
            packageName: string;
            versionName: string;
            versionCode: number;
        }

        /** 宿主工具调用结果 */
        interface ToolInvokeResult {
            success: boolean;
            toolName: string;
            result: string;
            error?: string | null;
        }

        /** 更新检查结果 */
        interface UpdateCheckResult {
            status: 'available' | 'up_to_date' | 'checking' | 'error' | 'unknown';
            newVersion?: string;
            updateUrl?: string;
            downloadUrl?: string;
            message?: string;
        }

        /** 调用宿主内置工具，对应 AIToolHandler */
        interface ToolsApi {
            invoke(toolName: string, params?: Record<string, string | number | boolean>): Promise<ApiResult<ToolInvokeResult>>;
        }

        /** 沙盒包管理，对应 PackageManager */
        interface PackagesApi {
            list(): Promise<ApiResult<string[]>>;
            enable(packageName: string): Promise<ApiResult<string>>;
            disable(packageName: string): Promise<ApiResult<string>>;
            isEnabled(packageName: string): Promise<ApiResult<boolean>>;
            /** 重新扫描外部包目录并重载已启用的包（热重载） */
            reload(): Promise<ApiResult<string>>;
        }

        /** 宿主应用信息接口 */
        interface HostApi {
            getAppInfo(): Promise<ApiResult<AppInfo>>;
        }

        /** Fork 更新渠道检查接口 */
        interface UpdateApi {
            check(currentVersion?: string): Promise<ApiResult<UpdateCheckResult>>;
        }

        /** 宿主信息，对应 ForkApiBridge.hostInfo() */
        interface HostInfo {
            host: string;
            apiVersion: string;
            platform: string;
            runtime: string;
            injected: boolean;
            applicationId: string;
        }

        /** 能力协商，对应 Kotlin DeveloperApiRegistry.negotiate */
        function negotiate(manifest: ApiManifest): Promise<ApiResult<Capability[]>>;

        /** 列出宿主支持的全部能力标识 */
        function listCapabilities(): Promise<ApiResult<Capability[]>>;

        /** 读取宿主信息（版本、平台、是否已注入） */
        function getHostInfo(): Promise<ApiResult<HostInfo>>;

        /** 查询能力是否已授权，对应 Kotlin DeveloperApiRegistry.isCapabilityGranted */
        function isCapabilityGranted(packageId: string, capability: Capability): boolean;

        /** 探测 Fork 运行时是否已注入；已注入时返回 true */
        function isAvailable(): boolean;

        /** 能力命名空间（对应 8 个 capability） */
        const secureTokenStore: SecureTokenStoreApi;
        const oauthCallback: OAuthApi;
        const modelConfig: ModelConfigApi;
        const aiProvider: AiProviderApi;
        const controlledHttp: ControlledHttpApi;
        const eventBus: EventBusApi;
        const lifecycleHook: LifecycleApi;
        const developerDiagnostics: DiagnosticsApi;

        /** 扩展开放的宿主接口 */
        const tools: ToolsApi;
        const packages: PackagesApi;
        const host: HostApi;
        const update: UpdateApi;
    }
}

export {};
