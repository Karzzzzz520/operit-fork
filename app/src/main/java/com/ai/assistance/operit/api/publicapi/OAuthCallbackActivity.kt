package com.ai.assistance.operit.api.publicapi

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * Developer-preview OAuth callback entry point.
 *
 * OAuth protocol and provider logic remain in Sandbox Package/ToolPkg. This
 * activity only converts the browser result into a broadcast that the host
 * bridge adapter can consume.
 */
class OAuthCallbackActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val callback = intent?.data
        if (callback != null) {
            sendBroadcast(Intent(ACTION_OAUTH_CALLBACK).apply {
                setPackage(packageName)
                putExtra(EXTRA_CALLBACK_URI, callback.toString())
                putExtra(EXTRA_STATE, callback.getQueryParameter("state"))
                putExtra(EXTRA_CODE, callback.getQueryParameter("code"))
                putExtra(EXTRA_ERROR, callback.getQueryParameter("error"))
            })
        }
        finish()
    }

    companion object {
        const val ACTION_OAUTH_CALLBACK = "com.ai.assistance.operit.api.OAUTH_CALLBACK"
        const val EXTRA_CALLBACK_URI = "callback_uri"
        const val EXTRA_STATE = "state"
        const val EXTRA_CODE = "code"
        const val EXTRA_ERROR = "error"
    }
}
