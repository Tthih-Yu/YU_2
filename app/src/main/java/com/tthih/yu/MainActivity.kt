package com.tthih.yu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tthih.yu.electricity.ElectricityActivity
import com.tthih.yu.schedule.ScheduleActivity
import com.tthih.yu.campuscard.CampusCardActivity
import com.tthih.yu.library.LibraryActivity
import com.tthih.yu.todo.TodoActivity
import com.tthih.yu.ui.theme.YUTheme
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.tthih.yu.electricity.ElectricityWidgetProvider
import com.tthih.yu.R
import com.tthih.yu.AboutActivity

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        setContent {
            YUTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(
                        onElectricityClick = {
                            startActivity(Intent(this, ElectricityActivity::class.java))
                        },
                        onScheduleClick = {
                            startActivity(Intent(this, ScheduleActivity::class.java))
                        },
                        onCampusCardClick = {
                            startActivity(Intent(this, CampusCardActivity::class.java))
                        },
                        onLibraryClick = {
                            startActivity(Intent(this, LibraryActivity::class.java))
                        },
                        onTodoClick = {
                            startActivity(Intent(this, TodoActivity::class.java))
                        },
                        onAboutClick = {
                            startActivity(Intent(this, AboutActivity::class.java))
                        }
                    )
                }
            }
        }
        
        // 初始化电费小部件 (异步执行)
        lifecycleScope.launch(Dispatchers.IO) { // 使用 IO 调度器处理潜在的 I/O
            try {
                ElectricityWidgetProvider.updateAllWidgets(this@MainActivity)
            } catch (e: Exception) {
                // 在后台线程记录日志，如果需要在UI线程提示用户，可以使用 withContext(Dispatchers.Main)
                Log.e("MainActivity", "初始化电费小部件异常：${e.message}")
            }
        }
    }
}

@Composable
fun HomeScreen(
    onElectricityClick: () -> Unit,
    onScheduleClick: () -> Unit,
    onCampusCardClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onTodoClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    // 添加旋转状态
    var rotationState by remember { mutableStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = rotationState,
        animationSpec = tween(durationMillis = 300),
        label = "rotation"
    )
    
    // 使用Box作为顶层容器，以便可以放置绝对定位的元素
    Box(modifier = Modifier.fillMaxSize()) {
        // 主要内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部标题栏
            TopAppBar(
                modifier = Modifier.fillMaxWidth()
            )
            
            // 学校名称和应用标题
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "安徽工程大学",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "智慧校园",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            // 功能卡片区域
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 功能区域标题
                Text(
                    text = "常用功能",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // 功能卡片网格
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 电费查询卡片
                    FeatureCard(
                        icon = R.drawable.ic_electricity,
                        title = "宿舍电费",
                        description = "查询与管理电费",
                        backgroundColor = Color(0xFFE8F5E9),
                        modifier = Modifier.weight(1f),
                        onClick = onElectricityClick
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 课表查询卡片
                    FeatureCard(
                        icon = R.drawable.ic_schedule,
                        title = "课程表",
                        description = "查看每日课程",
                        backgroundColor = Color(0xFFE3F2FD),
                        modifier = Modifier.weight(1f),
                        onClick = onScheduleClick
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 第二行功能卡片
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 校园卡卡片
                    FeatureCard(
                        icon = R.drawable.ic_card,
                        title = "校园卡",
                        description = "余额与消费记录",
                        backgroundColor = Color(0xFFFFF8E1),
                        modifier = Modifier.weight(1f),
                        onClick = onCampusCardClick
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 图书馆卡片
                    FeatureCard(
                        icon = R.drawable.ic_library,
                        title = "图书馆",
                        description = "图书检索",
                        backgroundColor = Color(0xFFFCE4EC),
                        modifier = Modifier.weight(1f),
                        onClick = onLibraryClick
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 第三行功能卡片
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 待办事项卡片
                    FeatureCard(
                        icon = R.drawable.ic_todo,
                        title = "待办事项",
                        description = "管理日常任务",
                        backgroundColor = Color(0xFFE0F2F1),
                        modifier = Modifier.weight(1f),
                        onClick = onTodoClick
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // 占位卡片(可以在未来添加更多功能)
                    Box(modifier = Modifier.weight(1f)) {
                        // 暂时为空
                    }
                }
            }
            
            // 底部信息
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Tthih © 2025",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        // 将关于按钮放在顶层Box中，绝对定位到右下角
        IconButton(
            onClick = {
                // 触发旋转动画
                rotationState += 360f
                onAboutClick()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary)
                .graphicsLayer(rotationZ = rotation)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_info),
                contentDescription = "关于",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "YU校园助手",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White
        ),
        modifier = modifier
    )
}

@Composable
fun FeatureCard(
    icon: Int,
    title: String,
    description: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(160.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = title,
                tint = Color.Unspecified,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
    }
}