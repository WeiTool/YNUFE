package com.ynufe.data.room.course

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "course",
    indices = [Index(value = ["studentId"])]
)
data class CourseEntity (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val name: String,         // 课程名
    val teacher: String,      // 老师
    val room: String,         // 教室
    val weeks: String,        // 周次字符串（如 "1-18周"）
    val dayOfWeek: Int,       // 星期几（1-7），对应你解析代码里的 dayOfWeek
    val startSection: Int,    // 开始节次，对应解析代码里的 rowIndex
    val rowSpan: Int          // 跨越节次，对应解析代码里的 rowSpan
)