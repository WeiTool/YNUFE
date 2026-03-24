package com.ynufe.ui.grade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ynufe.data.room.grade.GradeEntity
import com.ynufe.utils.GradeUiState

// ─────────────────────────────────────────────────────────────────
// 入口：从 ViewModel 收集 uiState 并分发
// ─────────────────────────────────────────────────────────────────

@Composable
fun GradeScreen(gradeViewModel: GradeViewModel = hiltViewModel()) {
    // 1. 收集 UI 状态
    val uiState by gradeViewModel.uiState.collectAsState()
    // 2. 收集搜索文本状态
    val searchQuery by gradeViewModel.searchQuery.collectAsState()
    // 3. 收集当前的筛选模式状态 (全部/及格/不及格)
    val filterStatus by gradeViewModel.filterStatus.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is GradeUiState.Loading -> GradeSkeletonList()
            is GradeUiState.Empty -> EmptyGradeContent()
            is GradeUiState.Success -> GradeScreenContent(
                grades = state.grades,
                searchQuery = searchQuery,
                currentFilter = filterStatus,
                onQueryChange = { gradeViewModel.onSearchQueryChange(it) },
                onFilterChange = { gradeViewModel.onFilterChange(it) }
            )
            is GradeUiState.Error -> GradeErrorContent(state.message)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// GradeUiState.Success → 成绩列表
// ─────────────────────────────────────────────────────────────────

@Composable
fun GradeScreenContent(
    grades: List<GradeEntity>,
    searchQuery: String,
    currentFilter: GradeFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (GradeFilter) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 顶部公共 UI：标题、搜索框、筛选按钮
        item {
            Column {
                Text(
                    text = "学业成绩单",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 搜索框
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索课程...") },
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentFilter == GradeFilter.ALL,
                        onClick = { onFilterChange(GradeFilter.ALL) },
                        label = { Text("全部") }
                    )
                    FilterChip(
                        selected = currentFilter == GradeFilter.PASSED,
                        onClick = { onFilterChange(GradeFilter.PASSED) },
                        label = { Text("过的") },
                        leadingIcon = if (currentFilter == GradeFilter.PASSED) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = currentFilter == GradeFilter.FAILED,
                        onClick = { onFilterChange(GradeFilter.FAILED) },
                        label = { Text("挂的") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedLabelColor = MaterialTheme.colorScheme.error,
                            selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        leadingIcon = if (currentFilter == GradeFilter.FAILED) {
                            { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                }
            }
        }

        // 列表内容判断
        if (grades.isEmpty()) {
            // 当搜索或筛选导致结果为空时，显示局部占位图
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "未找到相关成绩记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            // 有数据时正常渲染列表
            items(grades) { grade ->
                GradeItemCard(grade)
            }
        }
    }
}
@Composable
fun GradeItemCard(grade: GradeEntity) {
    // 将字符串分数转为数字，转换失败则默认为 0
    val scoreValue = grade.score.toDoubleOrNull() ?: 0.0
    // 判断是否及格
    val isPassed = scoreValue >= 60.0

    // 根据结果选择颜色
    val scoreColor = if (isPassed) {
        Color(0xFF008000)
    } else {
        // 不及格显示红色
        MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = grade.courseName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${grade.courseType} | 学分: ${grade.credit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = grade.score,
                    style = MaterialTheme.typography.headlineSmall,
                    color = scoreColor,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "绩点: ${grade.gradePoint}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// GradeUiState.Loading → 骨架屏
// ─────────────────────────────────────────────────────────────────

@Composable
fun GradeSkeletonList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(6) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {}
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// GradeUiState.Empty → 暂无成绩
// ─────────────────────────────────────────────────────────────────

@Composable
fun EmptyGradeContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "暂无成绩记录",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "请前往用户页面更新数据",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// GradeUiState.Error → 错误提示
// ─────────────────────────────────────────────────────────────────

@Composable
fun GradeErrorContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "加载失败",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}