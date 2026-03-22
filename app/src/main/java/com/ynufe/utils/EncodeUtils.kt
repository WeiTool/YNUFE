package com.ynufe.utils

import android.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncodeUtils @Inject constructor() {

    /**
     * 按照服务端 JS 算法生成 encoded 字段。
     *
     * 服务器在 Logon.do?flag=sess 响应体里返回 "scode#sxh"：
     *   - scode：一串随机字符，作为插入源
     *   - sxh：每位是一个数字，表示从 scode 头部取几个字符插入到 code[i] 之后
     *
     * code = userAccount + "%%%" + userPassword  （明文拼接，不做任何编码）
     * 只处理前 20 位，第 20 位之后原样追加。
     *
     * @param userAccount 学号
     * @param userPassword 密码（明文）
     * @param sessData     initSession 接口返回的原始字符串，格式 "scode#sxh"
     */
    fun getFirstEncode(userAccount: String, userPassword: String, sessData: String): String {
        // 切片sess 例如l8tD46688679V9Z7A45nO9avb5wVcm53ur67JA75P7uOp3#31323332123223331231
        val parts = sessData.split("#")
        // 如果服务器返回格式异常，降级为明文拼接（会被服务器拒绝，但至少不崩溃）
        if (parts.size < 2) {
            return "$userAccount%%%$userPassword"
        }

        var scode = parts[0]
        val sxh = parts[1]
        val code = "$userAccount%%%$userPassword"
        val encoded = StringBuilder()

        // 循环用户与密码的长度
        for (i in code.indices) {
            if (i < 20) {
                // sxh[i] 是单个数字字符，表示从 scode 头部取几个字符
                val n = sxh[i].digitToInt() // 3
                encoded.append(code[i]) // 2
                encoded.append(scode.substring(0, n)) // l8t
                scode = scode.substring(n) // 2
            } else {
                // 第 20 位之后原样追加，结束循环
                encoded.append(code.substring(i))
                break
            }
        }

        return encoded.toString()
    }

    fun encodeInp(input: String): String {
        // Base64.NO_WRAP 表示不换行，保持和 JS 输出一致
        return Base64.encodeToString(input.toByteArray(), Base64.NO_WRAP)
    }

    fun getSecondeEncode(userAccount: String, userPassword: String) : String {
        val processedAccount = encodeInp(userAccount)
        val processedPassword = encodeInp(userPassword)
        return "$processedAccount%%%$processedPassword"
    }
}