package com.ynufe.data.repository

import com.ynufe.data.api.ApiServices
import com.ynufe.data.room.grade.GradeDao
import com.ynufe.utils.LoginResult
import javax.inject.Inject

class GradeRepository @Inject constructor(
    private val gradeDao: GradeDao,
    private val parser: ParseJsp,
    private val apiServices: ApiServices
) {
    suspend fun getGradeTable(studentId: String): LoginResult {
        return try {
            val response = apiServices.getGradeTable(
                fields = mutableMapOf(
                    "kksj" to "",
                    "kcxz" to "",
                    "kcmc" to "",
                    "xsfs" to "all"
                )
            )
            if (response.isSuccessful) {
                val html = response.body() ?: ""
                if (html.isNotEmpty()) {
                    parser.parseGradeTable(html, studentId)
                    LoginResult.Success(Unit)
                } else {
                    LoginResult.Error("成绩数据为空")
                }
            } else {
                LoginResult.Error("获取成绩失败: ${response.code()}")
            }
        } catch (e: Exception) {
            LoginResult.Error("获取成绩异常: ${e.localizedMessage}")
        }
    }

    // 提供给View
    fun getGradesByStudentId(studentId: String) = gradeDao.getGradesByStudentId(studentId) // 获取学生成绩
    suspend fun deleteGradesByStudentId(studentId: String) =
        gradeDao.deleteGradesByStudentId(studentId) // 删除学生所有成绩
}