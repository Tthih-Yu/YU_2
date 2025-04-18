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

class MainActivity : ComponentActivity() {
    
    private val PERMISSION_REQUEST_CODE = 100
    private val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_MEDIA_IMAGES
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    
    // 当用户回应权限请求时触发
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 检查所有权限是否都被授予
        val allGranted = permissions.entries.all { it.value }
        
        if (allGranted) {
            // 如果Android 11及以上需要管理所有文件权限，检查是否已授予
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && 
                !Environment.isExternalStorageManager()) {
                requestManageExternalStoragePermission()
            }
        } else {
            // 告知用户缺少必要权限
            Toast.makeText(
                this,
                "应用需要这些权限才能正常工作",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // 当用户从系统设置返回时触发
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { 
        // 无论结果如何都继续使用应用
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 检查并请求权限
        if (!hasRequiredPermissions()) {
            requestPermissions()
        } else {
            // 如果Android 11及以上需要管理所有文件权限，检查是否已授予
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && 
                !Environment.isExternalStorageManager()) {
                requestManageExternalStoragePermission()
            }
        }
        
        // 检测是否是小米MIUI系统，如果是则提示可能需要关闭优化
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.getDefault())
        val model = Build.MODEL.lowercase(Locale.getDefault())
        
        if (manufacturer.contains("xiaomi") || 
            manufacturer.contains("redmi") || 
            model.contains("mi") || 
            model.contains("redmi")) {
            Toast.makeText(
                this,
                "小米用户注意：如果应用运行异常，请在设置中禁用MIUI优化并给予所有权限",
                Toast.LENGTH_LONG
            ).show()
        }
        
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
        
        // 初始化电费小部件
        try {
            com.tthih.yu.electricity.ElectricityWidgetProvider.updateAllWidgets(this)
        } catch (e: Exception) {
            Log.e("MainActivity", "初始化电费小部件异常：${e.message}")
        }
    }
    
    // 检查是否拥有所有必需权限
    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    // 请求必需的权限
    private fun requestPermissions() {
        requestPermissionLauncher.launch(requiredPermissions)
    }
    
    // 请求管理所有文件的权限（Android 11及以上）
    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:$packageName")
                storagePermissionLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storagePermissionLauncher.launch(intent)
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