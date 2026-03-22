package com.ynufe.di

import android.content.Context
import androidx.room.Room
import com.ynufe.data.room.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ynufe_database"
        ).build()
    }

    @Provides
    fun provideUserDao(db: AppDatabase) = db.userDao()

    @Provides
    fun provideUserInfoDao(db: AppDatabase) = db.userInfoDao()

    @Provides
    fun provideCourseDao(db: AppDatabase) = db.courseDao()

    @Provides
    fun provideUserDeleteDao(db: AppDatabase) = db.userDeleteDao()

    @Provides
    fun provideGradeDao(db: AppDatabase) = db.gradeDao()

}