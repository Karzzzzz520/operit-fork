package com.ai.assistance.operit.core.tools.packTool

import android.content.Context
import android.os.FileObserver
import com.ai.assistance.operit.core.tools.AIToolHandler
import com.ai.assistance.operit.util.AppLogger
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Built-in sandbox package hot reload.
 *
 * Watches the external packages directory and refreshes the loaded packages after a quiet
 * period. Dropping a .js/.toolpkg file into the directory then takes effect without restarting
 * the app and without the adb broadcast the developer scripts used to require.
 */
object PackageHotReloadWatcher {
    private const val TAG = "PackageHotReloadWatcher"
    private const val PACKAGES_DIR_NAME = "packages"
    private const val DEBOUNCE_MS = 1500L

    private const val WATCH_MASK =
        FileObserver.CREATE or
            FileObserver.CLOSE_WRITE or
            FileObserver.DELETE or
            FileObserver.MOVED_TO or
            FileObserver.MOVED_FROM

    private val scope = CoroutineScope(Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private var observer: FileObserver? = null
    private var pending: Job? = null

    /** Starts watching once. Safe to call from Application.onCreate. */
    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) {
            return
        }
        val appContext = context.applicationContext
        val directory = File(appContext.getExternalFilesDir(null), PACKAGES_DIR_NAME)
        if (!directory.exists() && !directory.mkdirs()) {
            AppLogger.w(TAG, "cannot create packages directory: ${directory.absolutePath}")
        }

        @Suppress("DEPRECATION")
        val watcher =
            object : FileObserver(directory.absolutePath, WATCH_MASK) {
                override fun onEvent(event: Int, path: String?) {
                    scheduleReload(appContext, path)
                }
            }
        observer = watcher
        watcher.startWatching()
        AppLogger.d(TAG, "watching ${directory.absolutePath}")
    }

    fun stop() {
        if (!started.compareAndSet(true, false)) {
            return
        }
        observer?.stopWatching()
        observer = null
        pending?.cancel()
        pending = null
    }

    private fun scheduleReload(context: Context, path: String?) {
        synchronized(this) {
            pending?.cancel()
            pending =
                scope.launch {
                    delay(DEBOUNCE_MS)
                    AppLogger.d(TAG, "package change detected: $path")
                    runCatching {
                            val handler = AIToolHandler.getInstance(context)
                            val packageManager = PackageManager.getInstance(context, handler)
                            AppLogger.d(TAG, packageManager.refreshExternalPackagesForDebug())
                        }
                        .onFailure { error -> AppLogger.e(TAG, "hot reload failed", error) }
                }
        }
    }
}
