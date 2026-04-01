package com.ynufe.ui.info

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynufe.data.repository.CheckVersionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor(
    private val checkVersionRepository: CheckVersionRepository
) : ViewModel() {

    // ─────────────────────────────────────────────────────────────────
    // 版本信息：App 当前版本号
    //   使用 Compose State 而非 StateFlow，因为该值只在初始化时写入一次，
    //   无需 Flow 的响应式能力，Compose 直接重组即可
    // ─────────────────────────────────────────────────────────────────

    var currentVersion by mutableStateOf("未获取")
        private set

    // ─────────────────────────────────────────────────────────────────
    // 初始化：ViewModel 创建时立即获取版本号，避免 UI 显示"未获取"占位文本
    // ─────────────────────────────────────────────────────────────────

    init {
        loadCurrentVersion()
    }

    private fun loadCurrentVersion() {
        viewModelScope.launch {
            val version = checkVersionRepository.getCurrentVersionName()
            currentVersion = version.ifEmpty { "未知版本" }
        }
    }
}