package com.ynufe.data.room.grade

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: GradeEntity)

    // 获取所有成绩，按学期降序排列
    @Query("""
        SELECT * FROM (
            SELECT *,
            REPLACE(REPLACE(REPLACE(REPLACE(UPPER(courseName), ' ', ''), '　', ''), '（', '('), '）', ')') AS normalizedName
            FROM grade 
            WHERE studentId = :studentId
        ) 
        GROUP BY normalizedName
        HAVING MAX(CAST(score AS REAL)) 
        ORDER BY term DESC
    """)
    fun getGradesByStudentId(studentId: String): Flow<List<GradeEntity>>

    @Query("DELETE FROM grade WHERE studentId = :studentId")
    suspend fun deleteGradesByStudentId(studentId: String)
}