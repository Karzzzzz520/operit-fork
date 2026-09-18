package com.ai.assistance.operit.api.publicapi

interface DeveloperEventBus {
    fun publish(packageId: String, event: DeveloperEvent): DeveloperApiResult<Unit>
    fun subscribe(packageId: String, eventType: String, handler: (DeveloperEvent) -> Unit): DeveloperApiResult<String>
    fun unsubscribe(packageId: String, subscriptionId: String): DeveloperApiResult<Unit>
}

data class DeveloperEvent(val type: String, val payloadJson: String = "{}", val timestampEpochMs: Long = System.currentTimeMillis(), val correlationId: String? = null)
