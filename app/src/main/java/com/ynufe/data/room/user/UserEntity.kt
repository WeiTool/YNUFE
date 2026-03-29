package com.ynufe.data.room.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val studentId: String,
    val password: String = "",
    val startTime: Long? = null,
    val isActive: Boolean = false
)