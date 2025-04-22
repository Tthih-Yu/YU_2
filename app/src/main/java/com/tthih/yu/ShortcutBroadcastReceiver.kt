package com.tthih.yu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast

/**
 * 用于接收桌面快捷方式创建成功的回调广播接收器
 */
class ShortcutBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ShortcutReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Shortcut creation callback received")
        
        // 保存创建成功的标记
        val preferences = context.getSharedPreferences("YU_PREFS", Context.MODE_PRIVATE)
        preferences.edit().putBoolean("SHORTCUT_CREATED", true).apply()
        
        // 可以在这里添加其他处理逻辑
        Toast.makeText(context, "桌面快捷方式创建成功", Toast.LENGTH_SHORT).show()
    }
} 