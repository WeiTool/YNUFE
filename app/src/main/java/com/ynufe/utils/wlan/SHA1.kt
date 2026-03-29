package com.ynufe.utils.wlan

import java.security.MessageDigest

/**
 * SHA-1 加密扩展函数
 * @return 40位小写十六进制字符串
 */
fun String.sha1(): String {
    return try {
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest(this.toByteArray(Charsets.UTF_8))
        // 将字节数组转换为 16 进制字符串
        bytes.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        ""
    }
}