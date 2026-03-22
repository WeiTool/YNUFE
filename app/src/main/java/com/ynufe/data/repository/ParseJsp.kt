package com.ynufe.data.repository

import com.ynufe.data.room.course.CourseDao
import com.ynufe.data.room.course.CourseEntity
import com.ynufe.data.room.grade.GradeDao
import com.ynufe.data.room.grade.GradeEntity
import com.ynufe.data.room.userInfo.UserInfoDao
import com.ynufe.data.room.userInfo.UserInfoEntity
import com.ynufe.utils.TextUtils
import org.jsoup.Jsoup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParseJsp @Inject constructor(
    private val userDao: UserInfoDao,
    private val courseDao: CourseDao,
    private val gradeDao: GradeDao,
    private val textUtils: TextUtils
) {


    // ─────────────────────────────────────────────────────────────
    // 登录错误解析
    // ─────────────────────────────────────────────────────────────

    fun parseLoginError(html: String): String {
        return try {
            val doc = Jsoup.parse(html)
            val errorText = doc.getElementById("showMsg")?.text()?.trim() ?: ""
            errorText.ifEmpty { "登录验证失败，请稍后重试" }
        } catch (e: Exception) {
            "解析错误信息失败"
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 用户信息解析
    // ─────────────────────────────────────────────────────────────

    suspend fun parseStudentInfo(html: String) {
        val doc = Jsoup.parse(html)
        val contents = doc.select(".middletopdwxxcont")

        val name = contents.getOrNull(1)?.text() ?: "姓名获取失败"
        val studentId = contents.getOrNull(2)?.text() ?: "0"
        val college = contents.getOrNull(3)?.text() ?: "学院获取失败"
        val major = contents.getOrNull(4)?.text() ?: "专业获取失败"
        val className = contents.getOrNull(5)?.text() ?: "班级获取失败"

        userDao.insertUser(
            UserInfoEntity(
                name = name,
                studentId = studentId,
                college = college,
                major = major,
                className = className
            )
        )
    }

    // ─────────────────────────────────────────────────────────────
    // 课表解析
    // ─────────────────────────────────────────────────────────────

    /**
     * 解析课表并直接存入数据库。
     *
     * 每个课程格子在 HTML 中通常包含两个 div：
     *   - 第一个 div：只有课程名/教室/周次，没有教师（teacher 为空）
     *   - 第二个 div：包含完整信息，包括教师
     *
     * 处理策略：
     *   1. 跳过 teacher 为空的 div（它是冗余的头部条目）
     *   2. 清洗 name / teacher / room 中重复拼接的内容，只保留第一段
     *   3. 从 weeks 字段的节次标记（如 [03-04-05节]）反推真实 rowSpan
     */
    suspend fun parseCourseTable(html: String, studentId: String) {
        val doc = Jsoup.parse(html)
        val courseTable = doc.select("#kbtable").first()
        val rows = courseTable?.select("tr") ?: return

        // 用于在本次解析中记录已处理的课程，防止重复插入
        // Key 格式: "课程名_星期几"
        // Value: 该课程已占用的节次集合 (例如: setOf(1, 2))
        val processedCourses = mutableMapOf<String, MutableSet<Int>>()

        val occupied = Array(14) { BooleanArray(8) }

        for (rowIndex in 1 until rows.size) {
            if (rowIndex > 13) break
            val row = rows[rowIndex]
            val tds = row.select("td")
            var tdPointer = 0

            for (dayOfWeek in 1..7) {
                if (occupied[rowIndex][dayOfWeek]) continue
                val cell = tds.getOrNull(tdPointer) ?: break
                tdPointer++

                // 处理 HTML 的合并单元格逻辑
                val htmlRowSpan = cell.attr("rowspan").toIntOrNull() ?: 1
                if (htmlRowSpan > 1) {
                    for (i in 1 until htmlRowSpan) {
                        if (rowIndex + i <= 13) occupied[rowIndex + i][dayOfWeek] = true
                    }
                }

                val courseDivs = cell.select("div[class^=kbcontent]")
                courseDivs.forEach { div ->
                    val rawName = div.ownText().trim().removeSurrounding("\"")
                    val rawTeacher = div.select("font[title=老师]").text().trim()
                    val rawWeeks = div.select("font[title=周次(节次)]").text().trim()
                    val rawRoom = div.select("font[title=教室]").text().trim()

                    if (rawName.isEmpty() || rawTeacher.isEmpty()) return@forEach

                    val cleanName = textUtils.cleanField(rawName)
                    val cleanTeacher = textUtils.cleanTeacher(rawTeacher)
                    val realRowSpan =
                        textUtils.extractRowSpan(rawWeeks).takeIf { it > 1 } ?: htmlRowSpan

                    // ─────────────────────────────────────────────────────────────
                    // 关键去重逻辑：
                    // ─────────────────────────────────────────────────────────────
                    val courseKey = "${cleanName}_$dayOfWeek"
                    val occupiedSections = processedCourses.getOrPut(courseKey) { mutableSetOf() }

                    // 如果当前起始节次 (rowIndex) 已经被该课之前的记录覆盖了，则跳过
                    if (occupiedSections.contains(rowIndex)) {
                        return@forEach
                    }

                    // 将该课占用的所有节次标记为已处理
                    // 例如：第1节课，rowSpan为2，则标记 1 和 2 已经有课了
                    for (i in 0 until realRowSpan) {
                        occupiedSections.add(rowIndex + i)
                    }

                    // 执行插入（此时只会插入该课在当天的第一个起始点）
                    courseDao.insertCourse(
                        CourseEntity(
                            studentId = studentId,
                            name = cleanName,
                            teacher = cleanTeacher,
                            room = textUtils.cleanField(rawRoom),
                            weeks = textUtils.cleanField(rawWeeks),
                            dayOfWeek = dayOfWeek,
                            startSection = rowIndex,
                            rowSpan = realRowSpan
                        )
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 成绩解析
    // ─────────────────────────────────────────────────────────────

    suspend fun parseGradeTable(html: String, studentId: String) {
        val doc = Jsoup.parse(html)
        val courseTable = doc.select("#dataList").first()
        val rows = courseTable?.select("tr") ?: return

        for (rowIndex in 1 until rows.size) {
            val row = rows[rowIndex]
            val columns = row.select("td")
            val term = columns[1].text().trim()
            val courseName = columns[3].text().trim()
            val score = columns[5].text().trim()
            val credit = columns[7].text().trim()
            val gradePoint = columns[9].text().trim()
            val courseType = columns[13].text().trim()

            gradeDao.insertGrade(
                GradeEntity(
                    studentId = studentId,
                    courseName = courseName,
                    term = term,
                    score = score,
                    credit = credit,
                    gradePoint = gradePoint,
                    courseType = courseType
                )
            )
        }
    }
}