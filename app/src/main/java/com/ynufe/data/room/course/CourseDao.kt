package com.ynufe.data.room.course

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Query("SELECT * FROM course WHERE studentId = :studentId")
    fun getCoursesByStudentId(studentId: String): Flow<List<CourseEntity>>

    @Query("DELETE FROM course WHERE studentId = :studentId")
    suspend fun deleteCoursesByStudentId(studentId: String)
}