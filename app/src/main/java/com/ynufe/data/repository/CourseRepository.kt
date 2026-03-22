package com.ynufe.data.repository

import com.ynufe.data.api.ApiServices
import com.ynufe.data.room.course.CourseDao
import com.ynufe.utils.LoginResult
import javax.inject.Inject

class CourseRepository @Inject constructor(
    private val apiServices: ApiServices,
    private val parser: ParseJsp,
    private val courseDao: CourseDao
) {
    suspend fun getCourseTable(studentId: String): LoginResult {
        return try {
            val response = apiServices.getCourseTable()
            if (response.isSuccessful) {
                val html = response.body() ?: ""
                if (html.isNotEmpty()) {
                    parser.parseCourseTable(html, studentId)
                    LoginResult.Success(Unit)
                } else {
                    LoginResult.Error("课表数据为空")
                }
            } else {
                LoginResult.Error("获取课表失败: ${response.code()}")
            }
        } catch (e: Exception) {
            LoginResult.Error("获取课表异常: ${e.localizedMessage}")
        }
    }

    //  提供给viewmodel
    suspend fun deleteCoursesByStudentId(studentId: String) =
        courseDao.deleteCoursesByStudentId(studentId)
}