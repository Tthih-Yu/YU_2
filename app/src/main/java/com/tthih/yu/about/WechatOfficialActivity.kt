package com.tthih.yu.about

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.tthih.yu.R

class WechatOfficialActivity : AppCompatActivity() {

    private lateinit var ivQrCode: ImageView
    private lateinit var tvWechatId: TextView
    private lateinit var btnCopyId: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wechat_official)

        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "微信公众号"
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // 初始化视图
        ivQrCode = findViewById(R.id.iv_qrcode)
        tvWechatId = findViewById(R.id.tv_wechat_id)
        btnCopyId = findViewById(R.id.btn_copy_id)

        // 设置微信公众号ID
        val wechatId = "YU校园助手"
        tvWechatId.text = wechatId

        // 设置复制ID按钮点击事件
        btnCopyId.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Wechat Official ID", wechatId)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "公众号ID已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 