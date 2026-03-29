package com.ynufe.di

import android.content.Context
import com.google.gson.Gson
import com.ynufe.data.api.ApiServices
import com.ynufe.data.repository.CheckVersionRepository
import com.ynufe.data.repository.CourseRepository
import com.ynufe.data.repository.GradeRepository
import com.ynufe.data.repository.LoginSystem
import com.ynufe.data.repository.ParseJsp
import com.ynufe.data.repository.UserRepository
import com.ynufe.data.repository.WlanRepository
import com.ynufe.data.room.course.CourseDao
import com.ynufe.data.room.grade.GradeDao
import com.ynufe.data.room.user.UserDao
import com.ynufe.data.room.userInfo.UserDeleteDao
import com.ynufe.data.room.wlan.UserWlanInfoDao
import com.ynufe.utils.CryptoManager
import com.ynufe.utils.EncodeUtils
import com.ynufe.utils.wlan.TEA
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
    @Provides
    @Singleton
    fun provideLoginSystem(
        apiServices: ApiServices,
        encoder: EncodeUtils,
        parser: ParseJsp,
        cookieJar: MemoryCookieJar
    ): LoginSystem {
        return LoginSystem(apiServices, encoder, parser, cookieJar)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        loginSystem: LoginSystem,
        apiServices: ApiServices,
        parser: ParseJsp,
        deleteDao: UserDeleteDao,
        userDao: UserDao,
    ): UserRepository {
        return UserRepository(loginSystem, apiServices, parser, deleteDao, userDao)
    }

    @Provides
    @Singleton
    fun provideCourseRepository(
        apiServices: ApiServices,
        parser: ParseJsp,
        courseDao: CourseDao
    ): CourseRepository {
        return CourseRepository(apiServices, parser, courseDao)
    }

    @Provides
    @Singleton
    fun provideGradeRepository(
        gradeDao: GradeDao,
        parser: ParseJsp,
        apiServices: ApiServices
    ): GradeRepository {
        return GradeRepository(gradeDao, parser, apiServices)
    }

    @Provides
    @Singleton
    fun provideCheckVersionRepository(
        apiServices: ApiServices,
        @ApplicationContext context: Context
    ): CheckVersionRepository {
        return CheckVersionRepository(apiServices, context)
    }

    @Provides
    @Singleton
    fun provideWlanRepository(
        apiServices: ApiServices,
        wlanUserDao: UserWlanInfoDao,
        cryptoManager: CryptoManager,
        gson: Gson,
        tea: TEA
    ): WlanRepository {
        return WlanRepository(apiServices, wlanUserDao, cryptoManager,gson, tea)
    }
}