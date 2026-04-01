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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ynufe.data.room.course.CourseEntity
import com.ynufe.ui.theme.COURSE_COLORS
import com.ynufe.ui.theme.type.CourseLayout
import com.ynufe.ui.theme.type.CourseTextStyle
import com.ynufe.ui.theme.type.rememberSafeCourseCardStyles
import com.ynufe.utils.CourseUiState
import com.ynufe.utils.DateUtils
import com.ynufe.utils.DateUtils.formatDateMs
import com.ynufe.utils.toHalfWidth
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.abs


// ─────────────────────────────────────────────
// 辅助：课程名映射到背景色
// ─────────────────────────────────────────────

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
        onClassBeginTimeChange = { newTime -> viewModel.updateClassBeginTime(newTime) }
    ) {
        when (val state = uiState) {
            is CourseUiState.Loading -> CourseLoadingContent(
                onMenuClick = { scope.launch { drawerState.open() } }
            )
            is CourseUiState.NoUser -> CourseNoUserContent(
                onMenuClick = { scope.launch { drawerState.open() } }
            )
            is CourseUiState.Empty -> CourseNoUserContent(
                onMenuClick = { scope.launch { drawerState.open() } }
            )
            is CourseUiState.Success -> {
                CourseScreenContent(
                    currentTime = currentTime,
                    dayName = dayName,
                    timeSlots = viewModel.currentTimeSlots,
                    courses = state.courses,
                    semesterStartMs = state.classBeginTime ?: 0L,
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
                .padding(
                    horizontal = CourseLayout.SkeletonPaddingH,
                    vertical = CourseLayout.SkeletonPaddingV
                ),
            verticalArrangement = Arrangement.spacedBy(CourseLayout.SkeletonSpacing)
        ) {
            repeat(7) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CourseLayout.SkeletonCardHeight)
                        .clip(RoundedCornerShape(CourseLayout.SkeletonCardCorner))
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
                    modifier = Modifier.size(CourseLayout.EmptyStateIconSize),
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
    val pageCount = 18
    val actualCurrentWeek = DateUtils.getCurrentWeekInt(semesterStartMs)
    val initialPage = (actualCurrentWeek - 1).coerceIn(0, pageCount - 1)

    val pagerState = rememberPagerState(initialPage = initialPage) { pageCount }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
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

            // 当前页面不是本周时显示回到本周的 FAB
            this@Column.AnimatedVisibility(
                visible = pagerState.currentPage != initialPage,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = CourseLayout.FabPaddingEnd,
                        bottom = CourseLayout.FabPaddingBottom
                    )
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(initialPage) }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(CourseLayout.FabCorner),
                    modifier = Modifier.size(CourseLayout.FabSize)
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
        Spacer(modifier = Modifier.width(CourseLayout.TimeColWidth))

        dayNames.forEachIndexed { index, name ->
            val isToday = index == todayIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = CourseLayout.WeekHeaderPaddingV),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CourseLayout.WeekHeaderInnerSpacing)
                ) {
                    Text(
                        text = name,
                        // CourseTextStyle 统一取样式；今日列动态覆盖 fontWeight
                        style = CourseTextStyle.weekDayLabel,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                        color = if (isToday) Color.Red
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 今日列下方红色高亮条
                    Box(
                        modifier = Modifier
                            .width(CourseLayout.TodayIndicatorWidth)
                            .height(CourseLayout.TodayIndicatorHeight)
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
            .width(CourseLayout.TimeColWidth)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        timeSlots.forEach { slot ->
            Box(
                modifier = Modifier
                    .height(CourseLayout.CellHeight)
                    .fillMaxWidth()
                    .border(0.3.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 节次编号，如 "1"、"12"
                    Text(
                        text = "${slot.section}",
                        style = CourseTextStyle.timeSlotSection,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // 上课时间
                    Text(
                        text = slot.start,
                        style = CourseTextStyle.timeSlotTime,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // 下课时间，稍加透明度以形成层次
                    Text(
                        text = slot.end,
                        style = CourseTextStyle.timeSlotTime,
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
    val columnHeight = CourseLayout.CellHeight * totalRows

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
                    .offset(y = CourseLayout.CellHeight * row),
                thickness = 0.3.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
        // 渲染课程卡片
        courses.forEach { course ->
            val topOffset = CourseLayout.CellHeight * (course.startSection - 1)
            val cardHeight = CourseLayout.CellHeight * course.rowSpan
            CourseCard(
                course = course,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardHeight)
                    .offset(y = topOffset)
                    .padding(CourseLayout.DayColumnCardPadding)
            )
        }
    }
}

// ─────────────────────────────────────────────
// 课程卡片
//
// 设计原则：
//   1. BoxWithConstraints 同时获取实际内容宽度和高度
//   2. rememberSafeCourseCardStyles() 按宽度选档，并根据系统 fontScale
//      自动收缩字号，防止 2K 高密度屏/用户放大字体时溢出卡片
//   3. titleMaxLines 依据 rowSpan 和卡片宽度动态决定
//   4. toHalfWidth() 将全角标点转为半角，节省宝贵像素
//   5. maxLines + TextOverflow.Ellipsis 最终兜底，绝对不越界
// ─────────────────────────────────────────────

@Composable
fun CourseCard(course: CourseEntity, modifier: Modifier = Modifier) {
    val bgColor = courseColor(course.name)

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(CourseLayout.CardCorner))
            .background(bgColor.copy(alpha = 0.9f))
            .padding(
                horizontal = CourseLayout.CardPaddingH,
                vertical = CourseLayout.CardPaddingV
            )
    ) {
        val widthDp  = maxWidth.value
        val heightDp = maxHeight.value

        // ── 决定课程名最大行数 ────────────────────────────────────
        // rowSpan >= 2：高度 ≈ 148dp，宽裕，允许 3 行
        // rowSpan = 1 且宽度 >= 48dp（MD/LG）：字号较大，1 行即可
        // rowSpan = 1 且宽度 < 48dp（XS/SM）：需换行才能显示完整课名
        val titleMaxLines = when {
            course.rowSpan >= 2 -> 3
            widthDp >= 48f      -> 1
            else                -> 2
        }

        val styles = rememberSafeCourseCardStyles(
            cardWidthDp   = widthDp,
            cardHeightDp  = heightDp,
            titleMaxLines = titleMaxLines,
        )

        val isTall = course.rowSpan >= 2

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = if (isTall) Arrangement.spacedBy(2.dp) else Arrangement.Top
        ) {
            Text(
                text     = course.name.toHalfWidth(),
                style    = styles.title,
                color    = Color.White,
                maxLines = titleMaxLines,
                overflow = TextOverflow.Ellipsis,
            )

            if (course.room.isNotEmpty()) {
                Text(
                    text     = course.room.toHalfWidth(),
                    style    = styles.room,
                    color    = Color.White.copy(alpha = 0.92f)
                )
            }

            if (course.teacher.isNotEmpty()) {
                Text(
                    text     = course.teacher.toHalfWidth(),
                    style    = styles.teacher,
                    color    = Color.White.copy(alpha = 0.80f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // 计算当前卡片内容宽度（逻辑与 CourseType.kt 档位断点同步）
        val screenWidth = maxWidth.value
        val cardContentWidth = (screenWidth - CourseLayout.TimeColWidth.value) / 7f - 10f

        val levelName = when {
            cardContentWidth < 36f -> "极窄屏"
            cardContentWidth < 48f -> "窄屏"
            cardContentWidth < 68f -> "中屏"
            else -> "宽屏"
        }

        Column {
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
                        modifier = Modifier.width(CourseLayout.MenuIconButtonWidth)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "菜单",
                                modifier = Modifier.size(CourseLayout.MenuIconSize)
                            )
                            Text(
                                text = "菜单",
                                style = CourseTextStyle.menuIconLabel
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = CourseLayout.LevelBadgePaddingEnd)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(CourseLayout.LevelBadgeCorner)
                            )
                            .padding(
                                horizontal = CourseLayout.LevelBadgePaddingH,
                                vertical = CourseLayout.LevelBadgePaddingV
                            )
                    ) {
                        Text(
                            text = levelName,
                            style = CourseTextStyle.levelBadge,
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
        val drawerWidth = (maxWidth * 0.75f).coerceAtMost(CourseLayout.DrawerMaxWidth)

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
                        shape = RoundedCornerShape(CourseLayout.DrawerItemCorner),
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
                            shape = RoundedCornerShape(CourseLayout.DrawerItemCorner),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            },
            content = content
        )
    }
}

// ─────────────────────────────────────────────
// 预览数据
// ─────────────────────────────────────────────

object CoursePreviewData {
    val mockTimeSlots = listOf(
        TimeSlot(1, "08:30", "09:15"),
        TimeSlot(2, "09:20", "10:05"),
        TimeSlot(3, "10:25", "11:10"),
        TimeSlot(4, "11:15", "12:00"),
        TimeSlot(5, "14:00", "14:45")
    )

    val mockCourses = listOf(
        CourseEntity(
            id = 1, name = "高等数学", room = "教A-302", teacher = "张教授",
            dayOfWeek = 1, startSection = 1, rowSpan = 2, weeks = "1-16",
            studentId = "11111"
        ),
        CourseEntity(
            id = 2, name = "移动终端开发实践", room = "实训楼 504", teacher = "李老师",
            dayOfWeek = 2, startSection = 3, rowSpan = 2, weeks = "1-16",
            studentId = "11111"
        ),
        CourseEntity(
            id = 3, name = "大学英语(III)", room = "教C-101", teacher = "Wang.J",
            dayOfWeek = 5, startSection = 1, rowSpan = 2, weeks = "1-16",
            studentId = "11111"
        )
    )
}

@Composable
fun CoursePreviewWrapper(widthDp: Int) {
    MaterialTheme {
        Surface(modifier = Modifier.width(widthDp.dp)) {
            CourseScreenContent(
                currentTime = "2026-03-24",
                dayName = "星期二",
                timeSlots = CoursePreviewData.mockTimeSlots,
                courses = CoursePreviewData.mockCourses,
                semesterStartMs = System.currentTimeMillis(),
                onMenuClick = {}
            )
        }
    }
}