package com.ynufe.ui.course

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ynufe.data.room.course.CourseEntity
import com.ynufe.ui.theme.AppTextStyle
import com.ynufe.ui.theme.COURSE_COLORS
import com.ynufe.ui.theme.courseCardStylesForWidth
import com.ynufe.utils.CourseUiState
import com.ynufe.utils.DateUtils
import com.ynufe.utils.DateUtils.formatDateMs
import com.ynufe.utils.toHalfWidth
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs


// ─────────────────────────────────────────────
// 常量
// ─────────────────────────────────────────────

private val CELL_HEIGHT: Dp = 80.dp
private val TIME_COL_WIDTH: Dp = 50.dp

private fun courseColor(name: String): Color =
    COURSE_COLORS[abs(name.hashCode()) % COURSE_COLORS.size]

// ─────────────────────────────────────────────
// 入口：从 ViewModel 收集 uiState 并分发
// ─────────────────────────────────────────────

@Composable
fun CourseScreen(viewModel: CourseViewModel = hiltViewModel()) {
    val currentTime = remember { DateUtils.getCurrentDateDisplay() }
    val dayName = remember { DateUtils.getTodayDayName() }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val uiState by viewModel.uiState.collectAsState()

    // 仅在有用户数据时才能读取学期开始时间
    val classBeginTime = when (val s = uiState) {
        is CourseUiState.Success -> s.classBeginTime
        is CourseUiState.Empty -> s.classBeginTime
        else -> null
    }

    AppNavigationDrawer(
        drawerState = drawerState,
        currentScheduleName = viewModel.currentScheduleName,
        onToggleSchedule = { isAnning -> viewModel.toggleSchedule(isAnning) },
        classBeginTime = classBeginTime,
        onClassBeginTimeChange = { newTime -> viewModel.updateSemesterStart(newTime) }
    ) {
        when (val state = uiState) {
            // 数据库初始化中 → 骨架屏
            is CourseUiState.Loading -> CourseLoadingContent(
                onMenuClick = { scope.launch { drawerState.open() } }
            )
            // 未绑定账号 → 引导空状态
            is CourseUiState.NoUser -> CourseNoUserContent(
                onMenuClick = { scope.launch { drawerState.open() } }
            )
            // 已绑定但无课表 → 同样显示引导
            is CourseUiState.Empty -> CourseNoUserContent(
                onMenuClick = { scope.launch { drawerState.open() } }
            )
            // 课表就绪 → 正常渲染
            is CourseUiState.Success -> {

                CourseScreenContent(
                    currentTime = currentTime,
                    dayName = dayName,
                    timeSlots = viewModel.currentTimeSlots,
                    courses = state.courses,
                    semesterStartMs = state.classBeginTime?: 0L, // 传递学期开始时间供 Pager 计算
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// CourseUiState.Loading → 骨架屏
// ─────────────────────────────────────────────

@Composable
fun CourseLoadingContent(onMenuClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        CourseTopAppBar(
            currentTime = "加载中...",
            currentWeek = "",
            dayFormat = "",
            onMenuClick = onMenuClick
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(7) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// CourseUiState.NoUser → 引导空状态
// ─────────────────────────────────────────────

@Composable
fun CourseNoUserContent(onMenuClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        CourseTopAppBar(
            currentTime = "",
            currentWeek = "",
            dayFormat = "",
            onMenuClick = onMenuClick
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "暂无课表数据",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "左上角功能菜单可以设置校区与上课日期",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    // 全角括号改为半角
                    text = "(需要先绑定账号)",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "请前往用户页面绑定账号并获取课表",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// CourseUiState.Success → 课表主内容
// ─────────────────────────────────────────────

@Composable
fun CourseScreenContent(
    currentTime: String,
    dayName: String,
    timeSlots: List<TimeSlot>,
    courses: List<CourseEntity>,
    onMenuClick: () -> Unit,
    semesterStartMs: Long
) {
    val pageCount = 20
    val actualCurrentWeek = DateUtils.getCurrentWeekInt(semesterStartMs)
    val initialPage = (actualCurrentWeek - 1).coerceIn(0, pageCount - 1)

    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部导航栏
        CourseTopAppBar(
            currentTime = currentTime,
            currentWeek = if (pagerState.currentPage + 1 == actualCurrentWeek) {
                "第 ${pagerState.currentPage + 1} 周 (本周)"
            } else {
                "第 ${pagerState.currentPage + 1} 周"
            },
            dayFormat = dayName,
            onMenuClick = onMenuClick,
        )

        // 使用 Box 包裹，使按钮能悬浮在课表上方
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                TimeColumn(timeSlots = timeSlots)

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    beyondViewportPageCount = 1
                ) { pageIndex ->
                    val displayWeek = pageIndex + 1
                    val weekCourses = remember(courses, displayWeek) {
                        courses.filter {
                            DateUtils.isCourseInCurrentWeek(it.weeks, displayWeek)
                        }
                    }
                    CourseTable(
                        courses = weekCourses,
                        timeSlots = timeSlots,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 只要当前页面不是“本周”对应的页面，就显示按钮
            this@Column.AnimatedVisibility(
                visible = pagerState.currentPage != initialPage,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 32.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            // 平滑滚动回“本周”
                            pagerState.animateScrollToPage(initialPage)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "回到本周"
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 星期标题行
// ─────────────────────────────────────────────

@Composable
fun WeekHeaderRow() {
    val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val todayIndex = run {
        val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        (dow - 2 + 7) % 7
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Spacer(modifier = Modifier.width(TIME_COL_WIDTH))

        dayNames.forEachIndexed { index, name ->
            val isToday = index == todayIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = name,
                        // 从 AppTextStyle 统一取样式；今日列动态覆盖 fontWeight
                        style = AppTextStyle.weekDayLabel,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) Color.Red
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 今日列下方红色高亮条
                    Box(
                        modifier = Modifier
                            .width(20.dp)
                            .height(2.dp)
                            .background(if (isToday) Color.Red else Color.Transparent)
                    )
                }
            }
        }
    }

    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
}

// ─────────────────────────────────────────────
// 时间列
// ─────────────────────────────────────────────

@Composable
fun TimeColumn(timeSlots: List<TimeSlot>) {
    Column(
        modifier = Modifier
            .width(TIME_COL_WIDTH)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        timeSlots.forEach { slot ->
            Box(
                modifier = Modifier
                    .height(CELL_HEIGHT)
                    .fillMaxWidth()
                    .border(0.3.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 节次编号，如 "1"、"12"
                    Text(
                        text = "${slot.section}",
                        style = AppTextStyle.timeSlotSection,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // 上课时间
                    Text(
                        text = slot.start,
                        style = AppTextStyle.timeSlotTime,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 下课时间，稍加透明度以形成层次
                    Text(
                        text = slot.end,
                        style = AppTextStyle.timeSlotTime,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 课表主体
// ─────────────────────────────────────────────

@Composable
fun CourseTable(
    courses: List<CourseEntity>,
    timeSlots: List<TimeSlot>,
    modifier: Modifier = Modifier
) {
    val todayIndex = run {
        val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        (dow - 2 + 7) % 7
    }
    val coursesByDay = (1..7).map { day -> courses.filter { it.dayOfWeek == day } }

    Row(modifier = modifier.fillMaxWidth()) {
        (0..6).forEach { dayIndex ->
            DayColumn(
                courses = coursesByDay[dayIndex],
                totalRows = timeSlots.size,
                isToday = dayIndex == todayIndex,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────
// 单日列
// ─────────────────────────────────────────────

@Composable
fun DayColumn(
    courses: List<CourseEntity>,
    totalRows: Int,
    isToday: Boolean,
    modifier: Modifier = Modifier
) {
    val columnHeight = CELL_HEIGHT * totalRows

    Box(
        modifier = modifier
            .height(columnHeight)
            .background(
                if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                else Color.Transparent
            )
            .border(0.3.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        // 绘制行分割线
        repeat(totalRows) { row ->
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = CELL_HEIGHT * row),
                thickness = 0.3.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
        // 渲染课程卡片
        courses.forEach { course ->
            val topOffset = CELL_HEIGHT * (course.startSection - 1)
            val cardHeight = CELL_HEIGHT * course.rowSpan
            CourseCard(
                course = course,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .offset(y = topOffset)
                    // 卡片与列边框之间留 2dp 间距
                    .padding(2.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────
// 课程卡片
//
// 设计原则：
//   1. BoxWithConstraints 在运行时拿到实际卡片内容宽度
//   2. courseCardStylesForWidth() 按宽度断点选择最适合的字号组合
//   3. 课名 / 教室 / 教师三行信息在任意分辨率下均完整显示
//   4. toHalfWidth() 将全角标点转为半角，节省宝贵像素
//   5. maxLines + TextOverflow.Ellipsis 兜底，绝对不会越界
// ─────────────────────────────────────────────

@Composable
fun CourseCard(course: CourseEntity, modifier: Modifier = Modifier) {
    val bgColor = courseColor(course.name)

    // BoxWithConstraints 让我们在 Composable 测量阶段获得实际可用宽度，
    // 然后再决定用哪套字号，避免任何硬编码 sp 散落在此。
    // 注意：padding 在 modifier 里，BoxWithConstraints 内部 maxWidth
    // 已经是减去 padding 后的净内容宽度。
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor.copy(alpha = 0.9f))
            // 水平 padding 收窄至 3dp，为文字多留 1dp 空间
            .padding(horizontal = 3.dp, vertical = 4.dp)
    ) {
        // 根据卡片实际内容宽度选择对应字号档位
        val styles = courseCardStylesForWidth(maxWidth.value)

        // rowSpan >= 2 时卡片有足够垂直空间，行间加 2dp 间距
        val isTall = course.rowSpan >= 2

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = if (isTall) Arrangement.spacedBy(2.dp) else Arrangement.Top
        ) {
            // ── 课程名称（始终显示）────────────────────────────────
            // maxLines：单格最多 3 行（高度充裕），多格可适当放更多
            Text(
                text = course.name.toHalfWidth(),
                style = styles.title,
                color = Color.White,
                softWrap = true
            )

            // ── 教室（非空时显示，所有卡片均展示）─────────────────
            if (course.room.isNotEmpty()) {
                Text(
                    text = course.room.toHalfWidth(),
                    style = styles.room,
                    color = Color.White.copy(alpha = 0.92f),
                    softWrap = true
                )
            }

            // ── 教师（非空时显示，所有卡片均展示）─────────────────
            if (course.teacher.isNotEmpty()) {
                Text(
                    text = course.teacher.toHalfWidth(),
                    style = styles.teacher,
                    color = Color.White.copy(alpha = 0.80f),
                    softWrap = true
                )
            }
        }
    }
}

// ─────────────────────────────────────────────
// TopAppBar（含星期标题行）
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseTopAppBar(
    currentTime: String,
    currentWeek: String,
    dayFormat: String,
    onMenuClick: () -> Unit,
) {
    // 使用 BoxWithConstraints 动态获取屏幕宽度
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // 1. 计算当前卡片的内容宽度 (逻辑同步自 Type.kt)
        val screenWidth = maxWidth.value
        val cardContentWidth = (screenWidth - 50f) / 7f - 10f

        // 2. 匹配档位名称
        val levelName = when {
            cardContentWidth < 36f -> "极窄屏"
            cardContentWidth < 48f -> "窄屏"
            cardContentWidth < 68f -> "中屏"
            else -> "宽屏"
        }

        Column(modifier = Modifier) {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentTime,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentWeek,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dayFormat,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.width(56.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "菜单",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "菜单",
                                style = AppTextStyle.menuIconLabel
                            )
                        }
                    }
                },
                // ✨ 新增 actions：在导航栏右上角渲染档位标签
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = levelName,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
            WeekHeaderRow()
        }
    }
}

// ─────────────────────────────────────────────
// 侧边抽屉（含学期开始日期选择器）
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationDrawer(
    drawerState: DrawerState,
    currentScheduleName: String,
    onToggleSchedule: (Boolean) -> Unit,
    classBeginTime: Long?,
    onClassBeginTimeChange: (Long) -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = classBeginTime,
        initialDisplayMode = DisplayMode.Input
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { onClassBeginTimeChange(it) }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true,
                title = {
                    Text(
                        text = "设置上课日期  (点击日历图标可以切换显示)",
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                headline = {
                    DatePickerDefaults.DatePickerHeadline(
                        selectedDateMillis = datePickerState.selectedDateMillis,
                        displayMode = datePickerState.displayMode,
                        dateFormatter = remember { DatePickerDefaults.dateFormatter() },
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, bottom = 12.dp)
                    )
                }
            )
        }
    }

    BoxWithConstraints {
        // 抽屉宽度：屏幕 75%，最大不超过 360dp（适配平板不过宽）
        val drawerWidth = (maxWidth * 0.75f).coerceAtMost(360.dp)

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(drawerWidth),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "功能菜单",
                        modifier = Modifier.padding(
                            start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    Text(
                        "学期配置",
                        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(text = "上课日期", fontWeight = FontWeight.Medium)
                                Text(
                                    text = if (classBeginTime != null)
                                        formatDateMs(classBeginTime)
                                    else
                                        "点击设置",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (classBeginTime != null)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        selected = false,
                        onClick = { showDatePicker = true },
                        icon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                "选择上课日期",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Text(
                        "校区配置",
                        modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    listOf(
                        "安宁校区" to Icons.Default.Landscape,
                        "龙泉校区" to Icons.Default.Apartment
                    ).forEach { (name, icon) ->
                        val isSelected = currentScheduleName == name
                        NavigationDrawerItem(
                            label = {
                                Text(
                                    text = name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = isSelected,
                            onClick = {
                                onToggleSchedule(name == "安宁校区")
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(icon, contentDescription = null) },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                unselectedContainerColor = Color.Transparent,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            },
            content = content
        )
    }
}