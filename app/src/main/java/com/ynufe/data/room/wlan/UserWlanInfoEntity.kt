package com.ynufe.data.room.wlan

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ynufe.data.model.LoginResponse

@Entity(tableName = "user_wlan_info")
data class UserWlanInfoEntity(
    @PrimaryKey val studentId: String,
    val password: String = "",
    val isActive: Boolean = false,
    val ip: String,
    val onlineUser: String,
    val location: String = "",
    val error: String = "",
    val errorMsg: String = "",
    val ployMsg: String = "",
    val sucMsg: String = "",
    val log: String = ""
)