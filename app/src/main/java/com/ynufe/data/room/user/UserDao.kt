package com.ynufe.data.room.user

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // 获取当前活动User
    @Query("SELECT * FROM user WHERE isActive = 1 LIMIT 1")
    fun getIsActiveUser(): Flow<UserEntity?>

    // 获取当前活动User学号
    @Query("SELECT studentId FROM user WHERE isActive = 1 LIMIT 1")
    fun getIsActiveUserStudentId(): Flow<String?>

    // 获取所有User
    @Query("SELECT * FROM user")
    fun getAllUsers(): Flow<List<UserEntity>>

    // 更新开学时间
    @Query("UPDATE user SET startTime = :time WHERE studentId = :id")
    suspend fun updateUserStartTime(id: String, time: Long)

    // 激活指定用户
    @Query("UPDATE user SET isActive = 1 WHERE studentId = :studentId")
    suspend fun setActivateUser(studentId: String)

    // 将所有用户设为非激活
    @Query("UPDATE user SET isActive = 0")
    suspend fun deactivateAllUsers()
}