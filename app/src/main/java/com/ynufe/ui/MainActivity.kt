package com.ynufe.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ynufe.data.repository.UpdateResult
import com.ynufe.ui.course.CourseScreen
import com.ynufe.ui.grade.GradeScreen
import com.ynufe.ui.theme.YNUFETheme
import com.ynufe.ui.user.UserScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()
    private val checkVersionViewModel: CheckVersionViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        checkVersionViewModel.checkForUpdates()

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
}

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
                    // 跳转浏览器下载
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    context.startActivity(intent)
                    viewModel.dismissDialog()
                }
            )
        }
        is UpdateResult.Error -> {
            // 加 Log 或 Toast 看具体错误原因
            Log.e("CheckVersion", updateState.message)
        }
        else -> {} // NoUpdate 或 Error 时不处理
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
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
                when (currentDestination) {
                    AppDestinations.DateRange -> CourseScreen()
                    AppDestinations.Leaderboard -> GradeScreen()
                    AppDestinations.PROFILE -> UserScreen()
                }
            }
        }
    }
}
enum class AppDestinations(val label: String, val icon: ImageVector) {
    DateRange("课程表", Icons.Default.DateRange),
    Leaderboard("成绩", Icons.Default.Leaderboard),
    PROFILE("用户", Icons.Default.Person),
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