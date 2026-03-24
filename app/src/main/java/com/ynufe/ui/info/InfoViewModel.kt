package com.ynufe.ui.info

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InfoViewModel @Inject constructor(

) : ViewModel() {

    // 当前 App 的版本号
    var currentVersion by mutableStateOf("未获取")
        private set

    var feedbackText by mutableStateOf("")
    var selectedImages = mutableStateListOf<android.net.Uri>()

    fun sendFeedback() {
        if (feedbackText.isBlank()) return

        viewModelScope.launch {
            // 这里建议调用你自己的 API 接口
            // 接口逻辑：接收文字和图片文件 -> 后端使用 JavaMail 发送邮件到你的邮箱
            // val success = repository.submitFeedback(feedbackText, selectedImages)
        }
    }

}

enum class Type {
    UPDATE, FEEDBACK
}