package com.ai.assistance.operit.api.publicapi

interface DeveloperDiagnostics {
    fun record(packageId: String, event: DiagnosticEvent): DeveloperApiResult<Unit>
    fun query(packageId: String?, limit: Int = 100): DeveloperApiResult<List<DiagnosticEvent>>
    fun clear(packageId: String): DeveloperApiResult<Unit>
}

data class DiagnosticEvent(val level: String, val area: String, val message: String, val detailsJson: String = "{}", val timestampEpochMs: Long = System.currentTimeMillis())
