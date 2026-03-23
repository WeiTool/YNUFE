package com.ynufe.ui.course

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ynufe.data.room.course.CourseEntity
import com.ynufe.ui.theme.COURSE_COLORS
import com.ynufe.utils.CourseUiState
import com.ynufe.utils.DateUtils
import com.ynufe.utils.DateUtils.formatDateMs
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

    // semesterStartMs 在有用户时才有意义，供抽屉组件读取
    val semesterStartMs = when (val s = uiState) {
        is CourseUiState.Success -> s.semesterStartMs
        is CourseUiState.Empty -> s.semesterStartMs
        else -> null
    }

    AppNavigationDrawer(
        drawerState = drawerState,
        currentScheduleName = viewModel.currentScheduleName,
        onToggleSchedule = { isAnning -> viewModel.toggleSchedule(isAnning) },
        semesterStartMs = semesterStartMs,
        onSemesterStartChange = { newTime -> viewModel.updateSemesterStart(newTime) }
    ) {
        when (val state = uiState) {
            // 数据库初始化中：显示骨架屏
            is CourseUiState.Loading -> CourseLoadingContent(
                onMenuClick = { scope.launch { drawerState.open() } }
            )

            // 未绑定账号：引导空状态
            is CourseUiState.NoUser -> CourseNoUserContent(
                onMenuClick = { scope.launch { drawerState.open() } }
            )

            // 已绑定账号但无课表：空网格 + 提示
            is CourseUiState.Empty -> CourseNoUserContent(
                onMenuClick = { scope.launch { drawerState.open() } }
            )

            // 课表加载成功：正常渲染
            is CourseUiState.Success -> {
                val currentWeekInt = DateUtils.getCurrentWeekInt(state.semesterStartMs)
                val weekDisplay = DateUtils.getWeekDescription(currentWeekInt)
                val filteredCourses = remember(state.courses, currentWeekInt) {
                    if (currentWeekInt == -1) state.courses
                    else state.courses.filter {
                        DateUtils.isCourseInCurrentWeek(it.weeks, currentWeekInt)
                    }
                }
                CourseScreenContent(
                    currentTime = currentTime,
                    dayName = dayName,
                    currentWeek = weekDisplay,
                    timeSlots = viewModel.currentTimeSlots,
                    courses = filteredCourses,
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
// CourseUiState.Success / Empty → 课表主内容
// ─────────────────────────────────────────────

@Composable
fun CourseScreenContent(
    currentTime: String,
    dayName: String,
    currentWeek: String,
    timeSlots: List<TimeSlot>,
    courses: List<CourseEntity>,
    onMenuClick: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {
        CourseTopAppBar(
            currentTime = currentTime,
            currentWeek = currentWeek,
            dayFormat = dayName,
            onMenuClick = onMenuClick,
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            TimeColumn(timeSlots = timeSlots)
            CourseTable(
                courses = courses,
                timeSlots = timeSlots,
                modifier = Modifier.weight(1f)
            )
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
                        fontSize = 12.sp,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) Color.Red
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    Text(text = "${slot.section}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = slot.start, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = slot.end, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
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
        repeat(totalRows) { row ->
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = CELL_HEIGHT * row),
                thickness = 0.3.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
        courses.forEach { course ->
            val topOffset = CELL_HEIGHT * (course.startSection - 1)
            val cardHeight = CELL_HEIGHT * course.rowSpan
            CourseCard(
                course = course,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .offset(y = topOffset)
                    .padding(2.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────
// 课程卡片
// ─────────────────────────────────────────────

@Composable
fun CourseCard(course: CourseEntity, modifier: Modifier = Modifier) {
    val bgColor = courseColor(course.name)
    val isTall = course.rowSpan >= 2

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor.copy(alpha = 0.9f))
            .padding(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = if (isTall) Arrangement.spacedBy(4.dp) else Arrangement.Top
        ) {
            Text(text = course.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 16.sp, overflow = TextOverflow.Ellipsis)
            if (isTall) {
                if (course.room.isNotEmpty()) Text(text = course.room, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, lineHeight = 13.sp)
                if (course.teacher.isNotEmpty()) Text(text = course.teacher, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
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
    Column(modifier = Modifier) {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = currentTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = currentWeek, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = dayFormat, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.width(56.dp) // 稍微加宽一点
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "菜单",
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "菜单",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp
                        )
                    }
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0)
        )
        WeekHeaderRow()
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
    semesterStartMs: Long?,
    onSemesterStartChange: (Long) -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = semesterStartMs,
        initialDisplayMode = DisplayMode.Input
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { onSemesterStartChange(it) }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
                title = {
                    Text(
                        text = "设置上课日期",
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
        val drawerWidth = (maxWidth * 0.75f).coerceAtMost(360.dp)

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(drawerWidth),
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("功能菜单", modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp), style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    Text("学期配置", modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                    NavigationDrawerItem(
                        label = {
                            Column {
                                Text(text = "上课日期", fontWeight = FontWeight.Medium)
                                Text(
                                    text = if (semesterStartMs != null) formatDateMs(semesterStartMs) else "点击设置",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (semesterStartMs != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        selected = false,
                        onClick = { showDatePicker = true },
                        icon = { Icon(Icons.Default.CalendarMonth, "选择上课日期", tint = MaterialTheme.colorScheme.primary) },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Text("校区配置", modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                    listOf("安宁校区" to Icons.Default.Landscape, "龙泉校区" to Icons.Default.Apartment)
                        .forEach { (name, icon) ->
                            val isSelected = currentScheduleName == name
                            NavigationDrawerItem(
                                label = { Text(text = name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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