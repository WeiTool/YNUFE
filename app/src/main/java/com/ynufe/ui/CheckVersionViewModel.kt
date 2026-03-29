package com.ynufe.ui

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ynufe.data.repository.CheckVersionRepository
import com.ynufe.utils.UpdateResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckVersionViewModel @Inject constructor(
    private val repository: CheckVersionRepository
) : ViewModel() {

    // UI 观察的状态
    var updateState by mutableStateOf<UpdateResult>(UpdateResult.NoUpdate)
        private set

    fun checkForUpdatesIfNeed() {
        viewModelScope.launch {
            // 先判断时间，如果不到 6 小时，直接 return 不走网络请求
            if (repository.shouldCheckUpdate()) {
                updateState = repository.checkUpdate()
            } else {
                Log.d("Update", "距离上次检查不足 6 小时，跳过请求")
            }
        }
    }

    fun forceCheckForUpdates() {
        viewModelScope.launch {
            // 直接调用 repository.checkUpdate()，跳过 shouldCheckUpdate() 的判断
            updateState = repository.checkUpdate(force = true)
        }
    }

    fun dismissDialog() {
        updateState = UpdateResult.NoUpdate
    }
}