package com.ynufe.data.room.userInfo

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface UserDeleteDao {
    @Transaction
    suspend fun deleteAllUserInfo(studentId: String) {
        deleteCourseByStudentId(studentId)
        deleteUserInfoByStudentId(studentId)
        deleteUserByStudentId(studentId)
        deleteGradeByStudentId(studentId)
    }

    @Transaction
    suspend fun deleteUser(studentId: String) {
        deleteUserInfoByStudentId(studentId)
    }

    @Query("DELETE FROM user WHERE studentId = :studentId")
    suspend fun deleteUserByStudentId(studentId: String)

    @Query("DELETE FROM user_info WHERE studentId = :studentId")
    suspend fun deleteUserInfoByStudentId(studentId: String)

    @Query("DELETE FROM course WHERE studentId = :studentId")
    suspend fun deleteCourseByStudentId(studentId: String)

    @Query("DELETE FROM grade WHERE studentId = :studentId")
    suspend fun deleteGradeByStudentId(studentId: String)
}