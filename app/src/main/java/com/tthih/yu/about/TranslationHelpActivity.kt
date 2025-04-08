package com.tthih.yu.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.tthih.yu.R

class TranslationHelpActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var emailTextView: TextView
    private lateinit var copyEmailButton: Button
    private lateinit var githubButton: Button

    // 翻译项目相关链接
    private val translationEmail = "translate@yucampus.app"
    private val githubRepositoryUrl = "https://github.com/yucampus/translations"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translation_help)

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
        supportActionBar?.title = "翻译帮助"

        // 初始化其他视图
        emailTextView = findViewById(R.id.tv_translation_email)
        copyEmailButton = findViewById(R.id.btn_copy_email)
        githubButton = findViewById(R.id.btn_github)

        // 设置翻译邮箱
        emailTextView.text = translationEmail
    }

    private fun setupClickListeners() {
        // 复制邮箱按钮点击事件
        copyEmailButton.setOnClickListener {
            copyToClipboard(translationEmail)
            Toast.makeText(this, "邮箱已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }

        // GitHub按钮点击事件
        githubButton.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubRepositoryUrl))
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "无法打开链接，请确保您已安装浏览器", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 复制文本到剪贴板
    private fun copyToClipboard(text: String) {
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("翻译邮箱", text)
        clipboardManager.setPrimaryClip(clipData)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 