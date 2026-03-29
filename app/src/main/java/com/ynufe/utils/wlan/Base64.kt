package com.ynufe.utils.wlan

import android.util.Base64

object Base64  {
    private const val CUSTOM_ALPHA = "LVoJPiCN2R8G90yg+hmFHuacZ1OWMnrsSTXkYpUq/3dlbfKwv6xztjI7DeBE45QA"
    private const val STANDARD_ALPHA = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    // 建立索引映射表 (0-127 ASCII)
    private val map = IntArray(128).apply {
        STANDARD_ALPHA.forEachIndexed { i, char -> this[char.code] = i }
    }

    fun encode(data: ByteArray): String {
        // 使用 Android 原生 Base64 (NO_WRAP 表示不换行)
        val standard = Base64.encodeToString(data, Base64.NO_WRAP)

        // 映射转换
        return buildString {
            for (c in standard) {
                if (c == '=') {
                    append('=')
                } else {
                    // 防止索引越界安全处理
                    val index = c.code
                    if (index < map.size) {
                        append(CUSTOM_ALPHA[map[index]])
                    }
                }
            }
        }
    }
}