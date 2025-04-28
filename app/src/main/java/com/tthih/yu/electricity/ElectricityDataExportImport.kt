package com.tthih.yu.electricity

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 电费历史数据导入导出工具类
 */
class ElectricityDataExportImport {

    companion object {
        private const val TAG = "ElectricityDataExport"
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        /**
         * 导出历史数据为JSON格式
         * @param data 历史数据列表
         * @param uri 目标文件URI
         * @param contentResolver ContentResolver实例
         * @return 是否成功
         */
        fun exportToJson(data: List<ElectricityHistoryData>, uri: Uri, contentResolver: ContentResolver): Boolean {
            return try {
                val jsonArray = JSONArray()
                data.forEach { historyData ->
                    val jsonObject = JSONObject().apply {
                        put("timestamp", dateFormat.format(historyData.date))
                        put("balance", historyData.balance)
                        put("building", historyData.building)
                        put("roomId", historyData.roomId)
                        put("usage", historyData.usage)
                        put("recharge", historyData.recharge)
                    }
                    jsonArray.put(jsonObject)
                }

                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        writer.write(jsonArray.toString(2)) // 带缩进的JSON
                    }
                }
                Log.d(TAG, "成功导出${data.size}条历史数据为JSON")
                true
            } catch (e: Exception) {
                Log.e(TAG, "导出JSON失败: ${e.message}", e)
                false
            }
        }

        /**
         * 导出历史数据为CSV格式
         * @param data 历史数据列表
         * @param uri 目标文件URI
         * @param contentResolver ContentResolver实例
         * @return 是否成功
         */
        fun exportToCsv(data: List<ElectricityHistoryData>, uri: Uri, contentResolver: ContentResolver): Boolean {
            return try {
                val header = "时间戳,余额,宿舍楼,房间号,用电量,充值金额\n"
                val rows = data.joinToString("\n") { historyData ->
                    "${dateFormat.format(historyData.date)},${historyData.balance},${historyData.building}," +
                            "${historyData.roomId},${historyData.usage},${historyData.recharge}"
                }

                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                        writer.write(header)
                        writer.write(rows)
                    }
                }
                Log.d(TAG, "成功导出${data.size}条历史数据为CSV")
                true
            } catch (e: Exception) {
                Log.e(TAG, "导出CSV失败: ${e.message}", e)
                false
            }
        }

        /**
         * 从JSON文件导入历史数据
         * @param uri 源文件URI
         * @param contentResolver ContentResolver实例
         * @return 解析出的历史数据列表，如果解析失败返回空列表
         */
        fun importFromJson(uri: Uri, contentResolver: ContentResolver): List<ElectricityHistoryData> {
            return try {
                val jsonString = contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() ?: "" }
                val jsonArray = JSONArray(jsonString)
                val result = mutableListOf<ElectricityHistoryData>()

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    
                    // 获取必需的timestamp和balance字段
                    if (!jsonObject.has("timestamp") || !jsonObject.has("balance")) {
                        continue
                    }
                    
                    val timestampStr = jsonObject.getString("timestamp")
                    val balance = jsonObject.getDouble("balance")
                    
                    // 尝试解析日期
                    val date = try {
                        dateFormat.parse(timestampStr) ?: Date()
                    } catch (e: Exception) {
                        Log.w(TAG, "解析日期失败: $timestampStr, 使用当前日期")
                        Date()
                    }
                    
                    // 获取可选字段，如果不存在则使用默认值
                    val building = if (jsonObject.has("building")) jsonObject.getString("building") else ""
                    val roomId = if (jsonObject.has("roomId")) jsonObject.getString("roomId") else ""
                    val usage = if (jsonObject.has("usage")) jsonObject.getDouble("usage") else 0.0
                    val recharge = if (jsonObject.has("recharge")) jsonObject.getDouble("recharge") else 0.0
                    
                    // 创建历史数据对象并添加到结果列表
                    val historyData = ElectricityHistoryData(
                        id = 0, // 导入时ID设为0，让数据库自动生成新ID
                        date = date,
                        balance = balance,
                        building = building,
                        roomId = roomId,
                        usage = usage,
                        recharge = recharge
                    )
                    
                    result.add(historyData)
                }
                
                Log.d(TAG, "成功从JSON导入${result.size}条历史数据")
                result
            } catch (e: Exception) {
                Log.e(TAG, "从JSON导入失败: ${e.message}", e)
                emptyList()
            }
        }

        /**
         * 根据简化JSON格式导入历史数据（只包含timestamp和balance字段）
         * 将自动计算用电量和充值金额
         * @param uri 源文件URI
         * @param building 宿舍楼
         * @param roomId 房间号
         * @param contentResolver ContentResolver实例
         * @return 解析出的历史数据列表，如果解析失败返回空列表
         */
        fun importFromSimpleJson(uri: Uri, building: String, roomId: String, contentResolver: ContentResolver): List<ElectricityHistoryData> {
            return try {
                val jsonString = contentResolver.openInputStream(uri)?.bufferedReader().use { it?.readText() ?: "" }
                val jsonArray = JSONArray(jsonString)
                val result = mutableListOf<ElectricityHistoryData>()
                var previousBalance: Double? = null

                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    
                    // 获取必需的timestamp和balance字段
                    if (!jsonObject.has("timestamp") || !jsonObject.has("balance")) {
                        continue
                    }
                    
                    val timestampStr = jsonObject.getString("timestamp")
                    val balance = jsonObject.getDouble("balance")
                    
                    // 尝试解析日期
                    val date = try {
                        dateFormat.parse(timestampStr) ?: Date()
                    } catch (e: Exception) {
                        Log.w(TAG, "解析日期失败: $timestampStr, 使用当前日期")
                        Date()
                    }
                    
                    // 计算用电量和充值金额
                    var usage = 0.0
                    var recharge = 0.0
                    
                    if (previousBalance != null) {
                        val difference = balance - previousBalance
                        if (difference > 0) {
                            // 余额增加，表示充值
                            recharge = difference
                        } else if (difference < 0) {
                            // 余额减少，表示用电
                            usage = -difference
                        }
                    }
                    
                    previousBalance = balance
                    
                    // 创建历史数据对象并添加到结果列表
                    val historyData = ElectricityHistoryData(
                        id = 0, // 导入时ID设为0，让数据库自动生成新ID
                        date = date,
                        balance = balance,
                        building = building,
                        roomId = roomId,
                        usage = usage,
                        recharge = recharge
                    )
                    
                    result.add(historyData)
                }
                
                Log.d(TAG, "成功从简化JSON导入${result.size}条历史数据")
                result
            } catch (e: Exception) {
                Log.e(TAG, "从简化JSON导入失败: ${e.message}", e)
                emptyList()
            }
        }
    }
} 