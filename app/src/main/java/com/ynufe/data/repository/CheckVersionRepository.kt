package com.ynufe.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit
import com.ynufe.data.api.VersionApi
import com.ynufe.utils.UpdateResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CheckVersionRepository @Inject constructor(
    private val versionApi: VersionApi,
    @param:ApplicationContext private val context: Context
) {
    // 获取名为 "app_settings" 的本地存储文件，MODE_PRIVATE 表示只有你的 App 能读写它
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    // 存储时间戳的“钥匙”名称
    private val LAST_CHECK_KEY = "last_version_check_time"

    // 设置检查间隔：6小时 (6小时 * 60分钟 * 60秒 * 1000毫秒)
    private val CHECK_INTERVAL = 6 * 60 * 60 * 1000L

    fun shouldCheckUpdate(): Boolean {
        val lastCheck = prefs.getLong(LAST_CHECK_KEY, 0L)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastCheck) > CHECK_INTERVAL
    }

    fun updateLastCheckTime() {
        prefs.edit { putLong(LAST_CHECK_KEY, System.currentTimeMillis()) }
    }

    suspend fun checkUpdate(force: Boolean = false): UpdateResult {
        // 1. 检查逻辑：非强制检查时，如果距离上次检查不足 6 小时则跳过
        if (!force && !shouldCheckUpdate()) {
            return UpdateResult.NoUpdate
        }

        return try {
            // 2. 发起 API 请求
            val response = versionApi.checkVersion()

            if (response.isSuccessful) {
                val releases = response.body()

                if (!releases.isNullOrEmpty()) {
                    val latestRelease = releases[0]
                    // 查找列表中的第一个 APK 文件
                    val apkAsset = latestRelease.assets.firstOrNull { it.name.endsWith(".apk", true) }

                    // 格式化版本号（移除 V/v 前缀）
                    val latestVersion = latestRelease.name.replace(Regex("[Vv]"), "")
                    val currentVersion = getCurrentVersionName().replace(Regex("[Vv]"), "")

                    if (apkAsset == null) {
                        return UpdateResult.Error("未发现 APK 下载资源")
                    }

                    // 只有在成功获取到数据并对比前，才更新本地检查时间戳
                    updateLastCheckTime()

                    // 3. 比较版本号
                    val isNewer = isVersionNewer(latestVersion, currentVersion)

                    if (isNewer) {
                        UpdateResult.HasUpdate(
                            latestVersion = latestVersion,
                            currentVersion = currentVersion,
                            releaseNotes = latestRelease.body,
                            downloadUrl = apkAsset.browserDownloadUrl
                        )
                    } else {
                        UpdateResult.NoUpdate
                    }
                } else {
                    UpdateResult.Error("未发现发布版本")
                }
            } else {
                UpdateResult.Error("服务器响应错误: ${response.code()}")
            }
        } catch (e: Exception) {
            // 4. 捕获网络异常或解析异常
            UpdateResult.Error("网络连接失败: ${e.message}")
        }
    }

    // 获取当前 App 的 VersionName
    fun getCurrentVersionName(): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }


    //比较逻辑：支持 10.10.10 与 9.9.9 的正确比较
    private fun isVersionNewer(v1: String, v2: String): Boolean {
        // 过滤掉非数字部分，确保 toInt 不崩溃
        val levels1 = v1.split(".").mapNotNull { it.trim().toIntOrNull() }
        val levels2 = v2.split(".").mapNotNull { it.trim().toIntOrNull() }

        val maxLength = maxOf(levels1.size, levels2.size)
        for (i in 0 until maxLength) {
            // 如果某一方位数较短，缺少的位补 0
            val v1Part = levels1.getOrElse(i) { 0 }
            val v2Part = levels2.getOrElse(i) { 0 }

            if (v1Part > v2Part) return true
            if (v1Part < v2Part) return false
        }
        return false
    }
}

