package com.tthih.yu

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.Manifest
import android.app.PendingIntent
import android.content.DialogInterface
import android.content.pm.ShortcutManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.tthih.yu.ShortcutBroadcastReceiver
import com.tthih.yu.ui.theme.YUTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.withContext
import android.util.Log

class SplashActivity : ComponentActivity() {
    /* // Temporarily commented out - move to MainActivity later
    // 权限请求回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var allGranted = true

        permissions.entries.forEach {
            if (!it.value) {
                allGranted = false
            }
        }

        if (allGranted) {
            // 所有权限都已授予，继续操作
            checkFirstLaunch()
        } else {
            // 权限未完全授予，仍然继续
            checkFirstLaunch() // 直接继续
        }
    }
    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查权限
        // checkPermissions() // <-- REMOVED THIS CALL
        checkFirstLaunch() // <-- CALL THIS DIRECTLY
    }

    /* // Temporarily commented out - move to MainActivity later
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        // 添加需要请求的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13及以上
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 在Android 8.0以上，桌面快捷方式权限不需要显式授权

        if (permissions.isEmpty()) {
            // 没有需要请求的权限，直接继续
            checkFirstLaunch()
            return
        }

        // 检查是否需要请求权限
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            // 已有所有权限，继续操作
            checkFirstLaunch()
        } else {
            // 请求权限
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    */

    private fun checkFirstLaunch() {
        val preferences = getSharedPreferences("YU_PREFS", MODE_PRIVATE)
        val isFirstLaunch = preferences.getBoolean("IS_FIRST_LAUNCH", true)
        
        if (!isFirstLaunch) {
            navigateToMainActivity()
            return
        }
        
        setContent {
            YUTheme {
                SplashScreen(
                    onStartClick = {
                        preferences.edit().putBoolean("IS_FIRST_LAUNCH", false).apply()
                        // Launch shortcut creation in background
                        createShortcutAsync()
                        navigateToMainActivity()
                    }
                )
            }
        }
    }
    
    // New async function for shortcut creation
    private fun createShortcutAsync() {
        lifecycleScope.launch(Dispatchers.IO) { // Perform preparation in background
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val shortcutManager = getSystemService(ShortcutManager::class.java)
                    if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported) {
                        val shortcutIntent = Intent(this@SplashActivity, SplashActivity::class.java).apply {
                            action = Intent.ACTION_MAIN
                            addCategory(Intent.CATEGORY_LAUNCHER)
                        }
                        
                        // Icon loading now in background
                        val icon = IconCompat.createWithResource(this@SplashActivity, R.mipmap.ic_launcher)
                        
                        val pinShortcutInfo = ShortcutInfoCompat.Builder(this@SplashActivity, "yu_app_shortcut")
                            .setIntent(shortcutIntent)
                            .setShortLabel(getString(R.string.app_name))
                            .setIcon(icon)
                            .build()
                        
                        val successCallbackIntent = Intent(this@SplashActivity, ShortcutBroadcastReceiver::class.java)
                        val successCallback = PendingIntent.getBroadcast(
                            this@SplashActivity, 0,
                            successCallbackIntent,
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            } else {
                                PendingIntent.FLAG_UPDATE_CURRENT
                            }
                        )
                        
                        // Switch back to main thread to request pin shortcut (interacts with UI)
                        withContext(Dispatchers.Main) {
                            ShortcutManagerCompat.requestPinShortcut(
                                this@SplashActivity, pinShortcutInfo, successCallback.intentSender
                            )
                        }
                        
                        val preferences = getSharedPreferences("YU_PREFS", MODE_PRIVATE)
                        preferences.edit().putBoolean("SHORTCUT_REQUESTED", true).apply()
                    } else {
                        // Still show Toast on Main thread if needed
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@SplashActivity, "您的设备不支持创建桌面快捷方式", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Legacy shortcut creation (already uses broadcast, likely okay)
                    val shortcutIntent = Intent(this@SplashActivity, SplashActivity::class.java).apply {
                        action = Intent.ACTION_MAIN
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    val intent = Intent().apply {
                        putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
                        putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.app_name))
                        putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, 
                            Intent.ShortcutIconResource.fromContext(this@SplashActivity, R.mipmap.ic_launcher))
                        action = "com.android.launcher.action.INSTALL_SHORTCUT"
                    }
                    // Send broadcast from background is fine
                    sendBroadcast(intent)
                    
                    val preferences = getSharedPreferences("YU_PREFS", MODE_PRIVATE)
                    preferences.edit().putBoolean("SHORTCUT_CREATED", true).apply()
                }
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error creating shortcut", e)
            }
        }
    }
    
    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}

@Composable
fun SplashScreen(onStartClick: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000)
    )
    
    LaunchedEffect(key1 = true) {
        startAnimation = true
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 橙子图标
            Image(
                painter = painterResource(id = R.drawable.orange),
                contentDescription = "YU 图标",
                modifier = Modifier
                    .size(360.dp)
                    .clip(CircleShape)
                    .padding(8.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 应用名称
            Text(
                text = "YU",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 应用描述
            Text(
                text = "帮尽徽程人，听尽徽程事",
                fontSize = 30.sp,
                color = Color(0xFF2E7D32), // 墨绿色
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // 开始按钮
            Button(
                onClick = onStartClick,
                modifier = Modifier
                    .height(40.dp)
                    .width(160.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White
                )
            ) {
                Text(
                    text = "立即开始",
                    color = Color(0xFFFF8C00),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun gradient() = androidx.compose.ui.graphics.Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFF1E6), // 更淡的橙色
        Color(0xFFFFE6CC)  // 更淡的橙色
    )
) 