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

    // 当前 App 的版本号
    var currentVersion by mutableStateOf("未获取")
        private set

    init {
        // 初始化时获取当前版本
        getCurrentVersion()
    }

    private fun getCurrentVersion() {
        viewModelScope.launch {
            // 调用 Repository 获取当前版本
            val version = checkVersionRepository.getCurrentVersionName()
            currentVersion = version.ifEmpty { "未知版本" }
        }
    }
}

enum class Type {
    UPDATE, FEEDBACK
}