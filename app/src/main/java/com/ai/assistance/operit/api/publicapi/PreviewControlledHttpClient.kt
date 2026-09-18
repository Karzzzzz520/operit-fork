package com.ai.assistance.operit.api.publicapi

import java.net.HttpURLConnection
import java.net.URL

class PreviewControlledHttpClient : ControlledHttpClient {
    override fun execute(packageId: String, request: HttpRequestSpec): DeveloperApiResult<HttpResponseSpec> = runCatching {
        val connection = URL(request.url).openConnection() as HttpURLConnection
        connection.requestMethod = request.method.uppercase()
        connection.connectTimeout = request.timeoutMs.coerceIn(1000, 120000).toInt()
        connection.readTimeout = request.timeoutMs.coerceIn(1000, 120000).toInt()
        request.headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        if (request.body != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(request.body.toByteArray()) }
        }
        val body = (if (connection.responseCode >= 400) connection.errorStream else connection.inputStream)
            ?.bufferedReader()?.use { it.readText() }.orEmpty()
        DeveloperApiResult.ok(HttpResponseSpec(connection.responseCode, connection.headerFields.filterKeys { it != null }.mapValues { it.value.joinToString(",") }, body))
    }.getOrElse { DeveloperApiResult.error("http_error", it.message ?: it::class.java.simpleName) }
}
