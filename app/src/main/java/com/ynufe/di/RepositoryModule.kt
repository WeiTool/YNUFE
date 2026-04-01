package com.ynufe.di

import android.content.Context
import com.google.gson.Gson
import com.ynufe.data.api.AppApi
import com.ynufe.data.api.VersionApi
import com.ynufe.data.api.WlanApi
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
import com.ynufe.utils.wlan.XXTEA
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
        appApi: AppApi,
        encoder: EncodeUtils,
        parser: ParseJsp,
        cookieJar: MemoryCookieJar
    ): LoginSystem {
        return LoginSystem(appApi, encoder, parser, cookieJar)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        loginSystem: LoginSystem,
        appApi: AppApi,
        parser: ParseJsp,
        deleteDao: UserDeleteDao,
        userDao: UserDao,
    ): UserRepository {
        return UserRepository(loginSystem, appApi, parser, deleteDao, userDao)
    }

    @Provides
    @Singleton
    fun provideCourseRepository(
        appApi: AppApi,
        parser: ParseJsp,
        courseDao: CourseDao
    ): CourseRepository {
        return CourseRepository(appApi, parser, courseDao)
    }

    @Provides
    @Singleton
    fun provideGradeRepository(
        gradeDao: GradeDao,
        parser: ParseJsp,
        appApi: AppApi
    ): GradeRepository {
        return GradeRepository(gradeDao, parser, appApi)
    }

    @Provides
    @Singleton
    fun provideCheckVersionRepository(
        versionApi: VersionApi,
        @ApplicationContext context: Context
    ): CheckVersionRepository {
        return CheckVersionRepository(versionApi, context)
    }

    @Provides
    @Singleton
    fun provideWlanRepository(
        wlanApi: WlanApi,
        wlanUserDao: UserWlanInfoDao,
        cryptoManager: CryptoManager,
        gson: Gson,
        XXTEA: XXTEA
    ): WlanRepository {
        return WlanRepository(wlanApi, wlanUserDao, cryptoManager,gson, XXTEA)
    }
}