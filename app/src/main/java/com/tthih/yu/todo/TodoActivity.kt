package com.tthih.yu.todo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tthih.yu.R
import java.text.SimpleDateFormat
import java.util.*

class TodoActivity : ComponentActivity() {
    
    // 定义梦幻抹茶绿主题的颜色常量 - 更加精致和现代化的配色
    private val MatchaGreen = Color(0xFF7CB342) // 主色
    private val MatchaLightGreen = Color(0xFFDCEDC8) // 浅绿色
    private val MatchaDarkGreen = Color(0xFF558B2F) // 深绿色
    private val MatchaBgColor = Color(0xFFF8FAF5) // 背景色
    private val MatchaTextPrimary = Color(0xFF263238) // 主文本色
    private val MatchaTextSecondary = Color(0xFF546E7A) // 次要文本色
    private val MatchaTextHint = Color(0xFF90A4AE) // 提示文本色
    private val MatchaCardBg = Color(0xFFFFFFFF) // 卡片背景色
    private val MatchaDivider = Color(0xFFEEF4EA) // 分割线颜色
    
    // 优先级颜色
    private val PriorityHigh = Color(0xFFF44336) // 高优先级 - 红色
    private val PriorityMedium = Color(0xFFFF9800) // 中优先级 - 橙色
    private val PriorityLow = Color(0xFF4CAF50) // 低优先级 - 绿色
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo)
        
        val viewModel = ViewModelProvider(this, 
            ViewModelProvider.AndroidViewModelFactory.getInstance(application))[TodoViewModel::class.java]
        
        val composeView = findViewById<ComposeView>(R.id.compose_view)
        
        composeView.setContent {
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
                TodoScreen(viewModel)
            }
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TodoScreen(viewModel: TodoViewModel) {
        val todos by viewModel.filteredTodos.collectAsState(initial = emptyList())
        val tags by viewModel.availableTags.collectAsState(initial = emptySet())
        val currentTag by viewModel.currentTag.collectAsState(initial = null)
        val isAddDialogVisible by viewModel.isAddDialogVisible.collectAsState()
        val todoBeingEdited by viewModel.todoBeingEdited.collectAsState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MatchaBgColor)
                .padding(top = 16.dp)
        ) {
            // 顶部导航栏
            TopBar(
                onBackClick = { finish() }
            )
            
            // 标签过滤器 - 始终显示，无论是否有标签
            TagFilterSection(
                tags = tags,
                currentTag = currentTag,
                onTagSelected = { viewModel.setFilter(it) }
            )
            
            // 内容区域
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (todos.isEmpty()) {
                    // 空状态显示
                    EmptyStateMessage(
                        currentTag = currentTag,
                        onClearFilter = { viewModel.setFilter(null) }
                    )
                } else {
                // 待办事项列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(todos, key = { it.id }) { todo ->
                            EnhancedTodoItem(
                            todo = todo,
                            onToggleCompleted = { viewModel.toggleTodoCompleted(todo.id) },
                                onDelete = { viewModel.deleteTodo(todo.id) },
                                onEdit = { viewModel.showEditDialog(todo) }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // 为FAB留出空间
                        }
                    }
                }
                
                // 添加按钮
                FloatingActionButton(
                    onClick = { viewModel.showAddDialog() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 24.dp, end = 8.dp)
                        .shadow(8.dp, CircleShape),
                    containerColor = MatchaGreen,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加待办",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        // 添加/编辑待办对话框
        if (isAddDialogVisible) {
            EnhancedTodoDialog(
                todoToEdit = todoBeingEdited,
                onDismiss = { viewModel.hideDialog() },
                onSave = { title, description, dueDate, priority, tag ->
                    viewModel.saveTodo(title, description, dueDate, priority, tag)
                }
            )
        }
    }
    
    @Composable
    fun TopBar(onBackClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MatchaGreen
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 返回按钮
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // 标题
                Text(
                    text = "智能待办清单",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
                
                // 右侧图标
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "待办列表",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
    
    @Composable
    fun TagFilterSection(
        tags: Set<String>,
        currentTag: String?,
        onTagSelected: (String?) -> Unit
    ) {
        val viewModel = ViewModelProvider(this, 
            ViewModelProvider.AndroidViewModelFactory.getInstance(application))[TodoViewModel::class.java]
            
        val isTagInputVisible by viewModel.isTagInputVisible.collectAsState()
        val newTagName by viewModel.newTagName.collectAsState()
        
        // 长按删除标签相关状态
        var tagToDelete by remember { mutableStateOf<String?>(null) }
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }
        
        // 删除确认对话框
        if (showDeleteConfirmDialog && tagToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteConfirmDialog = false
                    tagToDelete = null
                },
                title = {
                    Text(
                        text = "删除标签",
                        fontWeight = FontWeight.Bold,
                        color = MatchaTextPrimary
                    )
                },
                text = {
                    Text(
                        text = "确定要删除标签\"${tagToDelete}\"吗？与此标签关联的待办事项将移除标签属性。",
                        color = MatchaTextPrimary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            tagToDelete?.let { viewModel.removeTag(it) }
                            showDeleteConfirmDialog = false
                            tagToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red.copy(alpha = 0.8f)
                        )
                    ) {
                        Text("删除")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { 
                            showDeleteConfirmDialog = false
                            tagToDelete = null
                        }
                    ) {
                        Text("取消")
                    }
                },
                containerColor = MatchaCardBg
            )
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MatchaCardBg
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // 标题和添加标签按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "按标签筛选",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MatchaTextPrimary
                    )
                    
                    IconButton(
                        onClick = { 
                            if (isTagInputVisible) {
                                viewModel.hideTagInput()
                    } else {
                                viewModel.showTagInput()
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isTagInputVisible) MatchaGreen.copy(alpha = 0.2f)
                                else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = if (isTagInputVisible) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = if (isTagInputVisible) "取消添加" else "添加标签",
                            tint = MatchaGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // 标签输入区域
                AnimatedVisibility(visible = isTagInputVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTagName,
                            onValueChange = { viewModel.setNewTagName(it) },
                            placeholder = { Text("输入新标签名称") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = MatchaGreen,
                                unfocusedIndicatorColor = MatchaTextHint.copy(alpha = 0.5f),
                                focusedLabelColor = MatchaGreen,
                                unfocusedLabelColor = MatchaTextHint
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        IconButton(
                            onClick = { viewModel.addCustomTag() },
                            enabled = newTagName.trim().isNotEmpty(),
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MatchaGreen)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "添加",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 标签列表
                if (tags.isEmpty() && !isTagInputVisible) {
                    // 显示空状态提示
                    Text(
                        text = "尚无标签，点击+按钮添加第一个标签",
                        fontSize = 14.sp,
                        color = MatchaTextSecondary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    // 所有标签显示
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        // 添加"全部"选项
                        item {
                            TagChip(
                                text = "全部",
                                isSelected = currentTag == null,
                                onClick = { onTagSelected(null) },
                                onLongClick = null  // "全部"标签不能删除
                            )
                        }
                        
                        // 添加所有标签
                        items(tags.toList()) { tag ->
                            TagChip(
                                text = tag,
                                isSelected = tag == currentTag,
                                onClick = { onTagSelected(tag) },
                                onLongClick = { 
                                    tagToDelete = tag
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun TagChip(
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)?  // 可空参数，为null时无法长按删除
    ) {
        val backgroundColor = if (isSelected) MatchaGreen else MatchaLightGreen
        val textColor = if (isSelected) Color.White else MatchaTextPrimary
        val borderColor = if (isSelected) MatchaDarkGreen else MatchaLightGreen
        
        // 为长按添加动画效果
        var isPressed by remember { mutableStateOf(false) }
        val scale by animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec = tween(durationMillis = 150),
            label = "scaleAnimation"
        )
        
        Surface(
            modifier = Modifier
                .clip(CircleShape)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = CircleShape,
            color = backgroundColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Box(
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .then(
                        if (onLongClick != null) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = { 
                                        onLongClick() 
                                    },
                                    onPress = {
                                        isPressed = true
                                        tryAwaitRelease()
                                        isPressed = false
                                    }
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = text,
                        fontSize = 14.sp,
                        color = textColor
                    )
                    
                    if (onLongClick != null) {
                        AnimatedVisibility(visible = isPressed) {
                            Row {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "长按删除",
                                    tint = textColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    fun EmptyStateMessage(
        currentTag: String?,
        onClearFilter: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (currentTag != null) Icons.Default.FilterList else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MatchaGreen.copy(alpha = 0.7f),
                modifier = Modifier.size(64.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = if (currentTag != null) {
                    "没有找到\"$currentTag\"标签的待办事项"
                } else {
                    "你的待办清单为空"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MatchaTextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (currentTag != null) {
                    "尝试清除筛选器或添加新的待办事项"
                } else {
                    "点击右下角的加号按钮创建你的第一个待办"
                },
                fontSize = 16.sp,
                color = MatchaTextSecondary,
                textAlign = TextAlign.Center
            )
            
            if (currentTag != null) {
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onClearFilter,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MatchaGreen
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterAltOff,
                        contentDescription = "清除筛选器",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("清除筛选器")
                }
            }
        }
    }
    
    @Composable
    fun EnhancedTodoItem(
        todo: Todo,
        onToggleCompleted: () -> Unit,
        onDelete: () -> Unit,
        onEdit: () -> Unit
    ) {
        var expanded by remember { mutableStateOf(false) }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MatchaCardBg
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // 顶部行 - 标题、优先级标签和操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 待办事项标题和完成状态指示器
                    Row(
                        modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 完成状态复选框
                        Checkbox(
                            checked = todo.isCompleted,
                            onCheckedChange = { onToggleCompleted() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MatchaGreen,
                                uncheckedColor = MatchaTextSecondary
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // 标题
                Text(
                    text = todo.title,
                    fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                    color = if (todo.isCompleted) MatchaTextSecondary else MatchaTextPrimary,
                    textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = if (expanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // 优先级标签
                    PriorityBadge(priority = todo.priority)
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 展开/折叠图标
                    Box(
                    modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { expanded = !expanded }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val rotation by animateFloatAsState(
                            targetValue = if (expanded) 180f else 0f,
                            animationSpec = tween(durationMillis = 300),
                            label = "rotateAnimation"
                        )
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "折叠" else "展开",
                            tint = MatchaTextSecondary,
                            modifier = Modifier.rotate(rotation)
                        )
                    }
                }
                
                // 展开内容
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Column(modifier = Modifier.padding(start = 32.dp, top = 8.dp)) {
                        // 描述
                        if (todo.description.isNotEmpty()) {
                Text(
                                text = todo.description,
                                fontSize = 14.sp,
                                color = MatchaTextSecondary,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            
                            Divider(
                                color = MatchaDivider,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        
                        // 底部信息区
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 日期信息
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                // 创建日期
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarToday,
                                        contentDescription = null,
                                        tint = MatchaTextHint,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "创建: ${todo.formattedCreatedDate}",
                                        fontSize = 12.sp,
                                        color = MatchaTextHint
                                    )
                                }
                                
                                // 截止日期（如果有）
                                if (todo.dueDate != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (todo.isOverdue && !todo.isCompleted) 
                                                Icons.Outlined.Alarm else Icons.Outlined.AccessTime,
                                            contentDescription = null,
                                            tint = if (todo.isOverdue && !todo.isCompleted) Color.Red else MatchaTextHint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "截止: ${todo.formattedDueDate}",
                                            fontSize = 12.sp,
                                            color = if (todo.isOverdue && !todo.isCompleted) Color.Red else MatchaTextHint
                                        )
                                    }
                                }
                                
                                // 标签（如果有）
                                if (todo.tag.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.Label,
                                            contentDescription = null,
                                            tint = MatchaTextHint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                Text(
                                            text = todo.tag,
                    fontSize = 12.sp,
                                            color = MatchaTextHint
                                        )
                                    }
                                }
                            }
                            
                            // 操作按钮
                            Row {
                                // 编辑按钮
                                IconButton(
                                    onClick = onEdit,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "编辑",
                                        tint = MatchaGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                
                // 删除按钮
                IconButton(
                    onClick = onDelete,
                                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(20.dp)
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
    fun PriorityBadge(priority: Priority) {
        val color = when (priority) {
            Priority.HIGH -> PriorityHigh
            Priority.MEDIUM -> PriorityMedium
            Priority.LOW -> PriorityLow
        }
        
        val label = when (priority) {
            Priority.HIGH -> "高"
            Priority.MEDIUM -> "中"
            Priority.LOW -> "低"
        }
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun EnhancedTodoDialog(
        todoToEdit: Todo?,
        onDismiss: () -> Unit,
        onSave: (String, String, String?, Priority, String) -> Unit
    ) {
        val isEditing = todoToEdit != null
        var title by remember { mutableStateOf(todoToEdit?.title ?: "") }
        var description by remember { mutableStateOf(todoToEdit?.description ?: "") }
        var dueDate by remember { mutableStateOf(todoToEdit?.formattedDueDate) }
        var priority by remember { mutableStateOf(todoToEdit?.priority ?: Priority.MEDIUM) }
        var tag by remember { mutableStateOf(todoToEdit?.tag ?: "") }
        
        val viewModel = ViewModelProvider(this, 
            ViewModelProvider.AndroidViewModelFactory.getInstance(application))[TodoViewModel::class.java]
            
        val availableTags by viewModel.availableTags.collectAsState(initial = emptySet())
        
        // 是否显示标签选择展开菜单
        var isTagMenuExpanded by remember { mutableStateOf(false) }
        
        // 日期选择器状态
        var showDatePicker by remember { mutableStateOf(false) }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = todoToEdit?.dueDate?.time
        )
        
        // 格式化选择的日期
        val selectedDate = remember(datePickerState.selectedDateMillis) {
            datePickerState.selectedDateMillis?.let { millis ->
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
            }
        }
        
        // 当日期选择器选择日期后，更新dueDate状态
        LaunchedEffect(selectedDate) {
            if (selectedDate != null) {
                dueDate = selectedDate
            }
        }
        
        // 显示日期选择器对话框
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                colors = DatePickerDefaults.colors(
                    containerColor = MatchaCardBg
                ),
                shape = RoundedCornerShape(24.dp),
                confirmButton = {
                    Button(
                        onClick = { 
                            showDatePicker = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MatchaGreen
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "确认",
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { 
                            showDatePicker = false
                        },
                        border = BorderStroke(1.dp, MatchaGreen),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "取消", 
                            color = MatchaGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp, start = 12.dp, end = 12.dp)
                ) {
                    // 自定义标题栏
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MatchaGreen.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "选择截止日期",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MatchaGreen
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = MatchaGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedDate ?: "未选择日期",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MatchaTextPrimary
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 日期选择器
                    DatePicker(
                        state = datePickerState,
                        showModeToggle = false,
                        title = null,
                        headline = null,
                        colors = DatePickerDefaults.colors(
                            containerColor = MatchaCardBg,
                            weekdayContentColor = MatchaTextSecondary,
                            subheadContentColor = MatchaTextSecondary,
                            yearContentColor = MatchaTextPrimary,
                            currentYearContentColor = MatchaGreen,
                            selectedYearContentColor = Color.White,
                            selectedYearContainerColor = MatchaGreen,
                            dayContentColor = MatchaTextPrimary,
                            selectedDayContentColor = Color.White,
                            selectedDayContainerColor = MatchaGreen,
                            todayContentColor = MatchaGreen,
                            todayDateBorderColor = MatchaGreen
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = MatchaCardBg,
            title = {
                Text(
                    text = if (isEditing) "编辑待办事项" else "添加待办事项",
                    fontWeight = FontWeight.Bold,
                    color = MatchaTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    // 标题输入
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("标题") },
                        placeholder = { Text("输入待办事项...") },
                    singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = MatchaGreen,
                            unfocusedIndicatorColor = MatchaTextHint.copy(alpha = 0.5f),
                            focusedLabelColor = MatchaGreen,
                            unfocusedLabelColor = MatchaTextHint
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 描述输入
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("描述（可选）") },
                        placeholder = { Text("输入详细描述...") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = MatchaGreen,
                            unfocusedIndicatorColor = MatchaTextHint.copy(alpha = 0.5f),
                            focusedLabelColor = MatchaGreen,
                            unfocusedLabelColor = MatchaTextHint
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 截止日期选择按钮
                    Text(
                        text = "截止日期（可选）",
                        color = MatchaTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 截止日期选择区域
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MatchaTextHint.copy(alpha = 0.5f)),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MatchaGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = dueDate ?: "点击选择日期",
                                fontSize = 16.sp,
                                color = if (dueDate != null) MatchaTextPrimary else MatchaTextHint
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            if (dueDate != null) {
                                IconButton(
                                    onClick = { dueDate = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "清除日期",
                                        tint = MatchaTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 优先级选择
                    Text(
                        text = "优先级",
                        color = MatchaTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        PriorityOption(
                            text = "低",
                            icon = Icons.Default.KeyboardArrowDown,
                            selected = priority == Priority.LOW,
                            color = PriorityLow,
                            onClick = { priority = Priority.LOW }
                        )
                        
                        PriorityOption(
                            text = "中",
                            icon = Icons.Default.Remove,
                            selected = priority == Priority.MEDIUM,
                            color = PriorityMedium,
                            onClick = { priority = Priority.MEDIUM }
                        )
                        
                        PriorityOption(
                            text = "高",
                            icon = Icons.Default.KeyboardArrowUp,
                            selected = priority == Priority.HIGH,
                            color = PriorityHigh,
                            onClick = { priority = Priority.HIGH }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 标签选择部分
                    Text(
                        text = "标签（可选）",
                        color = MatchaTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 标签选择下拉菜单
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isTagMenuExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MatchaTextHint.copy(alpha = 0.5f)),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Label,
                                    contentDescription = null,
                                    tint = MatchaGreen,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Text(
                                    text = if (tag.isNotEmpty()) tag else "选择标签",
                                    fontSize = 16.sp,
                                    color = if (tag.isNotEmpty()) MatchaTextPrimary else MatchaTextHint
                                )
                                
                                Spacer(modifier = Modifier.weight(1f))
                                
                                if (tag.isNotEmpty()) {
                                    IconButton(
                                        onClick = { tag = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "清除标签",
                                            tint = MatchaTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "展开选项",
                                        tint = MatchaTextSecondary
                                    )
                                }
                            }
                        }
                        
                        DropdownMenu(
                            expanded = isTagMenuExpanded,
                            onDismissRequest = { isTagMenuExpanded = false },
                            modifier = Modifier
                                .width(200.dp)
                                .background(MatchaCardBg)
                        ) {
                            if (availableTags.isEmpty()) {
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            "没有可用标签",
                                            color = MatchaTextSecondary
                                        ) 
                                    },
                                    onClick = { isTagMenuExpanded = false },
                                    enabled = false
                                )
                            } else {
                                availableTags.forEach { tagOption ->
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                tagOption,
                                                color = MatchaTextPrimary,
                                                fontWeight = if (tag == tagOption) FontWeight.Bold else FontWeight.Normal
                                            ) 
                                        },
                                        onClick = {
                                            tag = tagOption
                                            isTagMenuExpanded = false
                                        },
                                        leadingIcon = {
                                            if (tag == tagOption) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "已选择",
                                                    tint = MatchaGreen
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            
                            Divider(color = MatchaDivider)
                            
                            // 提示去标签管理区添加新标签
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        "去标签筛选区域添加新标签",
                                        color = MatchaGreen,
                                        fontSize = 14.sp
                                    ) 
                                },
                                onClick = { 
                                    isTagMenuExpanded = false
                                    onDismiss()
                                    // 提示用户关闭对话框并去添加标签
                                    Toast.makeText(
                                        this@TodoActivity,
                                        "请先关闭对话框，然后在标签筛选区域添加新标签",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "添加新标签",
                                        tint = MatchaGreen
                                    )
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onSave(title, description, dueDate, priority, tag) },
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MatchaGreen,
                        disabledContainerColor = MatchaGreen.copy(alpha = 0.5f)
                    )
                ) {
                    Text(if (isEditing) "保存" else "添加")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消", color = MatchaTextSecondary)
                }
            }
        )
    }
    
    @Composable
    fun PriorityOption(
        text: String,
        icon: ImageVector,
        selected: Boolean,
        color: Color,
        onClick: () -> Unit
    ) {
        val backgroundColor = if (selected) color.copy(alpha = 0.15f) else Color.Transparent
        val borderColor = if (selected) color else MatchaTextHint.copy(alpha = 0.3f)
        
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .width(80.dp),
            border = BorderStroke(1.dp, borderColor),
            color = backgroundColor,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = text,
                    fontSize = 14.sp,
                    color = if (selected) color else MatchaTextSecondary
                )
            }
        }
    }
} 