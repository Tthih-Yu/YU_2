package com.tthih.yu.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.tthih.yu.R

class EmailContactActivity : AppCompatActivity() {
    
    private lateinit var toolbar: Toolbar
    private lateinit var subjectEditText: TextInputEditText
    private lateinit var messageEditText: TextInputEditText
    private lateinit var sendButton: Button
    
    // 开发者邮箱，可根据实际情况修改
    private val developerEmail = "2190638246@qq.com"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_contact)
        
        // 初始化视图
        initViews()
        
        // 设置发送按钮点击事件
        setupSendButton()
    }
    
    private fun initViews() {
        // 设置工具栏
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "邮件联系"
        
        // 初始化其他视图
        subjectEditText = findViewById(R.id.et_subject)
        messageEditText = findViewById(R.id.et_message)
        sendButton = findViewById(R.id.btn_send)
    }
    
    private fun setupSendButton() {
        sendButton.setOnClickListener {
            val subject = subjectEditText.text.toString().trim()
            val message = messageEditText.text.toString().trim()
            
            // 验证输入
            if (subject.isEmpty()) {
                Toast.makeText(this, "请输入主题", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (message.isEmpty()) {
                Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // 发送邮件
            sendEmail(subject, message)
        }
    }
    
    private fun sendEmail(subject: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(developerEmail))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, message)
            }
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "未找到邮件应用", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "发送邮件失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 