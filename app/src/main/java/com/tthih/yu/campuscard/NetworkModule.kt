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

// Base URL - Changed to the direct campus card system address
private const val BASE_URL = "http://220.178.164.65:8053/"

object NetworkModule {
    // REMOVE old public cookieJar
    // val cookieJar = InMemoryCookieJar() 

    // ADD PersistentCookieJar instance (private)
    private lateinit var persistentCookieJar: PersistentCookieJar

    // ADD initialize function
    fun initialize(context: Context) {
        if (!::persistentCookieJar.isInitialized) { // Prevent re-initialization
            persistentCookieJar = PersistentCookieJar(context.applicationContext)
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient by lazy {
        if (!::persistentCookieJar.isInitialized) {
            throw IllegalStateException("NetworkModule must be initialized before using OkHttpClient")
        }
        OkHttpClient.Builder()
            // USE PersistentCookieJar
            .cookieJar(persistentCookieJar)
        .addInterceptor(loggingInterceptor)
            .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
        .build()
    }

    val apiService: CampusCardApiService by lazy {
        retrofit.create(CampusCardApiService::class.java)
    }

    fun clearCookies() {
        if (::persistentCookieJar.isInitialized) {
            persistentCookieJar.clear() // Clear persistent cookies
        } else {
            // Log or handle the case where clear is called before init? 
            // For now, it just won't do anything if not initialized.
        }
    }

    // Function to manually get cookies if needed (Now uses persistent jar)
    fun getCookiesForUrl(url: String): List<Cookie> {
        if (!::persistentCookieJar.isInitialized) return emptyList()
        val httpUrl = url.toHttpUrlOrNull()
        return if (httpUrl != null) {
            persistentCookieJar.loadForRequest(httpUrl)
        } else {
            emptyList()
        }
    }
    
    // Added helper to allow saving cookies from WebView (like in LoginActivity)
    fun saveCookiesFromResponse(url: String, cookies: List<Cookie>) {
         if (!::persistentCookieJar.isInitialized) return
         val httpUrl = url.toHttpUrlOrNull()
         if (httpUrl != null) {
             persistentCookieJar.saveFromResponse(httpUrl, cookies)
        }
    }
} 