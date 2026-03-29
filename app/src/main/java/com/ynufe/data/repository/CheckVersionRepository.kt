package com.ynufe.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.ynufe.data.api.ApiServices
import com.ynufe.utils.UpdateResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.core.content.edit

class CheckVersionRepository @Inject constructor(
    private val apiServices: ApiServices,
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

    suspend fun checkUpdate(): UpdateResult {
        // 首先检查时间：如果距离上次成功检测不足 6 小时，直接返回 NoUpdate
        // 这样 ViewModel 收到后不会弹窗，也不会执行后续的网络请求
        if (!shouldCheckUpdate()) {
            return UpdateResult.NoUpdate
        }

        return try {
            val response = apiServices.checkVersion()

            if (response.code() == 200) {
                val releases = response.body()
                if (!releases.isNullOrEmpty()) {
                    // 获取服务器上最新的版本 (列表第一个)
                    val latestRelease = releases[0]
                    val apkAsset =
                        latestRelease.assets.firstOrNull { it.name.endsWith(".apk", true) }

                    if (apkAsset == null) {
                        return UpdateResult.Error("未发现发布版本")
                    }

                    // 只有在成功获取到服务器数据并解析完成后，才更新本地的检查时间
                    // 这样可以确保：如果网络请求失败，下次打开 App 依然会尝试重新请求
                    updateLastCheckTime()

                    val latestVersion = latestRelease.name.replace(Regex("[Vv]"), "")

                    // 获取本地当前版本
                    val currentVersion = getCurrentVersionName().replace(Regex("[Vv]"), "")

                    // 比较版本
                    if (isVersionNewer(latestVersion, currentVersion)) {
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
                    // 虽然请求成功但列表为空，也视作成功一次，更新时间避免频繁骚扰服务器
                    updateLastCheckTime()
                    UpdateResult.Error("未发现发布版本")
                }
            } else {
                // 服务器报错（如 500），不调用 updateLastCheckTime，允许用户下次进入时重试
                UpdateResult.Error("服务器响应错误: ${response.code()}")
            }
        } catch (e: Exception) {
            // 网络超时或无网络，不更新时间，保证下次回到前台时能再次触发检测
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

