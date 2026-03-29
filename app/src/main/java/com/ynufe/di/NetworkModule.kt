package com.ynufe.di

import android.annotation.SuppressLint
import android.content.Context
import coil.ImageLoader
import com.google.gson.Gson
import com.ynufe.data.api.AppApi
import com.ynufe.data.api.VersionApi
import com.ynufe.data.api.WlanApi
import com.ynufe.utils.wlan.JsonpConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * 自定义 Cookie 管理器：实现自动接力 JSESSIONID 和 jsxsd
 */
class MemoryCookieJar : CookieJar {
    private val cookieStore = mutableMapOf<String, MutableSet<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(this) {
            val host = url.host
            val hostCookies = cookieStore.getOrPut(host) { mutableSetOf() }
            cookies.forEach { newCookie ->
                hostCookies.remove(newCookie)
                hostCookies.add(newCookie)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(this) {
            val host = url.host
            val hostCookies = cookieStore[host] ?: return emptyList()
            return hostCookies.filter { it.matches(url) }
        }
    }

    fun clear() {
        synchronized(this) {
            cookieStore.clear()
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMemoryCookieJar(): MemoryCookieJar {
        return MemoryCookieJar()
    }

    /**
     * 1. 教务系统专用的 OkHttpClient
     * 配置：跳过 SSL 认证，启用 CookieJar
     */
    @Provides
    @Singleton
    @Named("AppOkHttp")
    fun provideAppOkHttpClient(memoryCookieJar: MemoryCookieJar): OkHttpClient {
        val trustAllCerts = @SuppressLint("CustomX509TrustManager")
        object : X509TrustManager {
            @SuppressLint("TrustAllX509TrustManager")
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            @SuppressLint("TrustAllX509TrustManager")
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, arrayOf<TrustManager>(trustAllCerts), SecureRandom())
        }

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts)
            .hostnameVerifier { _, _ -> true }
            .cookieJar(memoryCookieJar)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * 2. 通用/外部 API 专用的 OkHttpClient (用于 Wlan 和 Gitee)
     * 配置：不跳过 SSL 认证，不配置 CookieJar (即 CookieJar.NO_COOKIES)
     */
    @Provides
    @Singleton
    @Named("DefaultOkHttp")
    fun provideDefaultOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            // 默认 SSL 验证
            // 默认无 Cookie
            .build()
    }

    // --- Retrofit 实例配置 ---

    /**
     * 教务系统 Retrofit
     * 特点：使用 Scalars (处理网页源码) + Gson，绑定 AppOkHttp
     */
    @Provides
    @Singleton
    @Named("AppRetrofit")
    fun provideAppRetrofit(
        @Named("AppOkHttp") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://xjwis.ynufe.edu.cn/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * 校园网认证 Retrofit
     * 特点：必须使用 JsonpConverterFactory，绑定 DefaultOkHttp
     */
    @Provides
    @Singleton
    @Named("WlanRetrofit")
    fun provideWlanRetrofit(
        @Named("DefaultOkHttp") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://172.16.130.31/")
            .client(okHttpClient)
            .addConverterFactory(JsonpConverterFactory(gson))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * 版本更新 (Gitee) Retrofit
     * 特点：标准配置，绑定 DefaultOkHttp
     */
    @Provides
    @Singleton
    @Named("VersionRetrofit")
    fun provideVersionRetrofit(
        @Named("DefaultOkHttp") okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://gitee.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // --- API 接口提供 ---

    @Provides
    @Singleton
    fun provideAppApi(@Named("AppRetrofit") retrofit: Retrofit): AppApi {
        return retrofit.create(AppApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWlanApi(@Named("WlanRetrofit") retrofit: Retrofit): WlanApi {
        return retrofit.create(WlanApi::class.java)
    }

    @Provides
    @Singleton
    fun provideVersionApi(@Named("VersionRetrofit") retrofit: Retrofit): VersionApi {
        return retrofit.create(VersionApi::class.java)
    }

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        @Named("AppOkHttp") okHttpClient: OkHttpClient
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)
            .build()
    }
}