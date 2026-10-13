package com.example.data.repository

import android.content.Context
import com.example.data.dao.TodoDao
import com.example.data.model.ChecklistItem
import com.example.data.model.TodoItem
import com.example.data.model.TodoWithChecklist
import com.example.reminder.ReminderManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class TodoRepository(
    private val context: Context,
    private val dao: TodoDao
) {

    val allTodos: Flow<List<TodoWithChecklist>> = dao.getAllTodosWithChecklist()

    fun getTodoById(id: Long): Flow<TodoWithChecklist?> {
        return dao.getTodoWithChecklistById(id)
    }

    suspend fun createOrUpdateTodo(
        todo: TodoItem,
        checklistTexts: List<String>
    ): Long {
        val isNew = todo.id == 0L
        val savedId: Long

        if (isNew) {
            savedId = dao.insertTodo(todo)
            if (checklistTexts.isNotEmpty()) {
                val items = checklistTexts.mapIndexed { index, text ->
                    ChecklistItem(
                        todoId = savedId,
                        content = text,
                        isChecked = false,
                        sortOrder = index
                    )
                }
                dao.insertChecklistItems(items)
            }
        } else {
            savedId = todo.id
            // PRD 4.3: 编辑开始时间/提醒方式/地点后：重设闹钟、清空前置提醒标记与智能冷却记录
            val updatedTodo = todo.copy(
                preNoticeSent = false,
                lastSmartNotifyAt = 0L,
                lastSmartTravelMin = 0
            )
            dao.updateTodo(updatedTodo)

            // 更新清单：保留原先的勾选状态（如果有相同项内容），或重新插入
            val oldTodo = dao.getTodoWithChecklistByIdSync(savedId)
            val oldChecklistMap = oldTodo?.checklist?.associate { it.content to it.isChecked } ?: emptyMap()

            dao.deleteChecklistItemsByTodoId(savedId)
            if (checklistTexts.isNotEmpty()) {
                val items = checklistTexts.mapIndexed { index, text ->
                    ChecklistItem(
                        todoId = savedId,
                        content = text,
                        isChecked = oldChecklistMap[text] ?: false,
                        sortOrder = index
                    )
                }
                dao.insertChecklistItems(items)
            }
        }

        // 重新查询最新完整数据并更新调度
        val finalTodo = dao.getTodoWithChecklistByIdSync(savedId)?.todo ?: todo.copy(id = savedId)
        ReminderManager.scheduleAlarmsForTodo(context, finalTodo)
        checkAndSyncWorkManager()

        return savedId
    }

    suspend fun setChecklistItemChecked(itemId: Long, isChecked: Boolean) {
        dao.setChecklistItemStatus(itemId, isChecked)
    }

    suspend fun toggleCompleted(todoId: Long, isCompleted: Boolean) {
        dao.updateTodoCompleted(todoId, isCompleted)
        if (isCompleted) {
            // PRD 4.3: 标记完成/删除后：撤销该事项全部闹钟，轮询自动跳过
            ReminderManager.cancelAlarmsForTodo(context, todoId)
        } else {
            // 恢复为进行中：重新调度闹钟
            val todoWithChecklist = dao.getTodoWithChecklistByIdSync(todoId)
            if (todoWithChecklist != null && !todoWithChecklist.todo.isExpired) {
                ReminderManager.scheduleAlarmsForTodo(context, todoWithChecklist.todo)
            }
        }
        checkAndSyncWorkManager()
    }

    suspend fun deleteTodo(todoId: Long) {
        ReminderManager.cancelAlarmsForTodo(context, todoId)
        dao.deleteTodoById(todoId)
        checkAndSyncWorkManager()
    }

    private suspend fun checkAndSyncWorkManager() {
        val smartList = dao.getUncompletedSmartTodosSync()
        val hasActiveSmart = smartList.any { !it.todo.isExpired && it.todo.hasLocation }
        ReminderManager.syncSmartReminderWork(context, hasActiveSmart)
    }
}
