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

    /** 返回当前激活的用户（isActive = 1）*/
    @Query("SELECT * FROM user WHERE isActive = 1 LIMIT 1")
    fun getUser(): Flow<UserEntity?>

    @Query("SELECT * FROM user")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("UPDATE user SET startTime = :time WHERE studentId = :id")
    suspend fun updateUserStartTime(id: String, time: Long)

    /** 将所有用户设为非激活 */
    @Query("UPDATE user SET isActive = 0")
    suspend fun deactivateAllUsers()

    /** 激活指定用户 */
    @Query("UPDATE user SET isActive = 1 WHERE studentId = :studentId")
    suspend fun activateUser(studentId: String)
}