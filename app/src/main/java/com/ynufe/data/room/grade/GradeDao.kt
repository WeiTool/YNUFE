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
            -- 创建一个标准化名字：转大写、去半角空格、去全角空格、统一全角括号为半角
            REPLACE(REPLACE(REPLACE(REPLACE(UPPER(courseName), ' ', ''), '　', ''), '（', '('), '）', ')') AS normalizedName
            FROM grade 
            WHERE studentId = :studentId
        ) 
        GROUP BY normalizedName 
        -- 在重名课程中，取分数（转为数字比较）最高的那一行
        HAVING MAX(CAST(score AS REAL)) 
        ORDER BY term DESC
    """)
    fun getGradesByStudentId(studentId: String): Flow<List<GradeEntity>>

    @Query("DELETE FROM grade WHERE studentId = :studentId")
    suspend fun deleteGradesByStudentId(studentId: String)
}