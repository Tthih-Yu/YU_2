package com.tthih.yu.campuscard

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// Base URL for WebVPN
private const val BASE_URL = "https://webvpn.ahpu.edu.cn/"

// In-memory cookie jar for simplicity
class InMemoryCookieJar : CookieJar {
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val storedCookies = cookieStore.getOrPut(host) { mutableListOf() }
        
        // Add or update cookies
        cookies.forEach { newCookie ->
            // Remove existing cookie with the same name
            storedCookies.removeAll { it.name == newCookie.name }
            storedCookies.add(newCookie)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val storedCookies = cookieStore[host] ?: emptyList<Cookie>()
        
        // Filter out expired cookies
        val currentTime = System.currentTimeMillis()
        return storedCookies.filter { it.expiresAt > currentTime }
    }

    fun clear() {
        cookieStore.clear()
    }

    fun getAllCookies(): Map<String, List<Cookie>> {
        return cookieStore.toMap() // Return a copy
    }
}

object NetworkModule {
    private val cookieJar = InMemoryCookieJar()

    // 在 Release 构建中使用 NONE 级别的日志拦截器
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // 修复 BuildConfig.DEBUG 引用问题，直接使用 NONE 级别
        level = HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(loggingInterceptor)
        .followRedirects(true) // Follow redirects automatically
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create()) // Use Gson, ensure Gson dependency is added
        .build()

    val apiService: CampusCardApiService = retrofit.create(CampusCardApiService::class.java)

    fun clearCookies() {
        cookieJar.clear()
    }

    // Function to manually get cookies if needed
    fun getCookiesForUrl(url: String): List<Cookie> {
        val httpUrl = url.toHttpUrlOrNull()
        return if (httpUrl != null) {
            cookieJar.loadForRequest(httpUrl)
        } else {
            emptyList()
        }
    }
} 