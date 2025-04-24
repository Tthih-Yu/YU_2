package com.tthih.yu.schedule

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.activity.compose.setContent // Added
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi // Ensure this is present
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable // Added
import androidx.compose.foundation.gestures.Orientation // Added
import androidx.compose.foundation.gestures.draggable // Added
import androidx.compose.foundation.gestures.rememberDraggableState // Added
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions // Added
import androidx.compose.foundation.text.KeyboardOptions // Added
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.* // Keep Material 3 imports
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState // Added
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity // Added
import androidx.compose.ui.platform.LocalFocusManager // Added
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle // Added
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction // Added
import androidx.compose.ui.text.input.KeyboardType // Added
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset // Added
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider // Added
import androidx.compose.animation.core.Animatable // Added
import androidx.compose.animation.core.FastOutSlowInEasing // Added
import androidx.compose.animation.core.tween // Added
import androidx.compose.animation.core.spring // Added
import androidx.annotation.RequiresApi // Ensure this is present
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.util.Log
import android.graphics.Color as AndroidColor // 使用别名区分Android原生的Color
import com.tthih.yu.R
import kotlinx.coroutines.delay // Added
import kotlinx.coroutines.launch // Added
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue // Added
import kotlin.math.roundToInt // Added

// --- Ensure these critical imports are present ---
// import androidx.compose.ui.graphics.Color // For Compose UI Colors - REMOVED, already imported above
// import androidx.compose.foundation.lazy.LazyColumn // For Lazy Lists - REMOVED, already imported above
// import androidx.compose.foundation.lazy.items // For list items function - REMOVED, already imported above
// import androidx.compose.foundation.lazy.LazyListScope // Sometimes needed explicitly for item {} - REMOVED, already imported above

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
        
        // Check if launched from widget item click
        if (intent?.action == ScheduleWidgetProvider.ACTION_VIEW_WEEK) {
            currentView = VIEW_WEEKLY
            // Optionally, jump to the current week when opened from widget
            viewModel.jumpToCurrentWeek() 
        }
        
        // 设置状态栏为透明
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        window.statusBarColor = AndroidColor.TRANSPARENT // 使用别名引用Android原生Color
        
        // 初始化Compose视图
        setupComposeView()
        
        // 初始化底部导航
        setupBottomNavigation()
        
        // 观察周课表数据变化 (Moved from onCreate to ensure it happens after potential view switch)
        // viewModel.loadWeekSchedules(viewModel.selectedWeek.value ?: 1)
        // Let onResume handle initial loading based on currentView
    }
    
    override fun onResume() {
        super.onResume()
        
        // 每次恢复活动时，强制重新加载课表数据
        val currentWeekValue = viewModel.selectedWeek.value ?: viewModel.currentWeek.value ?: 1
        
        // 延迟执行，确保UI已完全初始化
        Handler(Looper.getMainLooper()).postDelayed({
            // 根据当前视图类型重新加载数据
            if (currentView == VIEW_DAILY) {
                // 加载日视图数据 (加载选中日期的课程)
                viewModel.loadSchedulesByDate(viewModel.selectedDate.value ?: Date())
                Log.d("ScheduleActivity", "onResume: 重新加载日视图数据 for ${viewModel.selectedDate.value}")
            } else if (currentView == VIEW_WEEKLY) {
                // 加载周视图数据
                viewModel.loadWeekSchedules(currentWeekValue)
                Log.d("ScheduleActivity", "onResume: 重新加载周视图数据，当前周: $currentWeekValue")
            }
            
            // 刷新Compose视图 (Compose should react automatically, but explicit invalidation might help)
            findViewById<ComposeView>(R.id.compose_view)?.invalidate()
        }, 100) // Reduced delay slightly
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
                            
                            // 当切换到日视图时，确保重新加载日视图数据
                            if (it == VIEW_DAILY) {
                                viewModel.loadSchedulesByDate(viewModel.selectedDate.value ?: Date())
                                Log.d("ScheduleActivity", "切换到日视图，重新加载选中日期 ${viewModel.selectedDate.value} 的课程数据")
                            } 
                            else if (it == VIEW_WEEKLY) {
                                viewModel.loadWeekSchedules(viewModel.selectedWeek.value ?: 1)
                                Log.d("ScheduleActivity", "切换到周视图，重新加载课程数据")
                            }
                        }
                    )
                }
            }
        }
    }
    
    @Composable
    private fun DailyView(viewModel: ScheduleViewModel) {
        // 获取 ViewModel 中的状态，使用 remember { mutableStateOf(...) } 跟踪状态变化
        val selectedDateState = remember { mutableStateOf(viewModel.selectedDate.value ?: Date()) }
        val selectedWeekState = remember { mutableStateOf(viewModel.selectedWeek.value ?: 1) }
        val scheduleListState = remember { mutableStateOf(viewModel.dailySchedules.value ?: emptyList<ScheduleData>()) }
        
        // 监听 ViewModel LiveData 变化，并更新本地状态
        DisposableEffect(viewModel) {
            val dateObserver = Observer<Date> { newDate -> selectedDateState.value = newDate ?: Date() }
            val weekObserver = Observer<Int> { newWeek -> selectedWeekState.value = newWeek ?: 1 }
            val scheduleObserver = Observer<List<ScheduleData>> { newList -> scheduleListState.value = newList ?: emptyList() }
            
            viewModel.selectedDate.observeForever(dateObserver)
            viewModel.selectedWeek.observeForever(weekObserver)
            viewModel.dailySchedules.observeForever(scheduleObserver)
            
            onDispose {
                viewModel.selectedDate.removeObserver(dateObserver)
                viewModel.selectedWeek.removeObserver(weekObserver)
                viewModel.dailySchedules.removeObserver(scheduleObserver)
            }
        }
        
        val date = selectedDateState.value
        val selectedWeek = selectedWeekState.value
        val scheduleList = scheduleListState.value
        val dateFormat = SimpleDateFormat("M月d日 E", Locale.CHINA)
        val timeNodes = viewModel.getTimeNodes() // 获取时间节点列表
        
        // 添加调试日志
        Log.d("ScheduleActivity", "DailyView 刷新 - 日期: ${dateFormat.format(date)}, 周次: $selectedWeek")
        Log.d("ScheduleActivity", "课程数量: ${scheduleList.size}, 课程列表: $scheduleList")
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MatchaBgColor)
        ) {
            // 顶部日期和周次卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MatchaGreen // 使用主绿色
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // 日期
                    Text(
                        text = dateFormat.format(date),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    // 周次
                    Text(
                        text = "第 $selectedWeek 周",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // 课程列表，使用 LazyColumn 以提高性能
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // --- 上午 --- 
                item {
                    SectionHeader("上午")
                }
                val morningNodes = timeNodes.filter { it.node <= 5 }
                items(morningNodes, key = { it.node }) { timeNode ->
                    val coursesForThisNode = scheduleList.filter { 
                        it.startNode <= timeNode.node && it.endNode >= timeNode.node 
                    }
                    CourseItem(timeNode = timeNode, courses = coursesForThisNode, viewModel)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                
                // --- 下午 --- 
                item {
                    SectionHeader("下午")
                }
                val afternoonNodes = timeNodes.filter { it.node in 6..9 }
                items(afternoonNodes, key = { it.node }) { timeNode ->
                    val coursesForThisNode = scheduleList.filter { 
                        it.startNode <= timeNode.node && it.endNode >= timeNode.node 
                    }
                    CourseItem(timeNode = timeNode, courses = coursesForThisNode, viewModel)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
                
                // --- 晚上 --- 
                item {
                    SectionHeader("晚上")
                }
                val eveningNodes = timeNodes.filter { it.node >= 10 }
                items(eveningNodes, key = { it.node }) { timeNode ->
                    val coursesForThisNode = scheduleList.filter { 
                        it.startNode <= timeNode.node && it.endNode >= timeNode.node 
                    }
                    CourseItem(timeNode = timeNode, courses = coursesForThisNode, viewModel)
                }
            }
        }
    }
    
    // 分段标题 Composable
    @Composable
    private fun SectionHeader(title: String) {
        Text(
            text = title,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 8.dp)
                .padding(start = 16.dp),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MatchaTextSecondary
        )
    }
    
    // 课程项 Composable
    @Composable
    private fun CourseItem(timeNode: ScheduleTimeNode, courses: List<ScheduleData>, viewModel: ScheduleViewModel) {
        // 添加调试日志
        Log.d("ScheduleActivity", "CourseItem - 节次: ${timeNode.node}, 课程数量: ${courses.size}")
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧时间栏
            Column(
                modifier = Modifier
                    .width(55.dp) // 稍微减小宽度
                    .padding(end = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${timeNode.node}", // 节次
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MatchaTextPrimary
                )
                Text(
                    text = timeNode.startTime, // 开始时间
                    fontSize = 11.sp,
                    color = MatchaTextSecondary
                )
                 Text(
                    text = timeNode.endTime, // 结束时间
                    fontSize = 11.sp,
                    color = MatchaTextSecondary
                )
            }
            
            // 分隔线
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(60.dp) // 调整高度以匹配卡片
                    .background(MatchaDivider)
            )
            
            Spacer(modifier = Modifier.width(12.dp)) // 增加分隔线和卡片间距

            // 课程内容卡片或空闲占位符
            Box(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 60.dp) // 确保最小高度
            ) {
                if (courses.isEmpty()) {
                    // 空闲时段 - 更美观的卡片样式
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MatchaCardBg // 使用卡片背景色
                        ),
                        border = BorderStroke(1.dp, MatchaDivider), // 添加边框
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp // 移除阴影
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp), // 增加垂直内边距，使卡片看起来更充实
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "空闲时间",
                                color = MatchaTextHint,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                } else {
                    // 有课程时段 - 使用 Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MatchaLightGreen.copy(alpha = 0.6f) // 稍微调整透明度
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 0.dp // 移除阴影效果
                        ),
                        border = BorderStroke(0.5.dp, MatchaLightGreen.copy(alpha = 0.3f)) // 添加非常淡的描边
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp), // 调整内边距
                            verticalArrangement = Arrangement.Center
                        ) {
                            courses.forEach { course ->
                                Text(
                                    text = course.name,
                                    fontSize = 15.sp, // 调整字体大小
                                    fontWeight = FontWeight.SemiBold,
                                    color = MatchaTextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp)) // 增加间距
                                Text(
                                    text = "${course.classroom} | ${course.teacher}",
                                    fontSize = 13.sp, // 调整字体大小
                                    color = MatchaTextSecondary
                                )
                                // 如果有多个课程在同一时段，添加分隔线
                                if (courses.size > 1 && courses.indexOf(course) < courses.size - 1) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp), color = MatchaDivider)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun WeeklyView(viewModel: ScheduleViewModel) {
        // 状态获取和监听
        val selectedWeekState = remember { mutableStateOf(viewModel.selectedWeek.value ?: 1) }
        // 直接观察ViewModel中的currentWeek LiveData
        val currentWeekLiveData = viewModel.currentWeek
        val currentWeekObserved by currentWeekLiveData.observeAsState(viewModel.currentWeek.value ?: 1)
        
        // 使用derivedStateOf来减少不必要的重组
        val schedulesMapState = remember { mutableStateOf(viewModel.weeklySchedules.value ?: emptyMap<Int, List<ScheduleData>>()) }
        
        // 添加课程详情显示状态
        var selectedCourse by remember { mutableStateOf<ScheduleData?>(null) }
        var showDetailSheet by remember { mutableStateOf(false) }
        
        // 添加新课程状态
        var showAddCourseSheet by remember { mutableStateOf(false) }
        var longPressedDay by remember { mutableStateOf(1) } // 默认周一
        var longPressedNode by remember { mutableStateOf(1) } // 默认第一节
        
        // 获取今天是星期几
        val calendar = Calendar.getInstance()
        // Calendar中周日是1，周一是2，所以需要转换
        val todayWeekDay = if (calendar.get(Calendar.DAY_OF_WEEK) == 1) 7 else calendar.get(Calendar.DAY_OF_WEEK) - 1
        
        DisposableEffect(viewModel) {
            val selectedWeekObserver = Observer<Int> { week -> selectedWeekState.value = week ?: 1 }
            // 不再需要在这里观察 currentWeek
            // val currentWeekObserver = Observer<Int> { week -> currentWeekState.value = week ?: 1 }
            val scheduleObserver = Observer<Map<Int, List<ScheduleData>>> { map -> schedulesMapState.value = map ?: emptyMap() }
            
            viewModel.selectedWeek.observeForever(selectedWeekObserver)
            // viewModel.currentWeek.observeForever(currentWeekObserver) // Correctly removed
            viewModel.weeklySchedules.observeForever(scheduleObserver)
            
            onDispose {
                viewModel.selectedWeek.removeObserver(selectedWeekObserver)
                // viewModel.currentWeek.removeObserver(currentWeekObserver)
                viewModel.weeklySchedules.removeObserver(scheduleObserver)
            }
        }
        
        val selectedWeek = selectedWeekState.value
        // 使用观察到的 currentWeekObserved
        val currentWeek = currentWeekObserved 
        val weekSchedules = schedulesMapState.value
        
        // 显示课程详情底部弹窗
        if (showDetailSheet && selectedCourse != null) {
            CourseDetailSheet(
                course = selectedCourse!!,
                onDismiss = { showDetailSheet = false },
                onSave = { updatedCourse ->
                    viewModel.updateSchedule(updatedCourse)
                    showDetailSheet = false
                },
                onDelete = { courseToDelete ->
                    viewModel.deleteSchedule(courseToDelete)
                    showDetailSheet = false
                }
            )
        }
        
        // 显示添加课程底部弹窗
        if (showAddCourseSheet) {
            AddCourseSheet(
                weekDay = longPressedDay,
                startNode = longPressedNode,
                selectedWeek = selectedWeek,
                onDismiss = { showAddCourseSheet = false },
                onAdd = { newCourse ->
                    viewModel.addSchedule(newCourse)
                    showAddCourseSheet = false
                }
            )
        }
        
        // 使用协程范围来处理动画
        val coroutineScope = rememberCoroutineScope()
        
        // 页面宽度 - 用于计算拖动阈值
        val density = LocalDensity.current
        val screenWidth = remember {
            density.run { 360.dp.toPx() }
        }
        
        // 滑动阈值 - 超过屏幕宽度的30%时切换页面
        val swipeThreshold = screenWidth * 0.3f
        
        // 使用rememberable animatable来重用动画对象，避免每次创建新的实例
        val offsetXAnimatable = remember { Animatable(0f) }
        
        // 当前是否可以切换周次 - 使用derivedStateOf减少重组
        val canGoNext by remember(selectedWeek) { derivedStateOf { selectedWeek < (viewModel.totalWeeks.value ?: 18) } }
        val canGoPrevious by remember(selectedWeek) { derivedStateOf { selectedWeek > 1 } }
        
        // 是否正在动画中，防止动画过程中重复触发
        var isAnimating by remember { mutableStateOf(false) }
        
        // 延迟加载数据，先完成动画
        fun loadWeekSchedulesDelayed(week: Int) {
            coroutineScope.launch {
                delay(50) // 短暂延迟，让UI先有响应
                viewModel.loadWeekSchedules(week)
            }
        }
        
        // 定义可拖动状态 - 优化拖动逻辑
        val dragState = rememberDraggableState { delta ->
            if (!isAnimating) { // 只有在不动画时才响应拖动
                coroutineScope.launch {
                    // 根据是否能切换来限制拖动方向
                    val newOffset = offsetXAnimatable.value + delta
                    
                    // 限制拖动范围
                    val targetValue = when {
                        // 不能向前翻页且尝试向右拖动（正值）
                        !canGoPrevious && newOffset > 0 -> 0f
                        // 不能向后翻页且尝试向左拖动（负值）
                        !canGoNext && newOffset < 0 -> 0f
                        // 限制最大拖动距离为屏幕宽度的50%
                        newOffset > screenWidth * 0.5f -> screenWidth * 0.5f
                        newOffset < -screenWidth * 0.5f -> -screenWidth * 0.5f
                        // 正常情况下允许拖动
                        else -> newOffset
                    }
                    
                    offsetXAnimatable.snapTo(targetValue)
                }
            }
        }
        
        // 滑动后切换页面的函数 - 优化动画逻辑
        fun animateToPage(forward: Boolean) {
            if (isAnimating) return // 防止重复触发
            
            coroutineScope.launch {
                isAnimating = true
                
                if (forward && canGoNext) {
                    // 向左滑动到屏幕外
                    offsetXAnimatable.animateTo(
                        targetValue = -screenWidth,
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = FastOutSlowInEasing
                        )
                    )
                    
                    // 切换周次
                    viewModel.nextWeek()
                    // 延迟加载数据
                    loadWeekSchedulesDelayed(viewModel.selectedWeek.value ?: 1)
                    
                    // 重置位置并从右侧滑入
                    offsetXAnimatable.snapTo(screenWidth)
                    offsetXAnimatable.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = FastOutSlowInEasing
                        )
                    )
                } 
                else if (!forward && canGoPrevious) {
                    // 向右滑动到屏幕外
                    offsetXAnimatable.animateTo(
                        targetValue = screenWidth,
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = FastOutSlowInEasing
                        )
                    )
                    
                    // 切换周次
                    viewModel.previousWeek()
                    // 延迟加载数据
                    loadWeekSchedulesDelayed(viewModel.selectedWeek.value ?: 1)
                    
                    // 重置位置并从左侧滑入
                    offsetXAnimatable.snapTo(-screenWidth)
                    offsetXAnimatable.animateTo(
                        targetValue = 0f,
                        animationSpec = tween(
                            durationMillis = 200,
                            easing = FastOutSlowInEasing
                        )
                    )
                } 
                else {
                    // 回弹到原位 - 使用更流畅的动画规格
                    offsetXAnimatable.animateTo(
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = 0.75f,
                            stiffness = 400f
                        )
                    )
                }
                
                isAnimating = false
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MatchaBgColor)
                .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        // 判断是否应该切换页面，基于拖动距离和速度
                        // 正值表示向右拖动（上一页），负值表示向左拖动（下一页）
                        val isPreviousPage = offsetXAnimatable.value > 0
                        
                        when {
                            // 距离超过阈值，切换页面
                            offsetXAnimatable.value.absoluteValue >= swipeThreshold -> {
                                animateToPage(!isPreviousPage)
                            }
                            // 速度很快，切换页面 - 调整速度阈值使更灵敏
                            velocity.absoluteValue >= 800f -> {
                                animateToPage(!isPreviousPage)
                            }
                            // 否则回弹到原位
                            else -> {
                                animateToPage(false)
                            }
                        }
                    }
                )
                .verticalScroll(rememberScrollState()) // 保留垂直滚动
        ) {
            // 顶部周选择栏
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MatchaCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 上一周按钮
                    IconButton(
                        onClick = { 
                            if (canGoPrevious && !isAnimating) { // 避免动画中重复触发
                                animateToPage(false)
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        enabled = canGoPrevious && !isAnimating
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_left), 
                            contentDescription = "上一周", 
                            tint = if (canGoPrevious && !isAnimating) MatchaGreen else MatchaTextHint
                        )
                    }
                    
                    // 当前周显示
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(
                            enabled = !isAnimating // 避免动画中点击
                        ) { 
                            if (!isAnimating) viewModel.jumpToCurrentWeek() 
                        }
                    ) {
                        Text(
                            text = "第${selectedWeek}周",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MatchaTextPrimary
                        )
                        if (selectedWeek == currentWeek) {
                            Text(
                                text = " (本周)",
                                fontSize = 14.sp,
                                color = MatchaGreen,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                    
                    // 下一周按钮
                    IconButton(
                        onClick = { 
                            if (canGoNext && !isAnimating) { // 避免动画中重复触发
                                animateToPage(true)
                            }
                        },
                        modifier = Modifier.size(48.dp),
                        enabled = canGoNext && !isAnimating
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_right), 
                            contentDescription = "下一周", 
                            tint = if (canGoNext && !isAnimating) MatchaGreen else MatchaTextHint
                        )
                    }
                }
            }
            
            // 周一到周日表头
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp) // 调整左右边距
                    .padding(bottom = 4.dp)
            ) {
                // 左侧节次栏位置占位
                Spacer(modifier = Modifier.width(40.dp)) 
                
                val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MatchaTextSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 8.dp) // 增加垂直内边距
                    )
                }
            }
            
            // 添加日期显示行
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 8.dp)
            ) {
                // 左侧节次栏位置占位
                Spacer(modifier = Modifier.width(40.dp)) 
                
                // 计算并显示当前周的日期
                val startDate = viewModel.startDate.value ?: Date()
                val calendar = Calendar.getInstance().apply { time = startDate }
                
                // 计算所选周的第一天（周一）的日期
                calendar.add(Calendar.DAY_OF_YEAR, (selectedWeek - 1) * 7)
                // 调整到周一
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val offset = if (dayOfWeek == Calendar.SUNDAY) -6 else 2 - dayOfWeek
                calendar.add(Calendar.DAY_OF_YEAR, offset)
                
                // 显示周一到周日的日期
                for (i in 0..6) {
                    if (i > 0) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    val month = calendar.get(Calendar.MONTH) + 1
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    
                    Text(
                        text = "$month.$day",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MatchaTextSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp)
                    )
                }
            }
            
            // --- 课程表网格 --- 
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp) // 调整左右边距
            ) {
                // 左侧节次栏
                Column(modifier = Modifier.width(40.dp)) {
                    for (i in 1..11) {
                        Box(
                            modifier = Modifier
                                .height(55.dp) // 调整格子高度
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$i",
                                fontSize = 13.sp,
                                color = MatchaTextSecondary
                            )
                        }
                        
                        // 在上午和下午之间添加分隔
                        if (i == 5 || i == 9) {
                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                color = MatchaDarkGreen.copy(alpha = 0.3f),
                                thickness = 2.dp
                            )
                        }
                    }
                }
                
                // 课程网格 (周一到周日)
                Row(modifier = Modifier.fillMaxSize()) {
                    for (day in 1..7) {
                        val schedulesForDay = weekSchedules[day] ?: emptyList()
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            var currentNode = 1
                            while (currentNode <= 11) {
                                val coursesAtNode = schedulesForDay.filter { it.startNode == currentNode }
                                
                                if (coursesAtNode.isNotEmpty()) {
                                    // 这个节点是某门课的开始
                                    val course = coursesAtNode.first()
                                    val nodeSpan = course.endNode - course.startNode + 1
                                    val itemHeight = (nodeSpan * 55).dp // 根据跨越的节数计算高度
                                    
                                    CourseGridItem(
                                        course = course, 
                                        selectedWeek = selectedWeek,
                                        currentWeek = currentWeek, // 使用观察到的 currentWeek
                                        modifier = Modifier.height(itemHeight),
                                        onClick = { 
                                            selectedCourse = course
                                            showDetailSheet = true
                                        }
                                    )
                                    
                                    currentNode += nodeSpan // 跳过这门课占据的节数
                                } else {
                                    // 这个节点没有课开始，是空格子
                                    Box(
                                        modifier = Modifier
                                            .height(55.dp)
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = { },
                                                onLongClick = {
                                                    // 长按显示添加课程弹窗
                                                    longPressedDay = day
                                                    longPressedNode = currentNode
                                                    showAddCourseSheet = true
                                                }
                                            )
                                    )
                                    currentNode += 1
                                }
                                
                                // 在上午和下午之间添加分隔
                                if (currentNode - 1 == 5 || currentNode - 1 == 9) {
                                    Divider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        color = MatchaDarkGreen.copy(alpha = 0.3f),
                                        thickness = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsView(viewModel: ScheduleViewModel) {
        // 状态获取和监听 (与之前类似)
        val currentWeekState = remember { mutableStateOf(viewModel.currentWeek.value ?: 1) }
        val totalWeeksState = remember { mutableStateOf(viewModel.totalWeeks.value ?: 18) }
        val startDateState = remember { mutableStateOf(viewModel.startDate.value ?: Date()) }
        val isUsingManualWeekState = remember { mutableStateOf(viewModel.isUsingManualWeek.value ?: false) }
        val timeScheduleTypeState = remember { mutableStateOf(viewModel.timeScheduleType.value ?: 0) }
        
        // 添加时间表类型选择对话框状态
        var showTimeScheduleTypeDialog by remember { mutableStateOf(false) }
        
        // 添加观察LiveData的代码
        DisposableEffect(viewModel) {
            val currentWeekObserver = Observer<Int> { week -> currentWeekState.value = week ?: 1 }
            val totalWeeksObserver = Observer<Int> { total -> totalWeeksState.value = total ?: 18 }
            val startDateObserver = Observer<Date> { date -> startDateState.value = date ?: Date() }
            val isUsingManualWeekObserver = Observer<Boolean> { isManual -> isUsingManualWeekState.value = isManual }
            val timeScheduleTypeObserver = Observer<Int> { type -> timeScheduleTypeState.value = type }

            viewModel.currentWeek.observeForever(currentWeekObserver)
            viewModel.totalWeeks.observeForever(totalWeeksObserver)
            viewModel.startDate.observeForever(startDateObserver)
            viewModel.isUsingManualWeek.observeForever(isUsingManualWeekObserver)
            viewModel.timeScheduleType.observeForever(timeScheduleTypeObserver)

            onDispose {
                viewModel.currentWeek.removeObserver(currentWeekObserver)
                viewModel.totalWeeks.removeObserver(totalWeeksObserver)
                viewModel.startDate.removeObserver(startDateObserver)
                viewModel.isUsingManualWeek.removeObserver(isUsingManualWeekObserver)
                viewModel.timeScheduleType.removeObserver(timeScheduleTypeObserver)
            }
        }
        
        // 获取当前值，以便下面使用
        val currentWeek = currentWeekState.value
        val totalWeeks = totalWeeksState.value
        val startDate = startDateState.value
        val isUsingManualWeek = isUsingManualWeekState.value
        val timeScheduleType = timeScheduleTypeState.value
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        // 添加用于输入框的状态 - 使用获取到的值初始化
        var currentWeekInput by remember(currentWeek) { mutableStateOf(currentWeek.toString()) }
        var totalWeeksInput by remember(totalWeeks) { mutableStateOf(totalWeeks.toString()) }
        
        // 添加是否修改过的状态
        var isModified by remember { mutableStateOf(false) }
        
        // 时间表类型选择对话框
        if (showTimeScheduleTypeDialog) {
            AlertDialog(
                onDismissRequest = { showTimeScheduleTypeDialog = false },
                title = { Text("选择上课时间", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        viewModel.getTimeScheduleTypes().forEach { (type, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.setTimeScheduleType(type)
                                        showTimeScheduleTypeDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = type == timeScheduleType,
                                    onClick = { 
                                        viewModel.setTimeScheduleType(type)
                                        showTimeScheduleTypeDialog = false
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MatchaGreen,
                                        unselectedColor = MatchaTextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = name,
                                    fontSize = 16.sp,
                                    color = MatchaTextPrimary
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showTimeScheduleTypeDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MatchaGreen
                        )
                    ) {
                        Text("关闭")
                    }
                }
            )
        }
        
        // 使用Scaffold布局来添加FloatingActionButton
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MatchaBgColor),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        // 保存所有设置
                        val weekNum = currentWeekInput.toIntOrNull()
                        if (weekNum != null && weekNum > 0) {
                            viewModel.setCurrentWeek(weekNum)
                        }
                        
                        val weeksTotal = totalWeeksInput.toIntOrNull()
                        if (weeksTotal != null && weeksTotal in 10..24) {
                            viewModel.setTotalWeeks(weeksTotal)
                        }
                        
                        Toast.makeText(this@ScheduleActivity, "设置已保存", Toast.LENGTH_SHORT).show()
                        isModified = false
                    },
                    containerColor = MatchaGreen,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Save, contentDescription = "保存设置")
                }
            },
            floatingActionButtonPosition = FabPosition.End,
            containerColor = MatchaBgColor // 背景色与原来保持一致
        ) { paddingValues -> // Scaffold 提供的 paddingValues 已经考虑了FAB
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    // 使用 Scaffold 提供的 paddingValues
                    .padding(paddingValues) 
            ) {
                // 顶部标题栏 (保持现有样式)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MatchaCardBg)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 返回按钮
                        IconButton(
                            onClick = { finish() }, // 假设点击返回按钮结束当前Activity
                            modifier = Modifier.size(40.dp)
                        ) {
                           Icon(painter = painterResource(id = R.drawable.ic_arrow_left), 
                                contentDescription = "返回", 
                                tint = MatchaGreen)
                        }
                        
                        Text(
                            text = "课程表设置",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = MatchaTextPrimary
                        )
                        
                        // 占位符，保持标题居中
                        Spacer(modifier = Modifier.width(40.dp))
                    }
                }
                
                // 基本设置分组
                item { SectionHeader("基本设置") }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MatchaCardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingItem(
                                iconResId = Icons.Filled.DateRange,
                                title = "开学日期",
                                value = dateFormat.format(startDate),
                                onClick = { 
                                    showDatePicker(startDate) { 
                                        viewModel.setStartDate(it) 
                                        isModified = true
                                    } 
                                }
                            )
                            
                            // 添加校区时间表选择
                            SettingItem(
                                iconResId = Icons.Filled.AccessTime,
                                title = "上课时间",
                                value = viewModel.getTimeScheduleTypeName(timeScheduleType),
                                onClick = { 
                                    showTimeScheduleTypeDialog = true
                                }
                            )
                            
                            // 修改 当前周数 设置项 - 添加自动计算/手动设置状态显示
                            EditableSettingItem(
                                iconResId = Icons.Filled.ViewWeek,
                                title = "当前周数",
                                value = currentWeekInput,
                                subtitle = if (isUsingManualWeek) "手动设置" else "自动计算",
                                onValueChange = { 
                                    currentWeekInput = it 
                                    isModified = true
                                },
                                onSave = { 
                                    val week = it.toIntOrNull()
                                    if (week != null && week > 0) {
                                        viewModel.setCurrentWeek(week)
                                        isModified = false
                                    } else {
                                        currentWeekInput = currentWeek.toString() 
                                        Toast.makeText(this@ScheduleActivity, "请输入有效的周数", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            
                            // 添加切换周数计算模式的开关
                            SettingSwitch(
                                iconResId = Icons.Filled.SyncAlt,
                                title = "自动计算周数",
                                subtitle = "根据开学日期自动计算当前周数",
                                isChecked = !isUsingManualWeek,
                                onCheckedChange = { autoEnabled ->
                                    if (autoEnabled) {
                                        // 切换到自动模式
                                        viewModel.resetToAutoWeekMode()
                                        Toast.makeText(this@ScheduleActivity, "已切换到自动计算周数模式", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // 切换到手动模式，使用当前周数
                                        viewModel.setCurrentWeek(currentWeek)
                                        Toast.makeText(this@ScheduleActivity, "已切换到手动设置周数模式", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            
                            // 修改 学期总周数 设置项 (保持不变)
                            EditableSettingItem(
                                iconResId = Icons.Filled.CalendarViewMonth,
                                title = "学期总周数",
                                value = totalWeeksInput,
                                onValueChange = { 
                                    totalWeeksInput = it 
                                    isModified = true
                                },
                                onSave = { 
                                    val weeks = it.toIntOrNull()
                                    if (weeks != null && weeks in 10..24) { 
                                        viewModel.setTotalWeeks(weeks)
                                        isModified = false
                                    } else {
                                        totalWeeksInput = totalWeeks.toString()
                                        Toast.makeText(this@ScheduleActivity, "总周数应在10-24之间", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
                
                // 显示设置分组
                item { SectionHeader("显示设置") }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MatchaCardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SettingSwitch(
                                iconResId = Icons.Filled.Weekend, // 使用 Material Icons 占位符
                                title = "显示周末",
                                subtitle = "课表中是否显示周六日",
                                isChecked = true, // 示例值，需要从ViewModel或配置获取
                                onCheckedChange = { /* TODO: 实现逻辑 */ }
                            )
                            SettingSwitch(
                                iconResId = Icons.Filled.EventAvailable, // 使用 Material Icons 占位符
                                title = "显示非本周课程",
                                subtitle = "灰色显示非当前周次的课程",
                                isChecked = false, // 示例值
                                onCheckedChange = { /* TODO: 实现逻辑 */ }
                            )
                        }
                    }
                }

                // 数据管理分组
                item { SectionHeader("数据管理") }
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MatchaCardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                         Column(modifier = Modifier.fillMaxWidth()) {
                            SettingItem(
                                iconResId = Icons.Filled.Input, // 使用 Material Icons 占位符
                                title = "导入课表",
                                value = "从教务系统导入",
                                onClick = { showImportScheduleDialog() }
                            )
                             SettingItem(
                                iconResId = Icons.Filled.DeleteSweep, // 使用 Material Icons 占位符
                                title = "清空课表",
                                value = "删除所有课程数据",
                                onClick = { 
                                    // 添加确认对话框
                                    AlertDialog.Builder(this@ScheduleActivity)
                                        .setTitle("确认清空")
                                        .setMessage("确定要删除所有课程数据吗？此操作不可恢复。")
                                        .setPositiveButton("清空") { _, _ -> 
                                            viewModel.clearAllSchedules { 
                                                // 清除完成后显示 Toast
                                                Toast.makeText(this@ScheduleActivity, "课表已清空", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .setNegativeButton("取消", null)
                                        .show()
                                }
                            )
                            // --- 添加桌面小部件按钮 --- 
                            SettingItem(
                                iconResId = Icons.Filled.Widgets, // Choose an appropriate icon
                                title = "添加桌面小部件",
                                value = "将今日课表添加到桌面",
                                onClick = { 
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        requestPinScheduleWidget()
                                    } else {
                                        Toast.makeText(
                                            this@ScheduleActivity, 
                                            "请长按桌面空白处，选择\'小部件\'，然后找到\'今日课表\'进行添加", 
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) } // 底部间距
            }
        }
    }

    // 更新 SettingItem 以包含图标
    @Composable
    private fun SettingItem(
        iconResId: androidx.compose.ui.graphics.vector.ImageVector?, // 改为接收 ImageVector
        title: String,
        subtitle: String? = null, // 副标题可选
        value: String,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 显示图标（如果提供）
            if (iconResId != null) {
                Icon(
                    imageVector = iconResId, // 使用 ImageVector
                    contentDescription = title, 
                    tint = MatchaGreen, 
                    modifier = Modifier.size(24.dp).padding(end = 12.dp)
                )
            }
            
            // 标题和副标题
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = MatchaTextPrimary,
                    fontWeight = FontWeight.Normal // 调整字重
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = MatchaTextSecondary
                    )
                }
            }
            
            // 右侧值和箭头
            Row(verticalAlignment = Alignment.CenterVertically) {
                 Text(
                    text = value,
                    fontSize = 14.sp,
                    color = MatchaTextSecondary, // 调整颜色
                    modifier = Modifier.padding(end = 4.dp)
                )
                 Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = "选择",
                    tint = MatchaTextHint, // 调整箭头颜色
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Divider(modifier = Modifier.padding(start = if (iconResId != null) 52.dp else 16.dp, end = 16.dp), color = MatchaDivider, thickness = 0.5.dp)
    }

    // 更新 SettingSwitch 以包含图标
    @Composable
    private fun SettingSwitch(
        iconResId: androidx.compose.ui.graphics.vector.ImageVector?, // 改为接收 ImageVector
        title: String,
        subtitle: String? = null, // 副标题可选
        isChecked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp), // 调整垂直边距
            verticalAlignment = Alignment.CenterVertically
        ) {
             if (iconResId != null) {
                Icon(
                    imageVector = iconResId, // 使用 ImageVector
                    contentDescription = title, 
                    tint = MatchaGreen, 
                    modifier = Modifier.size(24.dp).padding(end = 12.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = MatchaTextPrimary,
                     fontWeight = FontWeight.Normal
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = MatchaTextSecondary
                    )
                }
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MatchaGreen,
                    checkedTrackColor = MatchaLightGreen,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = MatchaDivider
                )
            )
        }
         Divider(modifier = Modifier.padding(start = if (iconResId != null) 52.dp else 16.dp), color = MatchaDivider, thickness = 0.5.dp)
    }
    
    @Composable
    private fun BottomNavigationBar(
        currentView: Int,
        onViewChanged: (Int) -> Unit
    ) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = MatchaCardBg,
            tonalElevation = 8.dp // 使用 Material 3 的 Elevation
        ) {
             // --- 底部导航项 --- 
             // 请确保你有名为 ic_daily_view, ic_weekly_view, ic_settings 的图标资源
             NavigationBarItem(
                selected = currentView == VIEW_DAILY,
                onClick = { onViewChanged(VIEW_DAILY) },
                icon = { Icon(Icons.Filled.Today, contentDescription = "日视图") }, // 使用 Material Icons
                label = { Text("日视图", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MatchaGreen,
                    selectedTextColor = MatchaGreen,
                    unselectedIconColor = MatchaTextHint,
                    unselectedTextColor = MatchaTextHint,
                    indicatorColor = MatchaLightGreen.copy(alpha = 0.3f) // 指示器颜色
                )
            )
             NavigationBarItem(
                selected = currentView == VIEW_WEEKLY,
                onClick = { onViewChanged(VIEW_WEEKLY) },
                icon = { Icon(Icons.Filled.ViewWeek, contentDescription = "周视图") }, // 使用 Material Icons
                label = { Text("周视图", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MatchaGreen,
                    selectedTextColor = MatchaGreen,
                    unselectedIconColor = MatchaTextHint,
                    unselectedTextColor = MatchaTextHint,
                    indicatorColor = MatchaLightGreen.copy(alpha = 0.3f)
                )
            )
             NavigationBarItem(
                selected = currentView == VIEW_SETTINGS,
                onClick = { onViewChanged(VIEW_SETTINGS) },
                icon = { Icon(Icons.Filled.Settings, contentDescription = "设置") }, // 使用 Material Icons
                label = { Text("设置", fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MatchaGreen,
                    selectedTextColor = MatchaGreen,
                    unselectedIconColor = MatchaTextHint,
                    unselectedTextColor = MatchaTextHint,
                    indicatorColor = MatchaLightGreen.copy(alpha = 0.3f)
                )
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
        // 使用startActivityForResult启动导入活动
        val intent = Intent(this, ScheduleImportActivity::class.java)
        startActivityForResult(intent, REQUEST_IMPORT_SCHEDULE)
    }
    
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_IMPORT_SCHEDULE && resultCode == RESULT_OK) {
            // 获取导入的课程数量
            val count = data?.getIntExtra("IMPORTED_COURSES_COUNT", 0) ?: 0
            
            // 强制重新加载当前周的课表
            viewModel.loadWeekSchedules(viewModel.selectedWeek.value ?: 1)
            
            // 强制重新加载当天的课表
            viewModel.loadTodaySchedules()
            
            // 提示用户导入成功
            Toast.makeText(this, "成功导入 $count 门课程", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 课程详情对话框
    @Composable
    private fun CourseDetailSheet(
        course: ScheduleData,
        onDismiss: () -> Unit,
        onSave: (ScheduleData) -> Unit,
        onDelete: (ScheduleData) -> Unit
    ) {
        // 创建可编辑的课程状态
        var courseName by remember { mutableStateOf(course.name) }
        var courseClassroom by remember { mutableStateOf(course.classroom) }
        var courseTeacher by remember { mutableStateOf(course.teacher) }
        var courseStartNode by remember { mutableStateOf(course.startNode.toString()) }
        var courseEndNode by remember { mutableStateOf(course.endNode.toString()) }
        var courseStartWeek by remember { mutableStateOf(course.startWeek.toString()) }
        var courseEndWeek by remember { mutableStateOf(course.endWeek.toString()) }
        
        // 星期几中文映射
        val weekDayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val weekDay = weekDayNames[course.weekDay - 1]
        
        // 节次时间映射
        val timeNodes = viewModel.getTimeNodes()
        val startTime = timeNodes.find { it.node == course.startNode }?.startTime ?: ""
        val endTime = timeNodes.find { it.node == course.endNode }?.endTime ?: ""
        
        // 编辑状态
        var isEditing by remember { mutableStateOf(false) }
        var showDeleteConfirm by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "编辑课程" else "课程详情",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatchaTextPrimary
                    )
                    // 编辑/保存按钮
                    IconButton(
                        onClick = { 
                            if (isEditing) {
                                // 尝试保存编辑
                                try {
                                    val updatedCourse = course.copy(
                                        name = courseName,
                                        classroom = courseClassroom,
                                        teacher = courseTeacher,
                                        startNode = courseStartNode.toIntOrNull() ?: course.startNode,
                                        endNode = courseEndNode.toIntOrNull() ?: course.endNode,
                                        startWeek = courseStartWeek.toIntOrNull() ?: course.startWeek,
                                        endWeek = courseEndWeek.toIntOrNull() ?: course.endWeek
                                    )
                                    onSave(updatedCourse)
                                } catch (e: Exception) {
                                    // 处理转换错误
                                    Toast.makeText(this@ScheduleActivity, "请输入有效的数值", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                isEditing = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isEditing) Icons.Filled.Save else Icons.Filled.Edit,
                            contentDescription = if (isEditing) "保存" else "编辑",
                            tint = MatchaGreen
                        )
                    }
                }
            },
            text = { 
                // 课程内容区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()) // 添加垂直滚动
                ) {
                    // 课程名称
                    DetailItem(
                        title = "课程名称",
                        isEditing = isEditing,
                        value = courseName,
                        onValueChange = { courseName = it }
                    )
                    
                    // 教室
                    DetailItem(
                        title = "教室",
                        isEditing = isEditing,
                        value = courseClassroom,
                        onValueChange = { courseClassroom = it }
                    )
                    
                    // 教师
                    DetailItem(
                        title = "教师",
                        isEditing = isEditing,
                        value = courseTeacher,
                        onValueChange = { courseTeacher = it }
                    )
                    
                    // 星期几（不可编辑）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "星期",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MatchaTextPrimary,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            text = weekDay,
                            fontSize = 16.sp,
                            color = MatchaTextPrimary
                        )
                    }
                    
                    // 节次
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "节次",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MatchaTextPrimary,
                            modifier = Modifier.width(80.dp)
                        )
                        
                        if (isEditing) {
                            // 编辑模式：显示输入框
                            OutlinedTextField(
                                value = courseStartNode,
                                onValueChange = { courseStartNode = it },
                                modifier = Modifier.width(60.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MatchaGreen,
                                    unfocusedBorderColor = MatchaDivider
                                )
                            )
                            Text(
                                text = " - ",
                                fontSize = 16.sp,
                                color = MatchaTextPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            OutlinedTextField(
                                value = courseEndNode,
                                onValueChange = { courseEndNode = it },
                                modifier = Modifier.width(60.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MatchaGreen,
                                    unfocusedBorderColor = MatchaDivider
                                )
                            )
                        } else {
                            // 查看模式：显示文本
                            Text(
                                text = "${course.startNode}-${course.endNode}节 ($startTime-$endTime)",
                                fontSize = 16.sp,
                                color = MatchaTextPrimary
                            )
                        }
                    }
                    
                    // 周数
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "周数",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MatchaTextPrimary,
                            modifier = Modifier.width(80.dp)
                        )
                        
                        if (isEditing) {
                            // 编辑模式：显示输入框
                            OutlinedTextField(
                                value = courseStartWeek,
                                onValueChange = { courseStartWeek = it },
                                modifier = Modifier.width(60.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MatchaGreen,
                                    unfocusedBorderColor = MatchaDivider
                                )
                            )
                            Text(
                                text = " - ",
                                fontSize = 16.sp,
                                color = MatchaTextPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                            OutlinedTextField(
                                value = courseEndWeek,
                                onValueChange = { courseEndWeek = it },
                                modifier = Modifier.width(60.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MatchaGreen,
                                    unfocusedBorderColor = MatchaDivider
                                )
                            )
                        } else {
                            // 查看模式：显示文本
                            Text(
                                text = "第${course.startWeek}-${course.endWeek}周",
                                fontSize = 16.sp,
                                color = MatchaTextPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                // 底部按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { showDeleteConfirm = true }
                    ) {
                        Text("删除", color = Color(0xFFE57373))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { onDismiss() }
                    ) {
                        Text("取消")
                    }
                }
            },
            containerColor = MatchaCardBg
        )
        
        // 删除确认对话框
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("确认删除") },
                text = { Text("确定要删除《${course.name}》这门课程吗？此操作不可恢复。") },
                confirmButton = {
                    TextButton(
                        onClick = { 
                            onDelete(course)
                            showDeleteConfirm = false // 关闭确认对话框
                        }
                    ) {
                        Text("删除", color = Color(0xFFE57373))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteConfirm = false }
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
    
    // 课程详情项目
    @Composable
    private fun DetailItem(
        title: String,
        isEditing: Boolean,
        value: String,
        onValueChange: (String) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MatchaTextPrimary,
                modifier = Modifier.width(80.dp)
            )
            
            if (isEditing) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MatchaGreen,
                        unfocusedBorderColor = MatchaDivider
                    )
                )
            } else {
                Text(
                    text = value,
                    fontSize = 16.sp,
                    color = MatchaTextPrimary
                )
            }
        }
    }
    
    // ---- 新增可编辑的设置项 ----
    @Composable
    private fun EditableSettingItem(
        iconResId: androidx.compose.ui.graphics.vector.ImageVector?,
        title: String,
        value: String, 
        onValueChange: (String) -> Unit,
        onSave: (String) -> Unit, // 回调函数，在用户完成输入时调用
        keyboardType: KeyboardType = KeyboardType.Number, // 默认为数字键盘
        subtitle: String? = null
    ) {
        val focusManager = LocalFocusManager.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp), // 调整垂直边距
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 显示图标（如果提供）
            if (iconResId != null) {
                Icon(
                    imageVector = iconResId, 
                    contentDescription = title, 
                    tint = MatchaGreen, 
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )
            }
            
            // 标题和副标题列
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 标题
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = MatchaTextPrimary,
                    fontWeight = FontWeight.Normal
                )
                
                // 如果有副标题，显示它
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MatchaTextSecondary,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            // 输入框
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .width(80.dp) // 固定宽度
                    .padding(start = 8.dp),
                textStyle = TextStyle(textAlign = TextAlign.End),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = { 
                        onSave(value) // 用户点击完成时保存
                        focusManager.clearFocus() // 清除焦点
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MatchaGreen,
                    unfocusedBorderColor = MatchaDivider,
                    focusedContainerColor = MatchaBgColor.copy(alpha = 0.5f), // 聚焦时背景色
                    unfocusedContainerColor = Color.Transparent // 未聚焦时透明
                )
            )
        }
        Divider(modifier = Modifier.padding(start = if (iconResId != null) 52.dp else 16.dp, end = 16.dp), color = MatchaDivider, thickness = 0.5.dp)
    }
    
    // 课程格子 Composable
    @Composable
    private fun CourseGridItem(
        course: ScheduleData,
        selectedWeek: Int, // 传入当前选择的周次
        currentWeek: Int,  // 传入实际当前周次
        modifier: Modifier = Modifier,
        onClick: () -> Unit = {}
    ) {
        // 获取今天是星期几
        val calendar = Calendar.getInstance()
        // Calendar中周日是1，周一是2，所以需要转换
        val convertedTodayWeekDay = if (calendar.get(Calendar.DAY_OF_WEEK) == 1) 7 else calendar.get(Calendar.DAY_OF_WEEK) - 1
        
        // 判断是否为今天的课程（当前周+当天星期）
        // 直接使用传入的参数
        val isToday = course.weekDay == convertedTodayWeekDay && selectedWeek == currentWeek
        
        // 设置颜色：当天课程使用橙黄色，否则使用浅绿色
        val backgroundColor = if (isToday) { // 移除 highlightToday 参数的检查
            Color(0xFFFFAB00) // 橙黄色
        } else {
            MatchaLightGreen.copy(alpha = 0.7f)
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(2.dp)
                .clickable(onClick = onClick), // 添加点击事件
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), 
            border = BorderStroke(0.5.dp, backgroundColor.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp, vertical = 4.dp), // 调整内边距
                horizontalAlignment = Alignment.Start, // 水平居左对齐
                verticalArrangement = Arrangement.Top // 垂直居顶对齐
            ) {
                Text(
                    text = course.name,
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start, // 文本左对齐
                    color = MatchaTextPrimary,
                    maxLines = Int.MAX_VALUE, // 移除行数限制
                    overflow = androidx.compose.ui.text.style.TextOverflow.Clip, // 使用Clip避免Ellipsis
                    lineHeight = 14.sp // 设置较小的行高以减小行间距
                )
                Spacer(modifier = Modifier.height(1.dp)) // 进一步减小间距
                Text(
                    text = course.classroom,
                    fontSize = 10.sp, 
                    textAlign = TextAlign.Start, // 文本左对齐
                    color = MatchaTextSecondary,
                    maxLines = Int.MAX_VALUE, // 移除行数限制
                    overflow = androidx.compose.ui.text.style.TextOverflow.Clip, // 使用Clip避免Ellipsis
                    lineHeight = 12.sp // 设置较小的行高
                )
            }
        }
    }
    
    @Composable
    private fun AddCourseSheet(
        weekDay: Int,
        startNode: Int,
        selectedWeek: Int,
        onDismiss: () -> Unit,
        onAdd: (ScheduleData) -> Unit
    ) {
        // 创建可编辑的课程状态
        var courseName by remember { mutableStateOf("") }
        var courseClassroom by remember { mutableStateOf("") }
        var courseTeacher by remember { mutableStateOf("") }
        var courseStartNode by remember { mutableStateOf(startNode.toString()) }
        var courseEndNode by remember { mutableStateOf(startNode.toString()) }
        var courseStartWeek by remember { mutableStateOf(selectedWeek.toString()) }
        var courseEndWeek by remember { mutableStateOf(selectedWeek.toString()) }
        
        // 星期几中文映射
        val weekDayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val weekDayText = weekDayNames[weekDay - 1]
        
        // 输入验证状态
        var isNameValid by remember { mutableStateOf(true) }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "添加课程",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MatchaTextPrimary
                    )
                }
            },
            text = { 
                // 课程内容区域
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()) // 添加垂直滚动
                ) {
                    // 课程名称
                    Column {
                        DetailItem(
                            title = "课程名称",
                            isEditing = true,
                            value = courseName,
                            onValueChange = { 
                                courseName = it
                                isNameValid = courseName.isNotEmpty()
                            }
                        )
                        if (!isNameValid) {
                            Text(
                                text = "课程名称不能为空",
                                color = Color.Red,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 80.dp)
                            )
                        }
                    }
                    
                    // 教室
                    DetailItem(
                        title = "教室",
                        isEditing = true,
                        value = courseClassroom,
                        onValueChange = { courseClassroom = it }
                    )
                    
                    // 教师
                    DetailItem(
                        title = "教师",
                        isEditing = true,
                        value = courseTeacher,
                        onValueChange = { courseTeacher = it }
                    )
                    
                    // 星期几（不可编辑）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "星期",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MatchaTextPrimary,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            text = weekDayText,
                            fontSize = 16.sp,
                            color = MatchaTextPrimary
                        )
                    }
                    
                    // 节次
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "节次",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MatchaTextPrimary,
                            modifier = Modifier.width(80.dp)
                        )
                        
                        // 编辑模式：显示输入框
                        OutlinedTextField(
                            value = courseStartNode,
                            onValueChange = { courseStartNode = it },
                            modifier = Modifier.width(60.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MatchaGreen,
                                unfocusedBorderColor = MatchaDivider
                            )
                        )
                        Text(
                            text = " - ",
                            fontSize = 16.sp,
                            color = MatchaTextPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        OutlinedTextField(
                            value = courseEndNode,
                            onValueChange = { courseEndNode = it },
                            modifier = Modifier.width(60.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MatchaGreen,
                                unfocusedBorderColor = MatchaDivider
                            )
                        )
                    }
                    
                    // 周数
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "周数",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MatchaTextPrimary,
                            modifier = Modifier.width(80.dp)
                        )
                        
                        // 编辑模式：显示输入框
                        OutlinedTextField(
                            value = courseStartWeek,
                            onValueChange = { courseStartWeek = it },
                            modifier = Modifier.width(60.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MatchaGreen,
                                unfocusedBorderColor = MatchaDivider
                            )
                        )
                        Text(
                            text = " - ",
                            fontSize = 16.sp,
                            color = MatchaTextPrimary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        OutlinedTextField(
                            value = courseEndWeek,
                            onValueChange = { courseEndWeek = it },
                            modifier = Modifier.width(60.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MatchaGreen,
                                unfocusedBorderColor = MatchaDivider
                            )
                        )
                    }
                }
            },
            confirmButton = { 
                Button(
                    onClick = { 
                        // 验证课程名不为空
                        if (courseName.isEmpty()) {
                            isNameValid = false
                            return@Button
                        }
                        
                        // 验证和转换数值
                        val startNodeValue = courseStartNode.toIntOrNull() ?: startNode
                        val endNodeValue = courseEndNode.toIntOrNull() ?: startNode
                        val startWeekValue = courseStartWeek.toIntOrNull() ?: selectedWeek
                        val endWeekValue = courseEndWeek.toIntOrNull() ?: selectedWeek
                        
                        // 验证节次和周数
                        if (startNodeValue > endNodeValue) {
                            Toast.makeText(this@ScheduleActivity, "结束节次不能小于开始节次", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        if (endNodeValue > 11) {
                            Toast.makeText(this@ScheduleActivity, "节次数不能大于11", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        if (startWeekValue > endWeekValue) {
                            Toast.makeText(this@ScheduleActivity, "结束周次不能小于开始周次", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        if (endWeekValue > (viewModel.totalWeeks.value ?: 18)) {
                            Toast.makeText(this@ScheduleActivity, "周次超出总周数范围", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        
                        // 创建新课程并添加
                        val newCourse = ScheduleData(
                            id = 0, // Room会自动生成ID
                            name = courseName,
                            classroom = courseClassroom,
                            teacher = courseTeacher,
                            weekDay = weekDay,
                            startNode = startNodeValue,
                            endNode = endNodeValue,
                            startWeek = startWeekValue,
                            endWeek = endWeekValue
                        )
                        
                        onAdd(newCourse)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MatchaGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onDismiss() }
                ) {
                    Text("取消")
                }
            },
            containerColor = MatchaCardBg
        )
    }
    
    companion object {
        const val VIEW_DAILY = 0
        const val VIEW_WEEKLY = 1
        const val VIEW_SETTINGS = 2
        const val REQUEST_IMPORT_SCHEDULE = 1001
    }
    
    // --- Helper function to request pinning the widget --- 
    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestPinScheduleWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(this)
        val myProvider = ComponentName(this, ScheduleWidgetProvider::class.java)

        if (appWidgetManager.isRequestPinAppWidgetSupported) {
            // Create the PendingIntent object only if your app needs to be notified
            // that the user allowed the widget to be pinned. Note that, if the pinning
            // fails, your app isn't notified.
            // val successCallback = PendingIntent.getBroadcast(..)

            // Request pinning the widget
            appWidgetManager.requestPinAppWidget(myProvider, null, null)
        } else {
            // Fallback for devices that support O+ but not pinning
            Toast.makeText(this, "您的设备不支持直接添加小部件，请手动添加", Toast.LENGTH_LONG).show()
        }
    }
} 