package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.location.LocationHelper
import com.example.reminder.ReminderManager
import com.example.reminder.SmartDepartureEngine
import com.example.reminder.SmartEvaluationResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_FIXED_REMINDER = "com.example.action.FIXED_REMINDER"
        const val ACTION_PRE_NOTICE = "com.example.action.PRE_NOTICE"
        const val ACTION_SMART_CHECK = "com.example.action.SMART_CHECK"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val now = System.currentTimeMillis()

                when (action) {
                    ACTION_FIXED_REMINDER -> {
                        val todoId = intent.getLongExtra(ReminderManager.EXTRA_TODO_ID, -1L)
                        if (todoId != -1L) {
                            val item = db.todoDao().getTodoWithChecklistByIdSync(todoId)
                            if (item != null && !item.todo.isCompleted && item.todo.startTime > now) {
                                val todo = item.todo
                                val timeStr = ReminderManager.formatTime(todo.startTime)
                                val locationStr = if (todo.locationName.isNotBlank()) "（地点：${todo.locationName}）" else ""
                                val title = "待办日程提醒"
                                val content = "「${todo.title}」将于 $timeStr 开始$locationStr"

                                ReminderManager.showNotification(
                                    context = context,
                                    notificationId = (todo.id * 10 + 1).toInt(),
                                    title = title,
                                    content = content,
                                    todoId = todo.id
                                )
                            }
                        }
                    }

                    ACTION_PRE_NOTICE -> {
                        val todoId = intent.getLongExtra(ReminderManager.EXTRA_TODO_ID, -1L)
                        if (todoId != -1L) {
                            val item = db.todoDao().getTodoWithChecklistByIdSync(todoId)
                            if (item != null && !item.todo.isCompleted && item.todo.startTime > now &&
                                item.hasUncheckedItems && !item.todo.preNoticeSent
                            ) {
                                val todo = item.todo
                                val unchecked = item.uncheckedItemContents
                                val preview = unchecked.take(3).joinToString("、")
                                val suffix = if (unchecked.size > 3) "等" else ""
                                val title = "前置准备提醒"
                                val content = "「${todo.title}」不足 3 小时开始，还有 ${unchecked.size} 项准备未完成：$preview$suffix"

                                ReminderManager.showNotification(
                                    context = context,
                                    notificationId = (todo.id * 10 + 2).toInt(),
                                    title = title,
                                    content = content,
                                    todoId = todo.id
                                )

                                db.todoDao().updatePreNoticeSent(todo.id, true)
                            }
                        }
                    }

                    ACTION_SMART_CHECK -> {
                        // PRD 4.1 & 4.2: 智能提醒周期巡检（每15分钟检查路线与最晚出发时间）
                        val smartTodos = db.todoDao().getUncompletedSmartTodosSync()
                        if (smartTodos.isNotEmpty()) {
                            val userLocation = LocationHelper.getLastKnownLocation(context)
                            if (userLocation != null) {
                                val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                                val amapKey = prefs.getString("amap_api_key", null)

                                for (item in smartTodos) {
                                    val todo = item.todo
                                    if (!todo.isCompleted && todo.startTime > now && (todo.latitude != 0.0 || todo.longitude != 0.0)) {
                                        val estimate = SmartDepartureEngine.estimateTravelTime(
                                            context = context,
                                            todo = todo,
                                            userLat = userLocation.latitude,
                                            userLng = userLocation.longitude,
                                            amapKey = amapKey
                                        )

                                        val result = SmartDepartureEngine.evaluateSmartReminder(
                                            todo = todo,
                                            travelEstimate = estimate,
                                            now = now
                                        )

                                        if (result is SmartEvaluationResult.ShouldNotify) {
                                            val loc = if (todo.locationName.isNotBlank()) todo.locationName else "目的地"
                                            val title = "该规划出发了"
                                            val content = "「${todo.title}」：驾车约 ${estimate.drivingMinutes} 分钟 / 公交约 ${estimate.transitMinutes} 分钟可达 $loc，请合理安排出发时间"

                                            ReminderManager.showNotification(
                                                context = context,
                                                notificationId = (todo.id * 10 + 3).toInt(),
                                                title = title,
                                                content = content,
                                                todoId = todo.id
                                            )

                                            db.todoDao().updateSmartNotifyRecord(
                                                id = todo.id,
                                                notifyAt = now,
                                                travelMin = estimate.maxTravelMinutes
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // 省电优化：无智能待办时取消周期任务
                            ReminderManager.syncSmartReminderWork(context, false)
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
