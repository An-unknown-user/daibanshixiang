package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TodoWithChecklist
import com.example.ui.components.DateUtils
import com.example.ui.components.TodoCard
import com.example.ui.model.TabType
import com.example.ui.theme.FlagCoral
import com.example.ui.theme.SecondaryTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    currentTab: TabType,
    importantList: List<TodoWithChecklist>,
    inProgressList: List<TodoWithChecklist>,
    completedList: List<TodoWithChecklist>,
    onSelectTab: (TabType) -> Unit,
    onCardClick: (Long) -> Unit,
    onToggleComplete: (Long, Boolean) -> Unit,
    onFabClick: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val activeList = when (currentTab) {
        TabType.IMPORTANT -> importantList
        TabType.IN_PROGRESS -> inProgressList
        TabType.COMPLETED -> completedList
    }

    // Sorted by startTime ascending (closest first)
    val sortedList = remember(activeList) {
        activeList.sortedBy { it.todo.startTime }
    }

    // Grouped by date header
    val groupedItems = remember(sortedList) {
        sortedList.groupBy { item ->
            DateUtils.formatGroupHeader(item.todo.startTime, isExpired = false)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "智能待办",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "时间 + 地理位置智能出发提醒",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "设置")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                // Tab 1: 重要 (Default tab)
                NavigationBarItem(
                    selected = currentTab == TabType.IMPORTANT,
                    onClick = { onSelectTab(TabType.IMPORTANT) },
                    icon = {
                        BadgedBox(badge = {
                            if (importantList.isNotEmpty()) {
                                Badge(
                                    containerColor = FlagCoral,
                                    contentColor = Color.White
                                ) {
                                    Text(importantList.size.toString())
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (currentTab == TabType.IMPORTANT) Icons.Filled.Flag else Icons.Outlined.Flag,
                                contentDescription = "重要",
                                tint = if (currentTab == TabType.IMPORTANT) FlagCoral else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    label = { Text("重要", fontWeight = if (currentTab == TabType.IMPORTANT) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_important")
                )

                // Tab 2: 进行中
                NavigationBarItem(
                    selected = currentTab == TabType.IN_PROGRESS,
                    onClick = { onSelectTab(TabType.IN_PROGRESS) },
                    icon = {
                        BadgedBox(badge = {
                            if (inProgressList.isNotEmpty()) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ) {
                                    Text(inProgressList.size.toString())
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (currentTab == TabType.IN_PROGRESS) Icons.Filled.Schedule else Icons.Outlined.Schedule,
                                contentDescription = "进行中"
                            )
                        }
                    },
                    label = { Text("进行中", fontWeight = if (currentTab == TabType.IN_PROGRESS) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_in_progress")
                )

                // Tab 3: 已完成
                NavigationBarItem(
                    selected = currentTab == TabType.COMPLETED,
                    onClick = { onSelectTab(TabType.COMPLETED) },
                    icon = {
                        BadgedBox(badge = {
                            if (completedList.isNotEmpty()) {
                                Badge(
                                    containerColor = SecondaryTeal,
                                    contentColor = Color.White
                                ) {
                                    Text(completedList.size.toString())
                                }
                            }
                        }) {
                            Icon(
                                imageVector = if (currentTab == TabType.COMPLETED) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                contentDescription = "已完成"
                            )
                        }
                    },
                    label = { Text("已完成", fontWeight = if (currentTab == TabType.COMPLETED) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_completed")
                )
            }
        },
        floatingActionButton = {
            // FAB '+' only shown in "重要" and "进行中" tabs per PRD Section 2
            AnimatedVisibility(
                visible = currentTab != TabType.COMPLETED,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = onFabClick,
                    containerColor = if (currentTab == TabType.IMPORTANT) FlagCoral else MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.testTag("add_todo_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "新建待办")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (sortedList.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (currentTab) {
                                    TabType.IMPORTANT -> Icons.Default.Flag
                                    TabType.IN_PROGRESS -> Icons.Default.FormatListBulleted
                                    TabType.COMPLETED -> Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                tint = when (currentTab) {
                                    TabType.IMPORTANT -> FlagCoral
                                    TabType.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                                    TabType.COMPLETED -> SecondaryTeal
                                },
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when (currentTab) {
                            TabType.IMPORTANT -> "暂无重要事项"
                            TabType.IN_PROGRESS -> "暂无进行中的待办"
                            TabType.COMPLETED -> "暂无已完成的事项"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = when (currentTab) {
                            TabType.IMPORTANT -> "点击右下角加号，添加置顶旗标的重要日程与出行"
                            TabType.IN_PROGRESS -> "点击右下角加号创建待办，智能提醒您的最佳出发时刻"
                            TabType.COMPLETED -> "完成的事项将归档在此处，详情页可恢复为进行中"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("todo_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedItems.forEach { (dateHeader, itemsInGroup) ->
                        item(key = "header_$dateHeader") {
                            Surface(
                                color = Color.Transparent,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = dateHeader,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "${itemsInGroup.size} 项",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = itemsInGroup,
                            key = { it.todo.id }
                        ) { todoItem ->
                            TodoCard(
                                item = todoItem,
                                onCardClick = { onCardClick(todoItem.todo.id) },
                                onToggleComplete = {
                                    onToggleComplete(todoItem.todo.id, !todoItem.todo.isCompleted)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
