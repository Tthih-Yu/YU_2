package com.tthih.yu.schedule

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Observer
import com.tthih.yu.R
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import org.json.JSONArray

class ScheduleActivity : AppCompatActivity() {
    
    private val viewModel: ScheduleViewModel by viewModels()
    private var currentView = VIEW_DAILY // 默认显示日视图
    
    // 定义抹茶绿主题的颜色常量
    private val MatchaGreen = Color(0xFF8BC34A) // 主色
    private val MatchaLightGreen = Color(0xFFCFE5B4) // 浅绿色
    private val MatchaDarkGreen = Color(0xFF689F38) // 深绿色
    private val MatchaBgColor = Color(0xFFF4F8F0) // 背景色
    private val MatchaTextPrimary = Color(0xFF2E4F2C) // 主文本色
    private val MatchaTextSecondary = Color(0xFF5E7859) // 次要文本色
    private val MatchaTextHint = Color(0xFF8FA889) // 提示文本色
    private val MatchaCardBg = Color(0xFFFFFFFF) // 卡片背景色
    private val MatchaDivider = Color(0xFFEAF2E3) // 分割线颜色
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)
        
        // 设置状态栏为透明
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        // 初始化Compose视图
        setupComposeView()
        
        // 初始化底部导航
        setupBottomNavigation()
        
        // 观察周课表数据变化
        viewModel.loadWeekSchedules(viewModel.selectedWeek.value ?: 1)
    }
    
    private fun setupComposeView() {
        val composeView = findViewById<ComposeView>(R.id.compose_view)
        
        composeView.setContent {
            // 使用LaunchedEffect观察视图模式变化
            var viewType by remember { mutableStateOf(currentView) }
            
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    primary = MatchaGreen,
                    secondary = MatchaDarkGreen,
                    background = MatchaBgColor,
                    surface = MatchaCardBg,
                    onPrimary = Color.White,
                    onBackground = MatchaTextPrimary,
                    onSurface = MatchaTextPrimary
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (viewType) {
                            VIEW_DAILY -> DailyView(viewModel)
                            VIEW_WEEKLY -> WeeklyView(viewModel)
                            VIEW_SETTINGS -> SettingsView(viewModel)
                        }
                    }
                    
                    // 底部导航栏
                    BottomNavigationBar(
                        currentView = viewType,
                        onViewChanged = { 
                            viewType = it
                            currentView = it
                        }
                    )
                }
            }
        }
    }
    
    @Composable
    private fun DailyView(viewModel: ScheduleViewModel) {
        val date = viewModel.selectedDate.value ?: Date()
        val dateFormat = SimpleDateFormat("M月d日 E", Locale.CHINA)
        val timeNodes = viewModel.getTimeNodes()
        
        // 获取课程列表并监听变化
        val scheduleListState = remember { mutableStateOf(emptyList<ScheduleData>()) }
        val scheduleList = scheduleListState.value
        
        // 监听LiveData变化
        DisposableEffect(Unit) {
            val observer = androidx.lifecycle.Observer<List<ScheduleData>> { newList ->
                scheduleListState.value = newList ?: emptyList()
            }
            viewModel.dailySchedules.observeForever(observer)
            onDispose {
                viewModel.dailySchedules.removeObserver(observer)
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MatchaBgColor)
                .padding(PaddingValues(bottom = 56.dp))
        ) {
            // 顶部日期和口号
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(MatchaGreen, MatchaDarkGreen),
                            start = Offset(0f, 0f),
                            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                    .padding(16.dp)
            ) {
                // 日期
                Text(
                    text = "${Calendar.getInstance().get(Calendar.MONTH) + 1}月${Calendar.getInstance().get(Calendar.DAY_OF_MONTH)}日 星期${getDayOfWeekChinese(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))}",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                // 口号
                Text(
                    text = "唯有美食不可负",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .padding(bottom = 16.dp)
                )
                
                // 装饰元素
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    // 这是一个装饰性元素
                }
            }
            
            // 上午课程标题
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            ) {
                // 左侧装饰条
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .background(MatchaGreen, RoundedCornerShape(2.dp))
                )
                
                // 标题文本
                Text(
                    text = "上午课程",
                    modifier = Modifier.padding(start = 8.dp),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = MatchaTextPrimary
                )
            }
            
            // 上午课程列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // 过滤上午的课程（节次1-5）
                val morningClasses = timeNodes.filter { it.node <= 5 }
                
                items(morningClasses) { timeNode ->
                    val coursesForThisNode = scheduleList.filter { 
                        it.startNode <= timeNode.node && it.endNode >= timeNode.node 
                    }
                    
                    CourseItem(
                        timeNode = timeNode,
                        courses = coursesForThisNode
                    )
                }
            }
            
            // 下午课程标题
            Text(
                text = "下午课程",
                modifier = Modifier
                    .padding(start = 16.dp)
                    .padding(top = 16.dp)
                    .padding(bottom = 8.dp),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF000000) // iOS默认深色文本
            )
            
            // 下午课程列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // 过滤下午的课程（节次6-9）
                val afternoonClasses = timeNodes.filter { it.node in 6..9 }
                
                items(afternoonClasses) { timeNode ->
                    val coursesForThisNode = scheduleList.filter { 
                        it.startNode <= timeNode.node && it.endNode >= timeNode.node 
                    }
                    
                    CourseItem(
                        timeNode = timeNode,
                        courses = coursesForThisNode
                    )
                }
            }
            
            // 晚上课程标题
            Text(
                text = "晚上课程",
                modifier = Modifier
                    .padding(start = 16.dp)
                    .padding(top = 16.dp)
                    .padding(bottom = 8.dp),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF000000) // iOS默认深色文本
            )
            
            // 晚上课程列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                // 过滤晚上的课程（节次10-11）
                val eveningClasses = timeNodes.filter { it.node >= 10 }
                
                items(eveningClasses) { timeNode ->
                    val coursesForThisNode = scheduleList.filter { 
                        it.startNode <= timeNode.node && it.endNode >= timeNode.node 
                    }
                    
                    CourseItem(
                        timeNode = timeNode,
                        courses = coursesForThisNode
                    )
                }
            }
        }
    }
    
    @Composable
    private fun CourseItem(timeNode: ScheduleTimeNode, courses: List<ScheduleData>) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // 左侧时间栏
            Column(
                modifier = Modifier
                    .width(60.dp)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = "${timeNode.node}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666)
                )
                Text(
                    text = "${timeNode.startTime}\n${timeNode.endTime}",
                    fontSize = 12.sp,
                    color = Color(0xFF999999)
                )
            }
            
            // 课程内容
            if (courses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(45.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MatchaLightGreen.copy(alpha = 0.3f))
                        .border(
                            width = 1.dp,
                            color = MatchaLightGreen.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(8.dp)
                        .wrapContentSize(align = Alignment.Center)
                ) {
                    Text(
                        text = "空闲",
                        color = MatchaTextSecondary,
                        fontSize = 15.sp
                    )
                }
            } else {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(75.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MatchaLightGreen.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 1.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        courses.forEach { course ->
                            Text(
                                text = course.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MatchaTextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${course.classroom} | ${course.teacher}",
                                fontSize = 14.sp,
                                color = MatchaTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun WeeklyView(viewModel: ScheduleViewModel) {
        val selectedWeek = viewModel.selectedWeek.value ?: 1
        val currentWeek = viewModel.currentWeek.value ?: 1
        val totalWeeks = viewModel.totalWeeks.value ?: 18
        
        // 获取课程列表并监听变化
        val schedulesMapState = remember { mutableStateOf(emptyMap<Int, List<ScheduleData>>()) }
        val weekSchedules = schedulesMapState.value
        
        // 监听LiveData变化
        DisposableEffect(Unit) {
            val observer = androidx.lifecycle.Observer<Map<Int, List<ScheduleData>>> { newMap ->
                schedulesMapState.value = newMap ?: emptyMap()
            }
            viewModel.weeklySchedules.observeForever(observer)
            onDispose {
                viewModel.weeklySchedules.removeObserver(observer)
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MatchaBgColor)
                .padding(PaddingValues(bottom = 56.dp))
        ) {
            // 顶部周选择栏
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MatchaCardBg
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                ),
                border = BorderStroke(1.dp, MatchaLightGreen.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { 
                            viewModel.previousWeek()
                            // 强制重新加载视图
                            viewModel.loadWeekSchedules(viewModel.selectedWeek.value ?: 1)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(MatchaLightGreen, MatchaGreen.copy(alpha = 0.7f)),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            )
                    ) {
                        Text("<", fontSize = 18.sp, color = MatchaTextPrimary)
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "第${selectedWeek}周",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MatchaTextPrimary
                        )
                        
                        if (selectedWeek == currentWeek) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MatchaGreen.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "本周",
                                    fontSize = 12.sp,
                                    color = MatchaGreen
                                )
                            }
                        }
                    }
                    
                    IconButton(
                        onClick = { 
                            viewModel.nextWeek() 
                            // 强制重新加载视图
                            viewModel.loadWeekSchedules(viewModel.selectedWeek.value ?: 1)
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(MatchaLightGreen, MatchaGreen.copy(alpha = 0.7f)),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                            )
                    ) {
                        Text(">", fontSize = 18.sp, color = MatchaTextPrimary)
                    }
                }
            }
            
            // 周一到周日表头
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MatchaCardBg
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // 左侧节次栏位置占位
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .padding(4.dp)
                    ) {
                        Text("", fontSize = 14.sp)
                    }
                    
                    // 周一到周日
                    val weekDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
                    weekDays.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                                .wrapContentSize(align = Alignment.Center)
                        ) {
                            Text(
                                text = day,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                color = MatchaTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            
            // 课程表网格
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                // 左侧节次栏
                Column(
                    modifier = Modifier
                        .width(30.dp)
                ) {
                    for (i in 1..11) {
                        Box(
                            modifier = Modifier
                                .height(50.dp)
                                .padding(4.dp)
                                .wrapContentSize(align = Alignment.Center)
                        ) {
                            Text(
                                text = "$i",
                                fontSize = 14.sp,
                                color = MatchaTextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                // 课程网格
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // 周一到周日的课程列
                    for (day in 1..7) {
                        val schedulesForDay = weekSchedules[day] ?: emptyList()
                        
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            // 显示每节课的格子
                            for (nodeNum in 1..11) {
                                // 找出当前节次对应的课程
                                val coursesForNode = schedulesForDay.filter { 
                                    it.startNode <= nodeNum && it.endNode >= nodeNum 
                                }
                                
                                // 计算该课程的第一节课（避免重复显示）
                                val isFirstNodeOfCourse = coursesForNode.any { it.startNode == nodeNum }
                                
                                if (isFirstNodeOfCourse && coursesForNode.isNotEmpty()) {
                                    // 有课程，并且是该课程的第一节
                                    val course = coursesForNode.first()
                                    val courseHeight = (course.endNode - course.startNode + 1) * 50
                                    val courseHeightDp = courseHeight.dp
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(courseHeightDp)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(
                                                        MatchaLightGreen.copy(alpha = 0.7f),
                                                        MatchaLightGreen.copy(alpha = 0.4f)
                                                    )
                                                )
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = MatchaLightGreen,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .padding(4.dp)
                                            .wrapContentSize(align = Alignment.Center)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = course.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                textAlign = TextAlign.Center,
                                                color = MatchaTextPrimary
                                            )
                                            Text(
                                                text = course.classroom,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center,
                                                color = MatchaTextSecondary
                                            )
                                        }
                                    }
                                } else if (coursesForNode.isEmpty()) {
                                    // 没有课程
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MatchaBgColor)
                                    ) {
                                        // 空白占位
                                    }
                                }
                                // 如果有课程但不是第一节，则跳过（被前面的课程覆盖了）
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun SettingsView(viewModel: ScheduleViewModel) {
        // 使用remember和mutableStateOf来跟踪状态变化
        val currentWeekState = remember { mutableStateOf(1) }
        val totalWeeksState = remember { mutableStateOf(18) }
        val startDateState = remember { mutableStateOf(Date()) }
        
        // 监听currentWeek变化
        DisposableEffect(Unit) {
            val observer = Observer<Int> { newWeek ->
                currentWeekState.value = newWeek ?: 1
            }
            viewModel.currentWeek.observeForever(observer)
            onDispose {
                viewModel.currentWeek.removeObserver(observer)
            }
        }
        
        // 监听totalWeeks变化
        DisposableEffect(Unit) {
            val observer = Observer<Int> { newTotal ->
                totalWeeksState.value = newTotal ?: 18
            }
            viewModel.totalWeeks.observeForever(observer)
            onDispose {
                viewModel.totalWeeks.removeObserver(observer)
            }
        }
        
        // 监听startDate变化
        DisposableEffect(Unit) {
            val observer = Observer<Date> { newDate ->
                startDateState.value = newDate ?: Date()
            }
            viewModel.startDate.observeForever(observer)
            onDispose {
                viewModel.startDate.removeObserver(observer)
            }
        }
        
        val currentWeek = currentWeekState.value
        val totalWeeks = totalWeeksState.value
        val startDate = startDateState.value
        
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MatchaBgColor)
                .padding(PaddingValues(bottom = 56.dp))
        ) {
            // 顶部标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MatchaCardBg)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { finish() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MatchaLightGreen)
                ) {
                    Text("<", fontSize = 18.sp, color = MatchaTextPrimary)
                }
                
                Text(
                    text = "课程表设置",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = MatchaTextPrimary
                )
                
                Box(
                    modifier = Modifier.width(40.dp)
                ) {
                    // 占位，保持标题居中
                }
            }
            
            // 基本设置标题
            Text(
                text = "基本设置",
                color = MatchaTextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .padding(top = 16.dp)
                    .padding(bottom = 8.dp)
            )
            
            // 设置项卡片背景
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MatchaCardBg
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 2.dp
                ),
                border = BorderStroke(1.dp, MatchaLightGreen.copy(alpha = 0.3f))
            ) {
                // 开始上课时间
                SettingItem(
                    title = "开始上课时间",
                    subtitle = "课表开始的第一天，不是开学时间",
                    value = dateFormat.format(startDate)
                ) {
                    showDatePicker(startDate) { newDate ->
                        viewModel.setStartDate(newDate)
                    }
                }
                
                // 当前的周数
                SettingItem(
                    title = "当前的周数",
                    subtitle = "开学到现在几周，便于我们确定单双周",
                    value = "$currentWeek"
                ) {
                    // 弹出周数选择对话框
                    showWeekPicker(currentWeek, totalWeeks) { week ->
                        viewModel.setCurrentWeek(week)
                    }
                }
                
                // A学期总周数
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "本学期总周数",
                            fontSize = 16.sp,
                            color = MatchaTextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$totalWeeks",
                            fontSize = 16.sp,
                            color = MatchaGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Text(
                        text = "请选择本学期总共多少周",
                        fontSize = 13.sp,
                        color = MatchaTextSecondary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    
                    // 进度条
                    Slider(
                        value = totalWeeks.toFloat(),
                        onValueChange = { 
                            viewModel.setTotalWeeks(it.toInt())
                        },
                        valueRange = 10f..24f,
                        steps = 13, // 24-10-1 = 13
                        colors = SliderDefaults.colors(
                            thumbColor = MatchaGreen,
                            activeTrackColor = MatchaGreen,
                            inactiveTrackColor = MatchaLightGreen.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 切换项卡片背景
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                // 是否显示周末
                SettingSwitch(
                    title = "是否显示周末",
                    subtitle = "如果周末有课程，可打开该设置",
                    isChecked = true
                ) { isChecked ->
                    // 实现切换显示周末的逻辑
                    Toast.makeText(this@ScheduleActivity, "设置已更新", Toast.LENGTH_SHORT).show()
                }
                
                // 是否显示非本周课程
                SettingSwitch(
                    title = "是否显示非本周课程",
                    subtitle = "开启后单双周课程都可以看见哦",
                    isChecked = false
                ) { isChecked ->
                    // 实现切换显示非本周课程的逻辑
                    Toast.makeText(this@ScheduleActivity, "设置已更新", Toast.LENGTH_SHORT).show()
                }
                
                // 添加分割线
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MatchaDivider,
                    thickness = 1.dp
                )
                
                // 导入课表选项
                SettingItem(
                    title = "导入教务系统课表",
                    subtitle = "从安徽工程大学教务系统导入课表数据",
                    value = "导入"
                ) {
                    showImportScheduleDialog()
                }
            }
            
            // 其他设置项...
            Text(
                text = "特色功能",
                color = MatchaTextSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .padding(top = 24.dp)
                    .padding(bottom = 8.dp)
            )
        }
    }
    
    @Composable
    private fun SettingItem(
        title: String,
        subtitle: String,
        value: String,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = MatchaTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MatchaTextSecondary
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(MatchaLightGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = MatchaGreen
                )
                Text(
                    text = "›",
                    fontSize = 18.sp,
                    color = MatchaGreen,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
        
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MatchaDivider,
            thickness = 1.dp
        )
    }
    
    @Composable
    private fun SettingSwitch(
        title: String,
        subtitle: String,
        isChecked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    color = Color(0xFF000000)
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFF8E8E93) // iOS辅助文字色
                )
            }
            
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MatchaGreen,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE0E0E0)
                )
            )
        }
        
        Divider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MatchaDivider,
            thickness = 1.dp
        )
    }
    
    @Composable
    private fun BottomNavigationBar(
        currentView: Int,
        onViewChanged: (Int) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MatchaCardBg
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = "A",
                    label = "日视图",
                    isSelected = currentView == VIEW_DAILY,
                    onClick = { onViewChanged(VIEW_DAILY) }
                )
                
                BottomNavItem(
                    icon = "B",
                    label = "周视图",
                    isSelected = currentView == VIEW_WEEKLY,
                    onClick = { onViewChanged(VIEW_WEEKLY) }
                )
                
                BottomNavItem(
                    icon = "C",
                    label = "设置",
                    isSelected = currentView == VIEW_SETTINGS,
                    onClick = { onViewChanged(VIEW_SETTINGS) }
                )
            }
        }
    }
    
    @Composable
    private fun BottomNavItem(
        icon: String,
        label: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .clickable { onClick() }
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) MatchaGreen.copy(alpha = 0.15f) else Color.Transparent)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = icon,
                    color = if (isSelected) MatchaGreen else MatchaTextHint,
                    fontSize = 18.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            
            Text(
                text = label,
                color = if (isSelected) MatchaGreen else MatchaTextHint,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
    
    private fun setupBottomNavigation() {
        // 已在Compose中实现
    }
    
    private fun showDatePicker(currentDate: Date, onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        calendar.time = currentDate
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }
    
    private fun showWeekPicker(currentWeek: Int, totalWeeks: Int, onWeekSelected: (Int) -> Unit) {
        // 简单实现，实际可以使用NumberPicker或自定义对话框
        val weeks = (1..totalWeeks).toList()
        val choices = weeks.map { it.toString() }.toTypedArray()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择当前周次")
            .setSingleChoiceItems(choices, currentWeek - 1) { dialog, which ->
                onWeekSelected(which + 1)
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showTotalWeeksPicker(currentTotal: Int, onTotalSelected: (Int) -> Unit) {
        // 简单实现，实际可以使用NumberPicker或自定义对话框
        val weeks = (10..24).toList()
        val choices = weeks.map { it.toString() }.toTypedArray()
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择学期总周数")
            .setSingleChoiceItems(choices, currentTotal - 10) { dialog, which ->
                onTotalSelected(which + 10)
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun getDayOfWeekChinese(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "一"
            Calendar.TUESDAY -> "二"
            Calendar.WEDNESDAY -> "三"
            Calendar.THURSDAY -> "四"
            Calendar.FRIDAY -> "五"
            Calendar.SATURDAY -> "六"
            Calendar.SUNDAY -> "日"
            else -> "一"
        }
    }
    
    private fun showImportScheduleDialog() {
        // 创建一个自定义对话框
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_import_schedule, null)
        val importBtn = dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btn_import)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progress_bar)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tv_message)
        val webView = WebView(this)
        
        // 配置WebView
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.blockNetworkImage = false
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.setSupportZoom(true)
        webView.settings.builtInZoomControls = true
        webView.settings.displayZoomControls = false
        
        // 启用WebView调试
        WebView.setWebContentsDebuggingEnabled(true)
        
        webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.61 Safari/537.36"
        
        // 创建容器布局
        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.addView(dialogView)
        
        // 配置WebView布局参数
        val webViewParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            resources.displayMetrics.heightPixels / 2
        )
        webView.layoutParams = webViewParams
        
        // 初始隐藏WebView
        webView.visibility = View.GONE
        container.addView(webView)
        
        // 创建对话框
        val dialog = AlertDialog.Builder(this)
            .setTitle("导入教务系统课表")
            .setView(container)
            .setNegativeButton("取消", null)
            .create()
        
        // 设置导入按钮点击监听
        importBtn.setOnClickListener {
            if (webView.visibility == View.GONE) {
                // 首次点击，显示WebView并加载教务系统
                webView.visibility = View.VISIBLE
                messageTextView.text = "请在下方登录教务系统并导航到课表页面，然后再次点击导入按钮"
                importBtn.text = "解析并导入"
                
                // 加载登录页
                webView.loadUrl("https://webvpn.ahpu.edu.cn/http/webvpn40a1cc242791dfe16b3115ea5846a65e/authserver/login?service=https://webvpn.ahpu.edu.cn/enlink/api/client/callback/cas")
                
                // 设置WebViewClient
                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        messageTextView.text = "请登录并导航到课表页面，然后点击导入按钮"
                    }
                    
                    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        messageTextView.text = "页面加载出错: ${error?.description}"
                    }
                }
            } else {
                // 第二次点击，执行课表解析
                progressBar.visibility = View.VISIBLE
                importBtn.isEnabled = false
                messageTextView.text = "正在解析课表数据..."
                
                // 添加JavaScript接口
                val jsInterface = ScheduleJsInterface(object : ScheduleImportCallback {
                    override fun onProgress(message: String) {
                        runOnUiThread {
                            messageTextView.text = message
                        }
                    }
                    
                    override fun onSuccess(schedules: List<ScheduleData>) {
                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            messageTextView.text = "导入成功，共导入${schedules.size}门课程"
                            
                            // 保存到数据库
                            viewModel.clearSchedules {
                                schedules.forEach { schedule ->
                                    viewModel.addSchedule(schedule)
                                }
                                
                                Toast.makeText(this@ScheduleActivity, "课表导入成功", Toast.LENGTH_SHORT).show()
                                
                                // 重新加载课表
                                viewModel.loadWeekSchedules(viewModel.selectedWeek.value ?: 1)
                                viewModel.loadTodaySchedules()
                                
                                // 关闭对话框
                                dialog.dismiss()
                            }
                        }
                    }
                    
                    override fun onError(error: String) {
                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            messageTextView.text = "导入失败：$error"
                            importBtn.isEnabled = true
                        }
                    }
                })
                webView.addJavascriptInterface(jsInterface, "Android")
                
                // 执行课表解析脚本
                val parseScheduleJs = readRawJsFile(R.raw.schedule_parser)
                webView.evaluateJavascript(parseScheduleJs) { result ->
                    // 脚本会通过JavaScript接口回调结果
                    if (result == "null") {
                        runOnUiThread {
                            messageTextView.text = "解析课表失败，请确保页面中显示了课表内容"
                            importBtn.isEnabled = true
                        }
                    }
                }
            }
        }
        
        dialog.show()
    }
    
    // 导入回调接口
    interface ScheduleImportCallback {
        fun onProgress(message: String)
        fun onSuccess(schedules: List<ScheduleData>)
        fun onError(error: String)
    }
    
    // JavaScript接口
    private inner class ScheduleJsInterface(private val callback: ScheduleImportCallback) {
        @JavascriptInterface
        fun onProgress(message: String) {
            callback.onProgress(message)
        }
        
        @JavascriptInterface
        fun onScheduleData(jsonData: String) {
            try {
                val schedules = this@ScheduleActivity.parseScheduleData(jsonData)
                callback.onSuccess(schedules)
            } catch (e: Exception) {
                callback.onError("解析课表数据失败: ${e.message}")
            }
        }
        
        @JavascriptInterface
        fun onError(error: String) {
            callback.onError(error)
        }
    }
    
    // 解析课表数据
    private fun parseScheduleData(jsonData: String): List<ScheduleData> {
        val schedules = mutableListOf<ScheduleData>()
        
        try {
            val jsonArray = JSONArray(jsonData)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                // 解析课程数据
                val name = jsonObject.getString("name")
                val position = jsonObject.optString("position", "")
                val teacher = jsonObject.optString("teacher", "")
                val day = jsonObject.getInt("day")
                
                // 解析上课节次
                val sectionsArray = jsonObject.getJSONArray("sections")
                val startNode = sectionsArray.getInt(0)
                val endNode = sectionsArray.getInt(sectionsArray.length() - 1)
                
                // 解析周次
                val weeksArray = jsonObject.getJSONArray("weeks")
                val startWeek = weeksArray.getInt(0)
                val endWeek = weeksArray.getInt(weeksArray.length() - 1)
                
                // 创建课程数据对象
                val schedule = ScheduleData(
                    name = name,
                    classroom = position,
                    teacher = teacher,
                    weekDay = day,
                    startNode = startNode,
                    endNode = endNode,
                    startWeek = startWeek,
                    endWeek = endWeek
                )
                
                schedules.add(schedule)
            }
        } catch (e: Exception) {
            throw Exception("解析课表数据失败: ${e.message}")
        }
        
        return schedules
    }
    
    // 读取原始JS文件
    private fun readRawJsFile(resourceId: Int): String {
        return resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
    }
    
    companion object {
        const val VIEW_DAILY = 0
        const val VIEW_WEEKLY = 1
        const val VIEW_SETTINGS = 2
    }
} 