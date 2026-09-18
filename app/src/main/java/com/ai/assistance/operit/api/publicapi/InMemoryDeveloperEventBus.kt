package com.ai.assistance.operit.api.publicapi

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class InMemoryDeveloperEventBus : DeveloperEventBus {
    private data class Subscription(val packageId: String, val type: String, val handler: (DeveloperEvent) -> Unit)
    private val subscriptions = ConcurrentHashMap<String, Subscription>()

    override fun publish(packageId: String, event: DeveloperEvent): DeveloperApiResult<Unit> {
        subscriptions.values.filter { it.type == event.type || it.type == "*" }
            .forEach { subscription ->
                runCatching { subscription.handler(event) }
            }
        return DeveloperApiResult.ok()
    }

    override fun subscribe(packageId: String, eventType: String, handler: (DeveloperEvent) -> Unit): DeveloperApiResult<String> {
        if (packageId.isBlank() || eventType.isBlank()) return DeveloperApiResult.error("invalid_subscription", "packageId and eventType are required")
        val id = UUID.randomUUID().toString()
        subscriptions[id] = Subscription(packageId, eventType, handler)
        return DeveloperApiResult.ok(id)
    }

    override fun unsubscribe(packageId: String, subscriptionId: String): DeveloperApiResult<Unit> {
        val subscription = subscriptions[subscriptionId]
        if (subscription != null && subscription.packageId != packageId) return DeveloperApiResult.error("forbidden", "subscription belongs to another package")
        subscriptions.remove(subscriptionId)
        return DeveloperApiResult.ok()
    }
}
