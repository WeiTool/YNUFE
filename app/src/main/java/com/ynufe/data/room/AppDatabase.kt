package com.ynufe.data.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ynufe.data.room.course.CourseDao
import com.ynufe.data.room.course.CourseEntity
import com.ynufe.data.room.grade.GradeDao
import com.ynufe.data.room.grade.GradeEntity
import com.ynufe.data.room.user.UserDao
import com.ynufe.data.room.user.UserEntity
import com.ynufe.data.room.userInfo.UserDeleteDao
import com.ynufe.data.room.userInfo.UserInfoDao
import com.ynufe.data.room.userInfo.UserInfoEntity
import com.ynufe.data.room.wlan.UserWlanInfoDao
import com.ynufe.data.room.wlan.UserWlanInfoEntity

@Database(
    entities = [
        UserEntity::class,
        UserInfoEntity::class,
        CourseEntity::class,
        GradeEntity::class,
        UserWlanInfoEntity::class
    ],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ],
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun userInfoDao(): UserInfoDao
    abstract fun courseDao(): CourseDao
    abstract fun userDeleteDao(): UserDeleteDao
    abstract fun gradeDao(): GradeDao
    abstract fun userWlanInfoDao(): UserWlanInfoDao
}