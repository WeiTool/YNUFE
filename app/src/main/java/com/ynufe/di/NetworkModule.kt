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
 * 内存中的Cookie存储管理器
 * 实现OkHttp的CookieJar接口，用于在内存中保存和读取Cookie
 */
class MemoryCookieJar : CookieJar {
    // 存储所有Cookie的容器
    // key: 域名(host), value: 该域名下所有的Cookie集合（使用Set自动去重）
    private val cookieStore = mutableMapOf<String, MutableSet<Cookie>>()

    /**
     * 保存从服务器返回的Cookie
     * @param url 服务器请求的URL
     * @param cookies 服务器返回的Cookie列表
     */
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // 使用同步锁，保证多线程环境下的线程安全
        synchronized(this) {
            // 从URL中获取主机名（域名），例如：www.example.com
            val host = url.host
            // 从cookieStore中获取该域名对应的Cookie集合
            // 如果不存在则创建一个新的可变Set集合并返回
            val hostCookies = cookieStore.getOrPut(host) { mutableSetOf() }

            // 遍历所有需要保存的Cookie
            cookies.forEach { newCookie ->
                // 先移除已存在的相同Cookie（根据domain、path、name等判断）
                // Set会自动根据equals方法判断是否相同
                hostCookies.remove(newCookie)
                // 将新的Cookie添加到集合中
                hostCookies.add(newCookie)
            }
        }
    }

    /**
     * 加载将要发送给服务器的Cookie
     * @param url 将要请求的URL
     * @return 匹配该URL的Cookie列表
     */
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // 使用同步锁，保证多线程环境下的线程安全
        synchronized(this) {
            // 从URL中获取主机名（域名）
            val host = url.host
            // 从cookieStore中获取该域名对应的Cookie集合
            // 如果不存在则直接返回空列表
            val hostCookies = cookieStore[host] ?: return emptyList()

            // 过滤出所有匹配当前URL的Cookie
            // matches()方法会检查：
            // - Cookie的domain是否匹配
            // - Cookie的path是否匹配
            // - Cookie是否过期
            // - Cookie的secure标志等
            return hostCookies.filter { it.matches(url) }
        }
    }

    /**
     * 清空所有存储的Cookie
     * 通常用于用户登出或清除缓存数据时调用
     */
    fun clear() {
        // 使用同步锁，保证多线程环境下的线程安全
        synchronized(this) {
            // 清空整个Cookie存储容器
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