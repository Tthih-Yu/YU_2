package com.tthih.yu.util

import android.os.Handler
import android.os.Looper
import com.tthih.yu.about.UpdateInfo
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class NetworkUtil {
    companion object {
        // 更新接口URL
        private const val UPDATE_URL = "http://43.143.7.45:3000/api/update"
        
        // 检查更新的接口
        fun checkForUpdates(currentVersionCode: Int, callback: (Boolean, UpdateInfo?) -> Unit) {
            thread {
                try {
                    val url = URL("$UPDATE_URL?version=$currentVersionCode")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            response.append(line)
                        }
                        reader.close()
                        
                        // 解析JSON响应
                        val jsonObject = JSONObject(response.toString())
                        val hasUpdate = jsonObject.getBoolean("hasUpdate")
                        
                        if (hasUpdate) {
                            val data = jsonObject.getJSONObject("data")
                            val updateInfo = UpdateInfo(
                                versionName = data.getString("versionName"),
                                versionCode = data.getInt("versionCode"),
                                forceUpdate = data.getBoolean("forceUpdate"),
                                updateContent = data.getString("updateContent"),
                                downloadUrl = data.getString("downloadUrl"),
                                publishDate = data.getString("publishDate")
                            )
                            
                            // 在主线程回调
                            Handler(Looper.getMainLooper()).post {
                                callback(true, updateInfo)
                            }
                        } else {
                            // 没有更新
                            Handler(Looper.getMainLooper()).post {
                                callback(false, null)
                            }
                        }
                    } else {
                        // 请求失败
                        Handler(Looper.getMainLooper()).post {
                            callback(false, null)
                        }
                    }
                    
                    connection.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                    // 发生异常
                    Handler(Looper.getMainLooper()).post {
                        callback(false, null)
                    }
                }
            }
        }
    }
} 