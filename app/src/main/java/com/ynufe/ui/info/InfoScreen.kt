package com.ynufe.ui.info

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.ynufe.ui.CheckVersionViewModel
import com.ynufe.ui.theme.type.InfoLayout

@Composable
fun InfoScreen(
    checkViewModel: CheckVersionViewModel = hiltViewModel(),
    infoViewModel: InfoViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val feedbackUrl = "https://wj.qq.com/s2/26098416/7c06/"

    InfoContent(
        version = infoViewModel.currentVersion,
        onItemClick = { type ->
            when (type) {
                Type.FEEDBACK -> {
                    val intent = Intent(Intent.ACTION_VIEW, feedbackUrl.toUri())
                    context.startActivity(intent)
                }

                Type.UPDATE -> {
                    // 直接触发 MainActivity 已经在监听的更新逻辑
                    checkViewModel.forceCheckForUpdates()
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoContent(
    version: String,
    onItemClick: (Type) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo 区域
            item {
                Spacer(modifier = Modifier.height(InfoLayout.LogoTopMargin))
                Image(
                    painter = painterResource(id = com.ynufe.R.drawable.ic_launcher_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(InfoLayout.LogoSize)
                )
                Spacer(modifier = Modifier.height(InfoLayout.TitleToVersionSpacing))
                Text(
                    text = "版本 v$version",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(InfoLayout.HeaderBottomMargin))
            }

            // 功能列表项
            item {
                SectionHeader("建议与反馈")
                InfoRowItem(
                    icon = Icons.Default.EditNote,
                    title = "问题反馈",
                    subtitle = "提交建议或 Bug",
                    onClick = { onItemClick(Type.FEEDBACK) }
                )

                Spacer(modifier = Modifier.height(InfoLayout.SectionSpacing))

                SectionHeader("功能区")
                InfoRowItem(
                    icon = Icons.Default.SystemUpdateAlt,
                    title = "检查更新",
                    onClick = { onItemClick(Type.UPDATE) }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = InfoLayout.SectionHeaderHorizontalPadding,
                vertical = InfoLayout.SectionHeaderVerticalPadding
            ),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun InfoRowItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = InfoLayout.CardHorizontalPadding,
                vertical = InfoLayout.CardVerticalPadding
            )
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = InfoLayout.CardBackgroundAlpha),
        shape = RoundedCornerShape(InfoLayout.CardCornerRadius)
    ) {
        Row(
            modifier = Modifier.padding(InfoLayout.CardInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(InfoLayout.MainIconSize)
            )
            Spacer(modifier = Modifier.width(InfoLayout.IconToTextSpacing))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(InfoLayout.ArrowIconSize)
            )
        }
    }
}

@Preview(showBackground = true, name = "浅色模式")
@Composable
fun InfoScreenLightPreview() {
    // 使用你 Theme.kt 中定义的自定义主题
    com.ynufe.ui.theme.YNUFETheme(darkTheme = false) {
        // 预览内容
        InfoContent(
            version = "1.0.0",
            onItemClick = { label -> println("点击了: $label") }
        )
    }
}

@Preview(showBackground = true, name = "深色模式")
@Composable
fun InfoScreenDarkPreview() {
    // 强制开启深色模式测试适配情况
    com.ynufe.ui.theme.YNUFETheme(darkTheme = true) {
        InfoContent(
            version = "1.0.0",
            onItemClick = { }
        )
    }
}