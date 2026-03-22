package com.ynufe.utils

/**
 * 将字符串中的全角字符转换为半角字符
 */
fun String.toHalfWidth(): String {
    val charArray = this.toCharArray()
    for (i in charArray.indices) {
        if (charArray[i].code == 12288) { // 全角空格
            charArray[i] = 32.toChar()
        } else if (charArray[i].code in 65281..65374) { // 其他全角字符
            charArray[i] = (charArray[i].code - 65248).toChar()
        }
    }
    return String(charArray)
}