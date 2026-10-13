package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TodoItem
import com.example.data.model.TodoWithChecklist
import com.example.ui.components.DateUtils
import com.example.ui.components.LocationPickerContent
import com.example.ui.theme.FlagCoral
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TodoEditorSheet(
    initialItem: TodoWithChecklist?,
    defaultFlagged: Boolean,
    onSave: (TodoItem, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Form states
    var title by remember { mutableStateOf(initialItem?.todo?.title ?: "") }
    var content by remember { mutableStateOf(initialItem?.todo?.content ?: "") }

    val defaultCalendar = remember {
        Calendar.getInstance().apply {
            if (initialItem != null) {
                timeInMillis = initialItem.todo.startTime
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
            }
        }
    }

    var selectedStartTime by remember { mutableLongStateOf(defaultCalendar.timeInMillis) }
    var locationName by remember { mutableStateOf(initialItem?.todo?.locationName ?: "") }
    var latitude by remember { mutableStateOf(initialItem?.todo?.latitude ?: 0.0) }
    var longitude by remember { mutableStateOf(initialItem?.todo?.longitude ?: 0.0) }
    var isFlagged by remember { mutableStateOf(initialItem?.todo?.isFlagged ?: defaultFlagged) }
    var remindType by remember {
        mutableStateOf(initialItem?.todo?.remindType ?: TodoItem.REMIND_TYPE_NONE)
    }
    var fixedRemindMinutes by remember {
        mutableIntStateOf(initialItem?.todo?.fixedRemindMinutes ?: 15)
    }

    val checklistItems = remember {
        mutableStateListOf<String>().apply {
            if (initialItem != null && initialItem.checklist.isNotEmpty()) {
                addAll(initialItem.checklist.map { it.content })
            }
        }
    }
    var newChecklistText by remember { mutableStateOf("") }

    // Dialog & BottomSheet pickers
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedStartTime
    )
    val timePickerState = rememberTimePickerState(
        initialHour = defaultCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = defaultCalendar.get(Calendar.MINUTE),
        is24Hour = true
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialItem == null) "新建待办" else "编辑待办",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("editor_close_button")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "取消")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "请输入待办标题", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (remindType == TodoItem.REMIND_TYPE_SMART && locationName.isBlank() && latitude == 0.0) {
                                Toast.makeText(context, "选择智能提醒时，必须设置地点", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val todoToSave = (initialItem?.todo ?: TodoItem(
                                title = title,
                                startTime = selectedStartTime
                            )).copy(
                                title = title.trim(),
                                content = content.trim(),
                                startTime = selectedStartTime,
                                locationName = locationName.trim(),
                                latitude = latitude,
                                longitude = longitude,
                                isFlagged = isFlagged,
                                remindType = remindType,
                                fixedRemindMinutes = fixedRemindMinutes
                            )

                            onSave(todoToSave, checklistItems.filter { it.isNotBlank() })
                        },
                        modifier = Modifier.testTag("editor_save_button")
                    ) {
                        Text("保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题 *") },
                placeholder = { Text("准备去做什么？") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("todo_title_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // 2. Content Input
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("详细备注（选填）") },
                placeholder = { Text("补充更多备忘细节...") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("todo_content_input"),
                shape = RoundedCornerShape(12.dp)
            )

            // 3. Start Time Pickers
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "开始时间 *",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Date selector
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = DateUtils.formatGroupHeader(selectedStartTime, false),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Time selector
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = DateUtils.formatTimeOnly(selectedStartTime),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "完整时间：${DateUtils.formatFullDateTime(selectedStartTime)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. Flag Switch
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = if (isFlagged) FlagCoral else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "标记为重要",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = if (isFlagged) "收录在「重要」Tab 优先展示" else "收录在「进行中」Tab",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isFlagged,
                        onCheckedChange = { isFlagged = it },
                        modifier = Modifier.testTag("flag_switch")
                    )
                }
            }

            // 5. Reminder Type Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "提醒方式",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(TodoItem.REMIND_TYPE_NONE, "无提醒", Icons.Default.NotificationsOff),
                            Triple(TodoItem.REMIND_TYPE_FIXED, "固定时间", Icons.Default.Notifications),
                            Triple(TodoItem.REMIND_TYPE_SMART, "智能提醒", Icons.Default.DirectionsCar)
                        ).forEach { (type, label, icon) ->
                            val isSelected = remindType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { remindType = type },
                                label = { Text(label) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Smart reminder notice
                    AnimatedVisibility(visible = remindType == TodoItem.REMIND_TYPE_SMART) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "系统将根据您的实时位置与交通状况，自动计算并在合适时间提醒您出发。",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    // Fixed reminder advance picker
                    AnimatedVisibility(visible = remindType == TodoItem.REMIND_TYPE_FIXED) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "提前提醒时间：",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    5 to "提前5分钟",
                                    15 to "提前15分钟",
                                    30 to "提前30分钟",
                                    60 to "提前1小时",
                                    120 to "提前2小时"
                                ).forEach { (mins, label) ->
                                    val isSelected = fixedRemindMinutes == mins
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { fixedRemindMinutes = mins },
                                        label = { Text(label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Location Picker
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "待办地点",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (remindType == TodoItem.REMIND_TYPE_SMART) {
                                Text(
                                    text = " (智能提醒必填 *)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        TextButton(onClick = { showLocationPicker = true }) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (locationName.isBlank()) "选择地点" else "更换地点")
                        }
                    }

                    if (locationName.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = locationName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    )
                                    if (latitude != 0.0 && longitude != 0.0) {
                                        Text(
                                            text = "经纬度: ${"%.4f".format(latitude)}, ${"%.4f".format(longitude)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    locationName = ""
                                    latitude = 0.0
                                    longitude = 0.0
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "清除",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "未设置地点",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 7. Checklist (前置清单)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "前置清单（出发前准备）",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "将在开始前 3 小时自动推送未完成的清单项提醒",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Existing checklist items
                    checklistItems.forEachIndexed { index, itemText ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = itemText,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { checklistItems.removeAt(index) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Add new item input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newChecklistText,
                            onValueChange = { newChecklistText = it },
                            placeholder = { Text("添加准备项（如：带钥匙、带U盘）") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("checklist_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (newChecklistText.isNotBlank()) {
                                    checklistItems.add(newChecklistText.trim())
                                    newChecklistText = ""
                                }
                            },
                            modifier = Modifier.testTag("checklist_add_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "添加")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        val newCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
                        val currCal = Calendar.getInstance().apply { timeInMillis = selectedStartTime }
                        newCal.set(Calendar.HOUR_OF_DAY, currCal.get(Calendar.HOUR_OF_DAY))
                        newCal.set(Calendar.MINUTE, currCal.get(Calendar.MINUTE))
                        selectedStartTime = newCal.timeInMillis
                    }
                    showDatePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = selectedStartTime
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                    }
                    selectedStartTime = cal.timeInMillis
                    showTimePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("选择具体时间", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                TimePicker(state = timePickerState)
            }
        }
    }

    // Location Picker Bottom Sheet
    if (showLocationPicker) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showLocationPicker = false },
            sheetState = sheetState
        ) {
            LocationPickerContent(
                initialQuery = locationName,
                onSelectLocation = { name, lat, lng ->
                    locationName = name
                    latitude = lat
                    longitude = lng
                    showLocationPicker = false
                },
                onDismiss = { showLocationPicker = false }
            )
        }
    }
}
