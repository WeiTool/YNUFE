package com.ynufe.utils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextUtils @Inject constructor(){

    // ─────────────────────────────────────────────────────────────
    // 文本清洗工具
    // ─────────────────────────────────────────────────────────────
    fun cleanField(raw: String): String {
        if (raw.isBlank()) return ""

        // 1. 将字符串按空格切分成列表
        // 2. 过滤掉空字符串
        // 3. 使用 distinct() 去除重复的词组
        // 4. 重新用空格连接
        val parts = raw.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.distinct()

        // 如果你确定重复的都是一模一样的内容，取第一个即可
        return parts.firstOrNull() ?: ""
    }

    /**
     * 从 weeks 字段提取实际跨节数。
     * 例："1-18(周)[03-04节]"     → 2
     *     "1-18(周)[06-07-08节]"  → 3
     *     "1-18(周)"              → 1（无节次标记时默认为 1）
     */
    fun extractRowSpan(weeks: String): Int {
        val match = Regex("\\[(\\d+(?:-\\d+)*)节]").find(weeks) ?: return 1
        val sections = match.groupValues[1].split("-")
        return if (sections.size >= 2) {
            // [01-02] -> 2 - 1 + 1 = 2
            sections.last().toInt() - sections.first().toInt() + 1
        } else {
            1
        }
    }

    /**
     * 专门针对教师姓名的清洗逻辑：
     * 1. 移除括号及其内部内容（支持全角和半角）
     * 2. 移除常见的职称关键词
     */
    fun cleanTeacher(raw: String): String {
        val basicClean = cleanField(raw)
        if (basicClean.isEmpty()) return ""

        return basicClean
            // 移除括号及其内容：匹配 (内容) 或 （内容）
            .replace(Regex("[(（].*?[)）]"), "")
            // 移除常见的职称关键词
            .replace(Regex("教授|副教授|讲师|助教"), "")
            .trim()
    }

    fun parseHrefParams(href: String): Map<String, String> {
        val params = mutableMapOf<String, String>()

        // 定义匹配模式：查找 key=value 结构
        // [^&']+ 意思是不匹配 & 符号和单引号，直到遇到它们为止
        val regex = Regex("(\\w+)=([^&']+)")
        val matches = regex.findAll(href)

        for (match in matches) {
            val (key, value) = match.destructured
            params[key] = value
        }

        return params
    }
}