package com.ynufe.utils.wlan

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * HMAC-MD5 加密扩展函数
 * @return 32位小写十六进制字符串
 */
fun String.hmacMd5(token: String): String {
    return try {
        val mac = Mac.getInstance("HmacMD5")
        val secretKey = SecretKeySpec(token.toByteArray(Charsets.UTF_8), "HmacMD5")
        mac.init(secretKey)
        val bytes = mac.doFinal(this.toByteArray(Charsets.UTF_8))
        bytes.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        ""
    }
}