package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.TodoItem
import com.example.receiver.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReminderManager {

    const val CHANNEL_ID = "smart_todo_reminders"
    const val CHANNEL_NAME = "待办与出发提醒"

    const val EXTRA_TODO_ID = "extra_todo_id"
    private const val SMART_CHECK_REQ_CODE = 99999

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "固定时间提醒、前置清单检查及智能最晚出发规划提醒"
                enableVibration(true)
            }
            val notificationManager =
                context.getSystemService(Context.LOCATION_SERVICE)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules alarms for a todo item (Fixed Reminder and 3-hour Checklist Pre-notice)
     */
    fun scheduleAlarmsForTodo(context: Context, todo: TodoItem) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return

        // 1. Fixed Reminder Alarm
        val fixedReqCode = (todo.id * 10 + 1).toInt()
        val fixedIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIXED_REMINDER
            putExtra(EXTRA_TODO_ID, todo.id)
        }
        val fixedPendingIntent = PendingIntent.getBroadcast(
            context,
            fixedReqCode,
            fixedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fixedAlarmTime = todo.startTime - (todo.fixedRemindMinutes * 60 * 1000L)
        val now = System.currentTimeMillis()

        if (todo.remindType == TodoItem.REMIND_TYPE_FIXED && !todo.isCompleted && fixedAlarmTime > now) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            fixedAlarmTime,
                            fixedPendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            fixedAlarmTime,
                            fixedPendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        fixedAlarmTime,
                        fixedPendingIntent
                    )
                }
            } catch (_: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, fixedAlarmTime, fixedPendingIntent)
            }
        } else {
            alarmManager.cancel(fixedPendingIntent)
        }

        // 2. Checklist Pre-notice Alarm (3 hours before startTime)
        val preNoticeReqCode = (todo.id * 10 + 2).toInt()
        val preNoticeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_PRE_NOTICE
            putExtra(EXTRA_TODO_ID, todo.id)
        }
        val preNoticePendingIntent = PendingIntent.getBroadcast(
            context,
            preNoticeReqCode,
            preNoticeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val preNoticeAlarmTime = todo.startTime - (3 * 60 * 60 * 1000L)
        if (!todo.isCompleted && !todo.preNoticeSent && preNoticeAlarmTime > now) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            preNoticeAlarmTime,
                            preNoticePendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            preNoticeAlarmTime,
                            preNoticePendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        preNoticeAlarmTime,
                        preNoticePendingIntent
                    )
                }
            } catch (_: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, preNoticeAlarmTime, preNoticePendingIntent)
            }
        } else {
            alarmManager.cancel(preNoticePendingIntent)
        }
    }

    /**
     * Cancels all alarms for a specific todo item
     */
    fun cancelAlarmsForTodo(context: Context, todoId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return

        val fixedReqCode = (todoId * 10 + 1).toInt()
        val fixedIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_FIXED_REMINDER
        }
        val fixedPendingIntent = PendingIntent.getBroadcast(
            context,
            fixedReqCode,
            fixedIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (fixedPendingIntent != null) {
            alarmManager.cancel(fixedPendingIntent)
            fixedPendingIntent.cancel()
        }

        val preNoticeReqCode = (todoId * 10 + 2).toInt()
        val preNoticeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_PRE_NOTICE
        }
        val preNoticePendingIntent = PendingIntent.getBroadcast(
            context,
            preNoticeReqCode,
            preNoticeIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (preNoticePendingIntent != null) {
            alarmManager.cancel(preNoticePendingIntent)
            preNoticePendingIntent.cancel()
        }
    }

    /**
     * Periodically checks smart reminders every 15 minutes via AlarmManager repeating alarm.
     * Cancels when no active smart todos exist (power optimization).
     */
    fun syncSmartReminderWork(context: Context, hasActiveSmartTodos: Boolean) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            ?: return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_SMART_CHECK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            SMART_CHECK_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (hasActiveSmartTodos) {
            val interval = 15 * 60 * 1000L // 15 minutes
            val triggerAt = System.currentTimeMillis() + interval
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                interval,
                pendingIntent
            )
        } else {
            alarmManager.cancel(pendingIntent)
        }
    }

    /**
     * Shows a system notification with deep-link navigation to the todo's detail screen.
     */
    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        content: String,
        todoId: Long
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TODO_ID, todoId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    fun formatTime(millis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(millis))
    }
}
