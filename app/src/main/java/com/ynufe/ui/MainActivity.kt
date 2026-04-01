package com.ynufe.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ynufe.ui.course.CourseScreen
import com.ynufe.ui.grade.GradeScreen
import com.ynufe.ui.info.InfoScreen
import com.ynufe.ui.theme.YNUFETheme
import com.ynufe.ui.tool.ToolScreen
import com.ynufe.ui.user.UserScreen
import com.ynufe.ui.wlan.WlanScreen
import com.ynufe.utils.UpdateResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val checkVersionViewModel: CheckVersionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition {
            !mainViewModel.isReady.value
        }

        enableEdgeToEdge()
        setContent {
            YNUFETheme {
                YNUFEApp(checkVersionViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        checkVersionViewModel.checkForUpdatesIfNeed()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun YNUFEApp(viewModel: CheckVersionViewModel) {
    var currentDestination by remember { mutableStateOf(AppDestinations.DateRange) }
    val updateState = viewModel.updateState
    val context = LocalContext.current

    when (updateState) {
        is UpdateResult.HasUpdate -> {
            UpdateDialog(
                updateResult = updateState,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { url ->
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    context.startActivity(intent)
                    viewModel.dismissDialog()
                }
            )
        }

        else -> {}
    }

    SharedTransitionLayout {
        val sharedScope = this

        NavigationSuiteScaffold(
            navigationSuiteItems = {
                AppDestinations.entries.filter { it.showInNav }.forEach {
                    item(
                        icon = { Icon(it.icon, contentDescription = it.label) },
                        label = { Text(it.label) },
                        selected = it == currentDestination,
                        onClick = { currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    AnimatedContent(
                        targetState = currentDestination,
                        label = "screen_transition",
                        transitionSpec = {
                            when {
                                // ── 进入 WlanScreen：从屏幕底部向上滑入 ──────────
                                targetState == AppDestinations.WLAN_DETAIL ->
                                    (slideInVertically(tween(380)) { it } + fadeIn(tween(380)))
                                        .togetherWith(fadeOut(tween(200)))

                                // ── 离开 WlanScreen：向下滑出回到 ToolScreen ─────
                                initialState == AppDestinations.WLAN_DETAIL ->
                                    fadeIn(tween(250))
                                        .togetherWith(
                                            slideOutVertically(tween(380)) { it } + fadeOut(
                                                tween(
                                                    380
                                                )
                                            )
                                        )

                                // ── 其余目的地：保持原有的 fade 过渡 ────────────
                                else ->
                                    fadeIn(tween(500)).togetherWith(fadeOut(tween(500)))
                            }
                        }
                    ) { targetDest ->
                        val animatedScope = this

                        when (targetDest) {
                            AppDestinations.DateRange -> CourseScreen()

                            AppDestinations.Handyman -> ToolScreen(
                                sharedTransitionScope = sharedScope,
                                animatedVisibilityScope = animatedScope,
                                onNavigateToGrade = {
                                    currentDestination = AppDestinations.GRADE_DETAIL
                                },
                                onNavigateToWlan = {
                                    currentDestination = AppDestinations.WLAN_DETAIL
                                }
                            )

                            AppDestinations.GRADE_DETAIL -> GradeScreen(
                                sharedTransitionScope = sharedScope,
                                animatedVisibilityScope = animatedScope,
                                onBack = { currentDestination = AppDestinations.Handyman }
                            )

                            // ── 校园网详情页 ──────────────────────────────────
                            AppDestinations.WLAN_DETAIL -> WlanScreen(
                                onBack = { currentDestination = AppDestinations.Handyman }
                            )

                            AppDestinations.PROFILE -> UserScreen()
                            AppDestinations.INFO -> InfoScreen()
                        }
                    }
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
    val showInNav: Boolean = true
) {
    DateRange("课程表", Icons.Default.DateRange),
    Handyman("工具箱", Icons.Default.Handyman),
    PROFILE("用户", Icons.Default.Person),
    INFO("关于", Icons.Default.Info),
    GRADE_DETAIL("成绩单", Icons.Default.Leaderboard, showInNav = false),
    WLAN_DETAIL("校园网", Icons.Default.Wifi, showInNav = false)
}

@Composable
fun UpdateDialog(
    updateResult: UpdateResult.HasUpdate,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "发现新版本: ${updateResult.latestVersion}") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn {
                item {
                    Text(text = "当前版本: ${updateResult.currentVersion}")
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "更新日志:\n${updateResult.releaseNotes}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                updateResult.downloadUrl?.let { onConfirm(it) }
            }) {
                Text("立即下载")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后再说")
            }
        }
    )
}