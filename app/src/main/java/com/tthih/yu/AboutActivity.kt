package com.tthih.yu

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.tthih.yu.about.CheckUpdateActivity
import com.tthih.yu.about.EmailContactActivity
import com.tthih.yu.about.HelpUsActivity
import com.tthih.yu.about.OpenSourceLicenseActivity
import com.tthih.yu.about.PrivacyPolicyActivity
import com.tthih.yu.about.QQGroupActivity
import com.tthih.yu.about.TelegramGroupActivity
import com.tthih.yu.about.TermsActivity
import com.tthih.yu.about.TranslationHelpActivity
import com.tthih.yu.about.WechatOfficialActivity
import com.tthih.yu.about.AppIntroActivity

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // 设置工具栏
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.about)
        toolbar.setNavigationOnClickListener {
            finish()
        }

        // 获取当前版本名称
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0.0"
        }
        val versionTextView = findViewById<TextView>(R.id.version_text)
        versionTextView.text = versionName

        // 设置检查更新按钮点击事件
        val checkUpdateCard = findViewById<CardView>(R.id.card_check_update)
        checkUpdateCard.setOnClickListener {
            val intent = Intent(this, CheckUpdateActivity::class.java)
            startActivity(intent)
        }

        // 设置应用介绍按钮点击事件
        val appIntroCard = findViewById<CardView>(R.id.card_app_intro)
        appIntroCard.setOnClickListener {
            val intent = Intent(this, AppIntroActivity::class.java)
            startActivity(intent)
        }

        // 设置Telegram群组点击事件
        val telegramCard = findViewById<CardView>(R.id.card_telegram)
        telegramCard.setOnClickListener {
            val intent = Intent(this, TelegramGroupActivity::class.java)
            startActivity(intent)
        }

        // 设置QQ群组点击事件
        val qqCard = findViewById<CardView>(R.id.card_qq)
        qqCard.setOnClickListener {
            val intent = Intent(this, QQGroupActivity::class.java)
            startActivity(intent)
        }

        // 设置邮件联系点击事件
        val emailCard = findViewById<CardView>(R.id.card_email)
        emailCard.setOnClickListener {
            val intent = Intent(this, EmailContactActivity::class.java)
            startActivity(intent)
        }

        // 设置微信公众号点击事件
        val wechatCard = findViewById<CardView>(R.id.card_wechat)
        wechatCard.setOnClickListener {
            val intent = Intent(this, WechatOfficialActivity::class.java)
            startActivity(intent)
        }

        // 设置帮助我们点击事件
        val helpCard = findViewById<CardView>(R.id.card_help)
        helpCard.setOnClickListener {
            val intent = Intent(this, HelpUsActivity::class.java)
            startActivity(intent)
        }

        // 设置协助翻译点击事件
        val translateCard = findViewById<CardView>(R.id.card_translate)
        translateCard.setOnClickListener {
            val intent = Intent(this, TranslationHelpActivity::class.java)
            startActivity(intent)
        }

        // 设置使用协议点击事件
        val termsCard = findViewById<CardView>(R.id.card_terms)
        termsCard.setOnClickListener {
            val intent = Intent(this, TermsActivity::class.java)
            startActivity(intent)
        }

        // 设置隐私政策点击事件
        val privacyCard = findViewById<CardView>(R.id.card_privacy)
        privacyCard.setOnClickListener {
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            startActivity(intent)
        }

        // 设置开源许可点击事件
        val licenseCard = findViewById<CardView>(R.id.card_license)
        licenseCard.setOnClickListener {
            val intent = Intent(this, OpenSourceLicenseActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 