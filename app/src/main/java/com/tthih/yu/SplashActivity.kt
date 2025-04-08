package com.tthih.yu

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.tthih.yu.ui.theme.YUTheme

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 检查是否是第一次启动应用
        val preferences = getSharedPreferences("YU_PREFS", MODE_PRIVATE)
        val isFirstLaunch = preferences.getBoolean("IS_FIRST_LAUNCH", true)
        
        // 如果不是第一次启动，直接跳转到主页面
        if (!isFirstLaunch) {
            navigateToMainActivity()
            return
        }
        
        setContent {
            YUTheme {
                SplashScreen(
                    onStartClick = {
                        // 点击"立即开始"按钮后，更新首次启动状态并跳转到主页面
                        preferences.edit().putBoolean("IS_FIRST_LAUNCH", false).apply()
                        navigateToMainActivity()
                    }
                )
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