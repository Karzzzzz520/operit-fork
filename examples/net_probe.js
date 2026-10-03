/*
METADATA
{
    "name": "net_probe",
    "display_name": { "zh": "HTTP 探针", "en": "HTTP Probe" },
    "description": { "zh": "用 OperitFork.controlledHttp 发任意 HTTP 请求（GET/POST 等），带超时，返回状态码与响应体。适合调试接口、验证网络连通性。", "en": "Send arbitrary HTTP requests via OperitFork.controlledHttp (GET/POST/...), with timeout; returns status code and body." },
    "enabledByDefault": true,
    "category": "Development",
    "tools": [
        {
            "name": "request",
            "description": { "zh": "发起一个 HTTP 请求。", "en": "Send one HTTP request." },
            "parameters": [
                { "name": "method", "description": { "zh": "HTTP 方法，如 GET/POST/PUT/DELETE", "en": "HTTP method." }, "type": "string", "required": true },
                { "name": "url", "description": { "zh": "完整 URL", "en": "Full URL." }, "type": "string", "required": true },
                { "name": "headers_json", "description": { "zh": "请求头 JSON 字符串，如 {"Content-Type":"application/json"}", "en": "Headers as a JSON string." }, "type": "string", "required": false },
                { "name": "body", "description": { "zh": "请求体字符串", "en": "Request body string." }, "type": "string", "required": false },
                { "name": "timeout_ms", "description": { "zh": "超时（毫秒，最低3000ms，默认30000）", "en": "Timeout in ms (min 3000, default 30000)." }, "type": "number", "required": false }
            ]
        }
    ]
}
*/
const netProbe = (function () {
    const PACKAGE_ID = "net_probe";
    const DEFAULT_TIMEOUT_MS = 30000;
    const MIN_TIMEOUT_MS = 3000;
    const MAX_INLINE_BODY_CHARS = 8000;

    function apiResult(r) {
        if (r == null) return null;
        return { success: r.success, value: r.value, errorCode: r.errorCode, message: r.message };
    }

    async function ensureCapabilities() {
        if (typeof OperitFork === "undefined" || typeof OperitFork.isAvailable !== "function" || !OperitFork.isAvailable()) {
            throw new Error("OperitFork 运行时不可用（本包需要 fork 版）");
        }
        return apiResult(await OperitFork.negotiate({
            packageId: PACKAGE_ID,
            apiVersion: "1.0.0",
            requestedCapabilities: ["controlled_http"],
        }));
    }

    async function request(params = {}) {
        try {
            const method = String(params.method || "GET").toUpperCase();
            const url = String(params.url || "").trim();
            if (!url) {
                throw new Error("url 不能为空");
            }
            let timeout = DEFAULT_TIMEOUT_MS;
            if (params.timeout_ms !== undefined) {
                const parsed = parseInt(params.timeout_ms, 10);
                if (!Number.isFinite(parsed) || parsed < MIN_TIMEOUT_MS) {
                    throw new Error(`timeout_ms必须是整数且不少于${MIN_TIMEOUT_MS}毫秒`);
                }
                timeout = parsed;
            }
            let headers = {};
            if (params.headers_json) {
                try {
                    headers = JSON.parse(String(params.headers_json));
                } catch (e) {
                    throw new Error("headers_json 解析失败: " + e.message);
                }
            }
            const negotiate = await ensureCapabilities();
            const spec = { method: method, url: url, headers: headers, timeoutMs: timeout };
            if (params.body !== undefined && params.body !== null && params.body !== "") {
                spec.body = String(params.body);
            }
            let res = null;
            let err = null;
            try {
                res = apiResult(await OperitFork.controlledHttp.execute(PACKAGE_ID, spec));
            } catch (e) {
                err = String(e && e.message ? e.message : e);
            }
            const v = res && res.value;
            const bodyStr = v ? String(v.body || "") : "";
            return {
                command: method + " " + url,
                ok: !!(res && res.success),
                statusCode: v ? v.statusCode : null,
                body: bodyStr.length > MAX_INLINE_BODY_CHARS ? bodyStr.slice(0, MAX_INLINE_BODY_CHARS) : bodyStr,
                bodyTruncated: bodyStr.length > MAX_INLINE_BODY_CHARS,
                error: err || (res && res.errorCode) || null,
                timeoutMsUsed: timeout,
                negotiate: negotiate,
            };
        }
        catch (error) {
            console.error(`[net_probe/request] 错误: ${error.message}`);
            throw error;
        }
    }

    return { request: request };
})();
exports.request = netProbe.request;
