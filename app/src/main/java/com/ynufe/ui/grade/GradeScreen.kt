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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ynufe.data.room.grade.GradeEntity
import com.ynufe.ui.theme.GradeLayout
import com.ynufe.utils.GradeUiState

// ─────────────────────────────────────────────────────────────────
// 入口：从 ViewModel 收集 uiState 并分发
// ─────────────────────────────────────────────────────────────────

@Composable
fun GradeScreen(gradeViewModel: GradeViewModel = hiltViewModel()) {
    val uiState by gradeViewModel.uiState.collectAsState()
    val searchQuery by gradeViewModel.searchQuery.collectAsState()
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
        contentPadding = PaddingValues(GradeLayout.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(GradeLayout.ItemSpacing)
    ) {
        // 顶部公共 UI：标题、搜索框、筛选按钮
        item {
            Column {
                Text(
                    text = "学业成绩单",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(GradeLayout.TitleToSearchSpacing))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索课程...") },
                    shape = RoundedCornerShape(GradeLayout.SearchBarCorner)
                )

                Spacer(modifier = Modifier.height(GradeLayout.SearchToFilterSpacing))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(GradeLayout.FilterChipSpacing)
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
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(GradeLayout.EmptySearchIconSize / 2.67f)
                                )
                            }
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
                            {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(GradeLayout.EmptySearchIconSize / 2.67f)
                                )
                            }
                        } else null
                    )
                }
            }
        }

        if (grades.isEmpty()) {
            // 搜索/筛选结果为空时的局部占位图
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = GradeLayout.EmptySearchTopPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(GradeLayout.EmptySearchIconSize),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(GradeLayout.EmptySearchIconToTextSpacing))
                    Text(
                        text = "未找到相关成绩记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(grades) { grade ->
                GradeItemCard(grade)
            }
        }
    }
}

@Composable
fun GradeItemCard(grade: GradeEntity) {
    val scoreValue = grade.score.toDoubleOrNull() ?: 0.0
    val isPassed = scoreValue >= 60.0

    val scoreColor = if (isPassed) {
        Color(0xFF008000)
    } else {
        MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = GradeLayout.CardElevation)
    ) {
        Row(
            modifier = Modifier
                .padding(GradeLayout.ContentPadding)
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
            .padding(GradeLayout.ContentPadding),
        verticalArrangement = Arrangement.spacedBy(GradeLayout.ItemSpacing)
    ) {
        repeat(6) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GradeLayout.SkeletonCardHeight),
                shape = RoundedCornerShape(GradeLayout.SkeletonCardCorner),
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
            modifier = Modifier.size(GradeLayout.EmptyStateIconSize),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(GradeLayout.EmptyStateIconToTitleSpacing))
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
        Spacer(modifier = Modifier.height(GradeLayout.EmptyStateIconToTitleSpacing / 2))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}