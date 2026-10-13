package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.reminder.ReminderManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val uncompletedTodos = db.todoDao().getUncompletedTodosSync()
                val now = System.currentTimeMillis()

                var hasSmartTodos = false
                for (item in uncompletedTodos) {
                    val todo = item.todo
                    if (todo.startTime > now) {
                        // Re-register alarms
                        ReminderManager.scheduleAlarmsForTodo(context, todo)
                        if (todo.remindType == com.example.data.model.TodoItem.REMIND_TYPE_SMART) {
                            hasSmartTodos = true
                        }
                    }
                }

                // Restore WorkManager task
                ReminderManager.syncSmartReminderWork(context, hasSmartTodos)
            } catch (_: Exception) {
                // Ignore transient errors
            } finally {
                pendingResult.finish()
            }
        }
    }
}
