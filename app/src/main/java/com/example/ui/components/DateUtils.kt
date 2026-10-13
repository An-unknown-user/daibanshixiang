package com.example.ui.components

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {

    fun formatGroupHeader(timeMillis: Long, isExpired: Boolean): String {
        if (isExpired) {
            return "已过期事项"
        }

        val targetCal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val nowCal = Calendar.getInstance()

        val targetYear = targetCal.get(Calendar.YEAR)
        val targetDayOfYear = targetCal.get(Calendar.DAY_OF_YEAR)

        val nowYear = nowCal.get(Calendar.YEAR)
        val nowDayOfYear = nowCal.get(Calendar.DAY_OF_YEAR)

        if (targetYear == nowYear) {
            when (targetDayOfYear - nowDayOfYear) {
                0 -> return "今天"
                1 -> return "明天"
                2 -> return "后天"
                -1 -> return "昨天"
            }
        }

        val format = SimpleDateFormat("M月d日 EEE", Locale.CHINESE)
        return format.format(Date(timeMillis))
    }

    fun formatTimeOnly(timeMillis: Long): String {
        val format = SimpleDateFormat("HH:mm", Locale.CHINESE)
        return format.format(Date(timeMillis))
    }

    fun formatFullDateTime(timeMillis: Long): String {
        val format = SimpleDateFormat("yyyy年M月d日 HH:mm EEE", Locale.CHINESE)
        return format.format(Date(timeMillis))
    }

    fun getRelativeTimeDescription(timeMillis: Long, now: Long = System.currentTimeMillis()): String {
        val diff = timeMillis - now
        if (diff < 0) {
            val pastMin = (-diff / 60000).toInt()
            return when {
                pastMin < 60 -> "已过去 ${pastMin} 分钟"
                pastMin < 1440 -> "已过去 ${pastMin / 60} 小时"
                else -> "已过去 ${pastMin / 1440} 天"
            }
        } else {
            val futureMin = (diff / 60000).toInt()
            return when {
                futureMin < 60 -> "${futureMin} 分钟后"
                futureMin < 1440 -> "${futureMin / 60} 小时 ${(futureMin % 60)} 分钟后"
                else -> "${futureMin / 1440} 天后"
            }
        }
    }
}
