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

class QQGroupActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var qqGroupNumberTextView: TextView
    private lateinit var copyButton: Button
    private lateinit var qrCodeImageView: ImageView
    private lateinit var btnJoinGroup: Button

    // QQ群号，可以根据实际情况修改
    private val qqGroupNumber = "2190638246"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qq_group)

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
        supportActionBar?.title = "QQ群"

        // 初始化其他视图
        qqGroupNumberTextView = findViewById(R.id.tv_qq_group_number)
        copyButton = findViewById(R.id.btn_copy_number)
        qrCodeImageView = findViewById(R.id.iv_qq_qr_code)
        btnJoinGroup = findViewById(R.id.btn_join_group)

        // 设置QQ群号
        qqGroupNumberTextView.text = qqGroupNumber
    }

    private fun setupClickListeners() {
        // 复制按钮点击事件
        copyButton.setOnClickListener {
            copyToClipboard(qqGroupNumber)
            Toast.makeText(this, "QQ群号已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }

        // 设置加入群组按钮点击事件
        btnJoinGroup.setOnClickListener {
            try {
                // 使用新的链接格式
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://res.abeim.cn/api-qq?qq=2190638246")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开链接，请确保您已安装浏览器", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 复制文本到剪贴板
    private fun copyToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("QQ群号", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 