package com.ai.assistance.operit.api.publicapi

import java.util.concurrent.CopyOnWriteArrayList

class InMemoryDeveloperDiagnostics(private val maxEntries: Int = 1000) : DeveloperDiagnostics {
    private val events = CopyOnWriteArrayList<Pair<String, DiagnosticEvent>>()

    override fun record(packageId: String, event: DiagnosticEvent): DeveloperApiResult<Unit> {
        events.add(packageId to event)
        while (events.size > maxEntries) events.removeAt(0)
        return DeveloperApiResult.ok()
    }

    override fun query(packageId: String?, limit: Int): DeveloperApiResult<List<DiagnosticEvent>> {
        val result = events.asReversed().filter { packageId == null || it.first == packageId }.take(limit.coerceAtLeast(0)).map { it.second }
        return DeveloperApiResult.ok(result)
    }

    override fun clear(packageId: String): DeveloperApiResult<Unit> {
        events.removeIf { it.first == packageId }
        return DeveloperApiResult.ok()
    }
}
