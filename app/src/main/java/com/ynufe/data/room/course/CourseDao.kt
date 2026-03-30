package com.ynufe.data.room.course

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    // 插入用户课表
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    // 获取用户课表
    @Query("SELECT * FROM course WHERE studentId = :studentId")
    fun getCoursesByStudentId(studentId: String): Flow<List<CourseEntity>>

    // 删除用户课表
    @Query("DELETE FROM course WHERE studentId = :studentId")
    suspend fun deleteCoursesByStudentId(studentId: String)
}