package com.ynufe.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    /** 获取当前日期的显示文本 (如: 26年03月20日) */
    fun getCurrentDateDisplay(): String =
        SimpleDateFormat("yy年MM月dd日", Locale.getDefault()).format(Date())

    /** 获取今日星期文本 (如: 星期五) */
    fun getTodayDayName(): String =
        SimpleDateFormat("EEEE", Locale.CHINESE).format(Date())

    /** 将毫秒时间戳格式化为显示用的日期字符串 (yyyy/MM/dd) */
    fun formatDateMs(ms: Long): String =
        SimpleDateFormat("yyyy年MM月dd", Locale.getDefault()).format(Date(ms))

    /** 计算当前是第几周的数字 (返回 Int，未设置或未开学返回 -1) */
    fun getCurrentWeekInt(semesterStartMs: Long?): Int {
        semesterStartMs ?: return -1
        val startCal = Calendar.getInstance().apply {
            timeInMillis = semesterStartMs
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val diffMs = todayCal.timeInMillis - startCal.timeInMillis
        if (diffMs < 0) return -1
        return (diffMs / (7L * 24 * 60 * 60 * 1000)).toInt() + 1
    }

    /** 返回显示的周次文字 */
    fun getWeekDescription(weekInt: Int): String {
        return when {
            weekInt == -1 -> "未开学"
            else -> "第 $weekInt 周"
        }
    }

    /**
     * 核心逻辑：判断当前周是否需要上课
     * 解析格式如: "2,6,10,14(周)[01-02节]" 或 "1-18(周)[03-04节]"
     */
    fun isCourseInCurrentWeek(weeksString: String, currentWeek: Int): Boolean {
        if (currentWeek <= 0) return true // 未设置日期时默认显示所有课
        try {
            // 提取 (周) 之前的内容，例如 "2,6,10,14"
            val weekPart = weeksString.substringBefore("(周)")
            val segments = weekPart.split(",")
            for (segment in segments) {
                if (segment.contains("-")) {
                    val range = segment.split("-")
                    if (currentWeek in range[0].trim().toInt()..range[1].trim().toInt()) return true
                } else {
                    if (segment.trim().toInt() == currentWeek) return true
                }
            }
        } catch (e: Exception) {
            return true // 解析失败则保守显示
        }
        return false
    }
}