package com.ynufe.data.model

import com.google.gson.annotations.SerializedName

data class TokenResponse(
    @SerializedName("challenge") val challenge: String,
    @SerializedName("online_ip") val onlineIp: String
)

data class LoginResponse(
    @SerializedName("error") val error: String? = null,
    @SerializedName("error_msg") val errorMsg: String? = null,
    @SerializedName("ploy_msg") val ployMsg: String? = null,
    @SerializedName("suc_msg") val sucMsg: String? = null
)

data class LogoutResponse(
    @SerializedName("error") val error: String? = null,
    @SerializedName("error_msg") val errorMsg: String? = null,
)

data class WlanUserInfoResponse(
    @SerializedName("online_ip") val onlineIp: String,
    @SerializedName("online_device_total") val onlineUser: String? = null
)