package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TodoApplication
import com.example.data.model.TodoItem
import com.example.data.model.TodoWithChecklist
import com.example.location.LocationHelper
import com.example.reminder.ReminderManager
import com.example.reminder.SmartDepartureEngine
import com.example.reminder.TravelEstimate
import com.example.ui.model.TabType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TodoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as TodoApplication).repository

    private val _selectedTab = MutableStateFlow(TabType.IMPORTANT)
    val selectedTab: StateFlow<TabType> = _selectedTab.asStateFlow()

    private val _selectedDetailId = MutableStateFlow<Long?>(null)
    val selectedDetailId: StateFlow<Long?> = _selectedDetailId.asStateFlow()

    private val _isEditorOpen = MutableStateFlow(false)
    val isEditorOpen: StateFlow<Boolean> = _isEditorOpen.asStateFlow()

    private val _editingTodo = MutableStateFlow<TodoWithChecklist?>(null)
    val editingTodo: StateFlow<TodoWithChecklist?> = _editingTodo.asStateFlow()

    private val _defaultFlagForCreate = MutableStateFlow(false)
    val defaultFlagForCreate: StateFlow<Boolean> = _defaultFlagForCreate.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    // All todos from Room Flow
    val allTodos: StateFlow<List<TodoWithChecklist>> = repository.allTodos
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current detail item observed reactively
    val detailTodo: StateFlow<TodoWithChecklist?> = combine(allTodos, _selectedDetailId) { list, id ->
        if (id == null) null else list.find { it.todo.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Tab 1: 重要 (isFlagged == true && !isCompleted)
    val importantTodos: StateFlow<List<TodoWithChecklist>> = allTodos.combine(_selectedTab) { list, _ ->
        list.filter { it.todo.isFlagged && !it.todo.isCompleted }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Tab 2: 进行中 (isFlagged == false && !it.isCompleted)
    val inProgressTodos: StateFlow<List<TodoWithChecklist>> = allTodos.combine(_selectedTab) { list, _ ->
        list.filter { !it.todo.isFlagged && !it.todo.isCompleted }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Tab 3: 已完成 (isCompleted == true)
    val completedTodos: StateFlow<List<TodoWithChecklist>> = allTodos.combine(_selectedTab) { list, _ ->
        list.filter { it.todo.isCompleted }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Seed default sample todos on first launch if database is empty
        viewModelScope.launch {
            val existing = repository.allTodos.first()
            if (existing.isEmpty()) {
                seedInitialData()
            }
        }
    }

    private suspend fun seedInitialData() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        // 1. 重要事项：今日客户项目路演（智能提醒）
        cal.timeInMillis = now
        cal.add(Calendar.HOUR_OF_DAY, 2)
        cal.set(Calendar.MINUTE, 30)
        val roadshowStart = cal.timeInMillis

        repository.createOrUpdateTodo(
            todo = TodoItem(
                title = "重要客户项目最终方案路演",
                content = "带上演示电脑、激光笔与纸质合同副本三份，现场提前15分钟调试投屏设备。",
                startTime = roadshowStart,
                locationName = "市中心国际会议中心 A座302",
                latitude = 31.2304,
                longitude = 121.4737,
                isFlagged = true,
                remindType = TodoItem.REMIND_TYPE_SMART,
                isCompleted = false
            ),
            checklistTexts = listOf(
                "准备项目演示 Keynote 与备用 U 盘",
                "打印并盖章商务合同初稿（三份）",
                "携带 HDMI 转接头及演示激光笔",
                "确认客户参会人员名单与席卡"
            )
        )

        // 2. 重要事项：明天下午年度体检
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        val healthCheckStart = cal.timeInMillis

        repository.createOrUpdateTodo(
            todo = TodoItem(
                title = "年度健康体检预约",
                content = "前一天晚上8点后禁食水，带好身份证和医保卡。",
                startTime = healthCheckStart,
                locationName = "瑞金医院 体检中心二楼",
                latitude = 31.2132,
                longitude = 121.4682,
                isFlagged = true,
                remindType = TodoItem.REMIND_TYPE_FIXED,
                fixedRemindMinutes = 60,
                isCompleted = false
            ),
            checklistTexts = listOf(
                "携带本人身份证与医保卡",
                "体检前晚八点后禁食禁饮",
                "穿着宽松无金属配饰衣物"
            )
        )

        // 3. 进行中事项：下午团队周会（固定提醒）
        cal.timeInMillis = now
        cal.add(Calendar.HOUR_OF_DAY, 4)
        cal.set(Calendar.MINUTE, 0)
        val weeklyMeetingStart = cal.timeInMillis

        repository.createOrUpdateTodo(
            todo = TodoItem(
                title = "产品与研发双周对齐例会",
                content = "同步本季度核心里程碑推进度及下周排期。",
                startTime = weeklyMeetingStart,
                locationName = "公司 6F 创新空间会议室",
                latitude = 31.2222,
                longitude = 121.4580,
                isFlagged = false,
                remindType = TodoItem.REMIND_TYPE_FIXED,
                fixedRemindMinutes = 15,
                isCompleted = false
            ),
            checklistTexts = listOf(
                "更新 JIRA 敏捷看板任务状态",
                "整理本周上线的用户反馈要点"
            )
        )

        // 4. 已完成事项：提交上月差旅报销
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, -1)
        cal.set(Calendar.HOUR_OF_DAY, 15)
        val expenseDoneStart = cal.timeInMillis

        val expenseId = repository.createOrUpdateTodo(
            todo = TodoItem(
                title = "提交北京出差差旅发票报销",
                content = "高铁车票及酒店住宿发票已粘好送至财务。",
                startTime = expenseDoneStart,
                locationName = "财务部 305室",
                latitude = 0.0,
                longitude = 0.0,
                isFlagged = false,
                remindType = TodoItem.REMIND_TYPE_NONE,
                isCompleted = true
            ),
            checklistTexts = listOf(
                "核对高铁票增值税电子普通发票",
                "主管签字审批流程归档"
            )
        )
        repository.setChecklistItemChecked(1, true)
    }

    fun selectTab(tab: TabType) {
        _selectedTab.value = tab
    }

    fun openDetail(todoId: Long) {
        _selectedDetailId.value = todoId
    }

    fun closeDetail() {
        _selectedDetailId.value = null
    }

    fun openCreate(isFlagged: Boolean = _selectedTab.value == TabType.IMPORTANT) {
        _editingTodo.value = null
        _defaultFlagForCreate.value = isFlagged
        _isEditorOpen.value = true
    }

    fun openEdit(item: TodoWithChecklist) {
        _editingTodo.value = item
        _isEditorOpen.value = true
    }

    fun closeEditor() {
        _isEditorOpen.value = false
        _editingTodo.value = null
    }

    fun openSettings() {
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    fun saveTodo(todo: TodoItem, checklist: List<String>) {
        viewModelScope.launch {
            val savedId = repository.createOrUpdateTodo(todo, checklist)
            closeEditor()
            if (_selectedDetailId.value != null) {
                _selectedDetailId.value = savedId
            }
        }
    }

    fun toggleChecklistItem(itemId: Long, isChecked: Boolean) {
        viewModelScope.launch {
            repository.setChecklistItemChecked(itemId, isChecked)
        }
    }

    fun toggleCompleted(todoId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleCompleted(todoId, isCompleted)
        }
    }

    fun deleteTodo(todoId: Long) {
        viewModelScope.launch {
            repository.deleteTodo(todoId)
            if (_selectedDetailId.value == todoId) {
                closeDetail()
            }
        }
    }

    suspend fun getEstimatedTravel(todo: TodoItem): TravelEstimate {
        val userLocation = LocationHelper.getLastKnownLocation(getApplication())
        val userLat = userLocation?.latitude ?: 31.2304
        val userLng = userLocation?.longitude ?: 121.4737

        val prefs = getApplication<Application>().getSharedPreferences("app_settings", Application.MODE_PRIVATE)
        val amapKey = prefs.getString("amap_api_key", null)

        return SmartDepartureEngine.estimateTravelTime(
            context = getApplication(),
            todo = todo,
            userLat = userLat,
            userLng = userLng,
            amapKey = amapKey
        )
    }

    fun sendTestNotification(todo: TodoItem) {
        val timeStr = ReminderManager.formatTime(todo.startTime)
        ReminderManager.showNotification(
            context = getApplication(),
            notificationId = (System.currentTimeMillis() % 10000).toInt(),
            title = "【测试提醒】${todo.title}",
            content = "将于 $timeStr 开始（地点：${if (todo.locationName.isNotBlank()) todo.locationName else "未指定"}）",
            todoId = todo.id
        )
    }
}
