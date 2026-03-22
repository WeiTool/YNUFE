package com.ynufe.data.room.grade

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "grade",
    indices = [Index(value = ["studentId"])]
)
data class GradeEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val studentId: String,
    val courseName: String,
    val term: String,
    val score: String,
    val credit: String,
    val gradePoint: String,
    val courseType: String
)