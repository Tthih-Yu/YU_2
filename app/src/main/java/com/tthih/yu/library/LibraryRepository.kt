package com.tthih.yu.library

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 图书馆数据仓库类，负责处理与图书馆系统的数据交互
 */
class LibraryRepository {
    
    companion object {
        private const val TAG = "LibraryRepository"
        
        // 图书馆API地址
        private const val LIBRARY_BASE_URL = "http://211.86.225.3:8090/opac"
        private const val SEARCH_API = "$LIBRARY_BASE_URL/search.do"
    }
    
    /**
     * 搜索图书
     * @param keyword 搜索关键词
     * @param searchType 搜索类型(title:题名，author:作者，isbn:ISBN，publisher:出版社)
     * @param page 页码
     * @return 搜索结果
     */
    suspend fun searchBooks(keyword: String, searchType: String = "title", page: Int = 1): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
                val url = URL("$SEARCH_API?q=$encodedKeyword&searchType=$searchType&page=$page")
                
                Log.d(TAG, "搜索图书：URL = $url")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml")
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                    val response = StringBuilder()
                    var line: String?
                    
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    
                    Result.success(response.toString())
                } else {
                    Log.e(TAG, "搜索图书失败：HTTP错误码 $responseCode")
                    Result.failure(Exception("搜索失败，错误码：$responseCode"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "搜索图书异常：${e.message}", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * 获取图书详情
     * @param bookId 图书ID
     * @return 图书详情HTML
     */
    suspend fun getBookDetail(bookId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$LIBRARY_BASE_URL/book/$bookId")
                
                Log.d(TAG, "获取图书详情：URL = $url")
                
                val connection = url.openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml")
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                    val response = StringBuilder()
                    var line: String?
                    
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()
                    
                    Result.success(response.toString())
                } else {
                    Log.e(TAG, "获取图书详情失败：HTTP错误码 $responseCode")
                    Result.failure(Exception("获取详情失败，错误码：$responseCode"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取图书详情异常：${e.message}", e)
                Result.failure(e)
            }
        }
    }
} 