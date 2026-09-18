package com.ai.assistance.operit.api.publicapi

interface PluginLifecycle {
    fun onLoad(packageId: String): DeveloperApiResult<Unit>
    fun onEnable(packageId: String): DeveloperApiResult<Unit>
    fun onDisable(packageId: String): DeveloperApiResult<Unit>
    fun onUnload(packageId: String): DeveloperApiResult<Unit>
}

data class LifecycleContext(val packageId: String, val apiVersion: DeveloperApiVersion, val grantedCapabilities: Set<String>)
