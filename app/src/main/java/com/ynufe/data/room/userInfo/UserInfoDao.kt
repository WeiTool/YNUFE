package com.ynufe.data.room.userInfo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserInfoEntity)

    /**
     * 按 studentId 精确查询用户信息。
     * 当激活账号切换时，此 Flow 会立即发出 null（新账号尚无数据），
     * 避免 UI 残留旧账号的信息。
     */
    @Query("SELECT * FROM user_info WHERE studentId = :studentId LIMIT 1")
    fun getUserInfoByStudentId(studentId: String): Flow<UserInfoEntity?>
}