package com.ynufe.data.room.wlan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserWlanInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserInfo(user: UserWlanInfoEntity)

    // 获取所有用户信息
    @Query("SELECT * FROM user_wlan_info")
    fun getAllWlanInfo(): Flow<List<UserWlanInfoEntity>>

    // 获取学号对应的区域
    @Query("SELECT location FROM user_wlan_info WHERE studentId = :studentId")
    suspend fun getLocationById(studentId: String): String?

    @Query("SELECT log FROM user_wlan_info WHERE studentId = :studentId")
    suspend fun getLogById(studentId: String): String?

    // 获取激活用户学号
    @Query("SELECT * FROM user_wlan_info WHERE isActive = 1 LIMIT 1")
    fun getActiveFlow(): Flow<UserWlanInfoEntity?>

    // 激活指定用户
    @Query("UPDATE user_wlan_info SET isActive = 1 WHERE studentId = :studentId")
    suspend fun activateUser(studentId: String)

    // 根据学号获取isActive
    @Query("SELECT isActive FROM user_wlan_info WHERE studentId = :studentId")
    suspend fun getIsActiveById(studentId: String): Boolean

    // 根据学号删除该账号记录
    @Query("DELETE FROM user_wlan_info WHERE studentId = :studentId")
    suspend fun deleteByStudentId(studentId: String)

    // 删除所有账号记录
    @Query("DELETE FROM user_wlan_info")
    suspend fun deleteAllStudent()

    // 将所有用户设为非激活
    @Query("UPDATE user_wlan_info SET isActive = 0")
    suspend fun deactivateAllUsers()

    // 清除所有的错误信息状态
    @Query("UPDATE user_wlan_info SET error = '', errorMsg = '', ployMsg = '', sucMsg = '', log = '' WHERE studentId = :studentId")
    suspend fun clearAllStatus(studentId: String)

    // 退出时更新错误信息
    @Query(
        """
        UPDATE user_wlan_info 
        SET error = :error, errorMsg = :errorMsg, ployMsg = '', sucMsg = '' 
        WHERE studentId = :studentId
    """
    )
    suspend fun updateLogoutStatus(studentId: String, error: String, errorMsg: String)

    // 更新log字段
    @Query("UPDATE user_wlan_info SET log = :log WHERE studentId = :studentId")
    suspend fun updateLog(studentId: String, log: String)
}