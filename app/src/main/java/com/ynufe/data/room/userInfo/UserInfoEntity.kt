package com.ynufe.data.room.userInfo

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_info")
data class UserInfoEntity(
    @PrimaryKey val studentId: String,
    val name: String,
    val college: String,
    val major: String,
    val className: String
)