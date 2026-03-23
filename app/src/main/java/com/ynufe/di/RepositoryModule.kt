package com.ynufe.di

import android.content.Context
import com.ynufe.data.repository.LoginSystem
import com.ynufe.data.repository.ParseJsp
import com.ynufe.data.api.ApiServices
import com.ynufe.data.repository.CheckVersionRepository
import com.ynufe.data.repository.CourseRepository
import com.ynufe.data.repository.GradeRepository
import com.ynufe.data.repository.UserRepository
import com.ynufe.data.room.course.CourseDao
import com.ynufe.data.room.grade.GradeDao
import com.ynufe.data.room.userInfo.UserDeleteDao
import com.ynufe.data.room.userInfo.UserInfoDao
import com.ynufe.utils.EncodeUtils
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
        userInfoDao: UserInfoDao,
        deleteDao: UserDeleteDao
    ): UserRepository {
        return UserRepository(loginSystem, apiServices, parser, deleteDao, userInfoDao)
    }

    @Provides
    @Singleton
    fun provideCourseRepository(
        apiServices: ApiServices,
        parser: ParseJsp,
        courseDao: CourseDao
    ): CourseRepository{
        return CourseRepository(apiServices, parser, courseDao)
    }

    @Provides
    @Singleton
    fun provideGradeRepository(
        gradeDao: GradeDao,
        parser: ParseJsp,
        apiServices: ApiServices
    ): GradeRepository{
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

}