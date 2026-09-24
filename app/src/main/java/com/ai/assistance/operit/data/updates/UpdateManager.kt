package com.ai.assistance.operit.data.updates

import android.content.Context
import com.ai.assistance.operit.util.AppLogger
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ai.assistance.operit.R
import com.ai.assistance.operit.data.api.GitHubApiService
import com.ai.assistance.operit.data.preferences.UserPreferencesManager
import com.ai.assistance.operit.util.GithubReleaseUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

// 更新状态 - 移除下载相关状态
sealed class UpdateStatus {
    object Initial : UpdateStatus()
    object Checking : UpdateStatus()
    data class Available(
            val newVersion: String,
            val updateUrl: String,
            val releaseNotes: String,
            val downloadUrl: String = "" // 保留下载URL字段用于浏览器打开
    ) : UpdateStatus()
    data class PatchAvailable(
            val newVersion: String,
            val updateUrl: String,
            val releaseNotes: String,
            val patchUrl: String,
            val metaUrl: String
    ) : UpdateStatus()
    object UpToDate : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

/** UpdateManager - 处理应用更新的核心类 负责检查更新 */
class UpdateManager private constructor(private val context: Context) {
    private val TAG = "UpdateManager"

    // 更新状态LiveData，可从UI中观察
    private val _updateStatus = MutableLiveData<UpdateStatus>(UpdateStatus.Initial)
    val updateStatus: LiveData<UpdateStatus> = _updateStatus

    init {
        AppLogger.d(TAG, "UpdateManager initialized")
    }

    companion object {
        @Volatile private var INSTANCE: UpdateManager? = null

        fun getInstance(context: Context): UpdateManager {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance = UpdateManager(context.applicationContext)
                        INSTANCE = instance
                        instance
                    }
        }

        /**
         * 比较两个版本号
         * @return -1 如果v1 < v2, 0 如果 v1 == v2, 1 如果 v1 > v2
         */
        private data class ParsedVersion(val major: Int, val minor: Int, val patch: Int, val patchIndex: Int)

        /** Strips a leading "v" or the fork tag prefix so fork tags compare with plain versions. */
        private fun normalizeVersionTag(v: String): String =
            v.trim().removePrefix("v").removePrefix("fork-")

        private fun metadataIndex(s: String): Pair<Int, Int> {
            val plusIdx = s.indexOf('+')
            val forkIdx = s.indexOf("-f")
            val cutIdx = listOf(plusIdx, forkIdx).filter { it >= 0 }.minOrNull() ?: -1
            val index =
                when {
                    plusIdx >= 0 -> s.substring(plusIdx + 1).toIntOrNull() ?: 0
                    forkIdx >= 0 -> s.substring(forkIdx + 2).toIntOrNull() ?: 0
                    else -> 0
                }
            return Pair(cutIdx, index)
        }

        private fun baseVersionOf(v: String): String {
            val s = normalizeVersionTag(v)
            val (cutIdx, _) = metadataIndex(s)
            return if (cutIdx >= 0) s.substring(0, cutIdx) else s
        }

        private fun parseVersion(v: String): ParsedVersion {
            val s = normalizeVersionTag(v)
            val (cutIdx, patchIndex) = metadataIndex(s)
            val base = if (cutIdx >= 0) s.substring(0, cutIdx) else s

            val parts = base.split(".")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0

            return ParsedVersion(major = major, minor = minor, patch = patch, patchIndex = patchIndex)
        }

        fun compareVersions(v1: String, v2: String): Int {
            val p1 = parseVersion(v1)
            val p2 = parseVersion(v2)

            if (p1.major != p2.major) return p1.major.compareTo(p2.major)
            if (p1.minor != p2.minor) return p1.minor.compareTo(p2.minor)
            if (p1.patch != p2.patch) return p1.patch.compareTo(p2.patch)
            return p1.patchIndex.compareTo(p2.patchIndex)
        }

        /** 检查更新，返回更新状态 用于从MainActivity直接检查更新 */
        suspend fun checkForUpdates(context: Context, currentVersion: String): UpdateStatus {
            val manager = getInstance(context)
            return manager.checkForUpdatesInternal(currentVersion)
        }
    }

    suspend fun checkForUpdatesSilently(currentVersion: String) {
        AppLogger.d(TAG, "checkForUpdatesSilently() start: currentVersion=$currentVersion")
        try {
            val result = checkForUpdatesInternal(currentVersion)
            AppLogger.d(TAG, "checkForUpdatesSilently() done: status=${result::class.java.simpleName}")
            if (result is UpdateStatus.Available || result is UpdateStatus.PatchAvailable) {
                _updateStatus.postValue(result)
            }
        } catch (e: Exception) {
            AppLogger.w(TAG, "checkForUpdatesSilently() failed", e)
        }
    }

    /** 开始更新检查流程 */
    suspend fun checkForUpdates(currentVersion: String) {
        AppLogger.d(TAG, "checkForUpdates() start: currentVersion=$currentVersion")
        _updateStatus.postValue(UpdateStatus.Checking)

        try {
            val result = checkForUpdatesInternal(currentVersion)
            AppLogger.d(TAG, "checkForUpdates() done: status=${result::class.java.simpleName}")
            _updateStatus.postValue(result)
        } catch (e: Exception) {
            AppLogger.e(TAG, "Update check failed", e)
            _updateStatus.postValue(UpdateStatus.Error(context.getString(R.string.update_check_failed, e.message)))
        }
    }

    /** 检查更新的内部实现 */
    private suspend fun checkForUpdatesInternal(currentVersion: String): UpdateStatus {
        return withContext(Dispatchers.IO) {
            try {
                AppLogger.d(TAG, "checkForUpdatesInternal(): currentVersion=$currentVersion")

                // Patch updates belong to the upstream nightly channel. The fork ships full
                // releases only, so there is no patch candidate.
                val patchUpdate: UpdateStatus? = null

                // 从字符串资源中获取GitHub仓库信息
                val aboutWebsite = context.getString(R.string.about_website)

                // 解析GitHub仓库链接 - 处理HTML格式
                val htmlContent = aboutWebsite.replace("&lt;", "<").replace("&gt;", ">")
                val githubUrlPattern = "https://github.com/([^/\"<>]+)/([^/\"<>]+)".toRegex()
                val matchResult = githubUrlPattern.find(htmlContent)

                val (repoOwner, repoName) =
                        if (matchResult != null) {
                            Pair(matchResult.groupValues[1], matchResult.groupValues[2])
                        } else {
                            Pair("Karzzzzz520", "operit-fork") // 默认值
                        }

                val githubReleaseUtil = GithubReleaseUtil(context)
                val releaseInfo = githubReleaseUtil.fetchLatestReleaseInfo(repoOwner, repoName)

                AppLogger.d(
                    TAG,
                    "normal release check: repo=$repoOwner/$repoName releaseInfo=${releaseInfo?.version}"
                )

                if (releaseInfo != null) {
                    val normalUpdate: UpdateStatus =
                        if (compareVersions(releaseInfo.version, currentVersion) > 0) {
                            UpdateStatus.Available(
                            newVersion = releaseInfo.version,
                            updateUrl = releaseInfo.releasePageUrl,
                            releaseNotes = releaseInfo.releaseNotes,
                            downloadUrl = releaseInfo.downloadUrl
                            )
                        } else {
                            UpdateStatus.UpToDate
                        }

                    val patch = patchUpdate as? UpdateStatus.PatchAvailable
                    val normal = normalUpdate as? UpdateStatus.Available
                    if (patch != null && normal != null) {
                        val finalStatus = if (compareVersions(normal.newVersion, patch.newVersion) >= 0) {
                            normalUpdate
                        } else {
                            patchUpdate
                        }

                        return@withContext finalStatus
                    }

                    val finalStatus = patchUpdate ?: normalUpdate
                    finalStatus
                } else {
                    val finalStatus = patchUpdate ?: UpdateStatus.Error(context.getString(R.string.update_cannot_fetch_info))
                    finalStatus
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error checking for updates", e)
                return@withContext UpdateStatus.Error(context.getString(R.string.update_check_failed, e.message))
            }
        }
    }

}
