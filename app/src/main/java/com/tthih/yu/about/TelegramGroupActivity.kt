package com.tthih.yu.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.tthih.yu.R

class TelegramGroupActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var telegramLinkTextView: TextView
    private lateinit var copyButton: Button
    private lateinit var qrCodeImageView: ImageView
    private lateinit var joinButton: Button

    // Telegram群组链接，可以根据实际情况修改
    private val telegramLink = "https://t.me/yu_app_group"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_telegram_group)

        // 初始化视图
        initViews()

        // 设置点击事件
        setupClickListeners()
    }

    private fun initViews() {
        // 设置工具栏
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Telegram群组"

        // 初始化其他视图
        telegramLinkTextView = findViewById(R.id.tv_telegram_link)
        copyButton = findViewById(R.id.btn_copy_link)
        qrCodeImageView = findViewById(R.id.iv_telegram_qr_code)
        joinButton = findViewById(R.id.btn_join_group)

        // 设置Telegram链接
        telegramLinkTextView.text = telegramLink
    }

    private fun setupClickListeners() {
        // 复制按钮点击事件
        copyButton.setOnClickListener {
            copyToClipboard(telegramLink)
            Toast.makeText(this, "链接已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }

        // 加入群组按钮点击事件
        joinButton.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(telegramLink))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开链接，请确保您已安装浏览器或Telegram应用", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 复制文本到剪贴板
    private fun copyToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Telegram链接", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 