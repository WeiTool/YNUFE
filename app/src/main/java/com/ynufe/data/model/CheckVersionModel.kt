package com.ynufe.data.model

import com.google.gson.annotations.SerializedName

data class CheckVersionResponse(
    @SerializedName("name") val name: String,
    @SerializedName("body") val body: String,
    @SerializedName("assets") val assets: List<Asset>
)

data class Asset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val browserDownloadUrl: String
)