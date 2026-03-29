package com.ynufe.utils.wlan

import com.google.gson.Gson
import java.lang.reflect.Type
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit

class JsonpConverterFactory(private val gson: Gson) : Converter.Factory() {
    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        return Converter<ResponseBody, Any> { value ->
            val raw = value.string()
            // 核心脱壳逻辑：只取 {} 之间的部分
            val startIndex = raw.indexOf("{")
            val endIndex = raw.lastIndexOf("}")

            val json = if (startIndex != -1 && endIndex != -1) {
                raw.substring(startIndex, endIndex + 1)
            } else {
                raw
            }
            // 将脱壳后的纯 JSON 交给原生的 Gson 解析器
            gson.fromJson(json, type)
        }
    }
}