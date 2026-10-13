package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String = "",
    val startTime: Long, // Epoch milliseconds
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isFlagged: Boolean = false,
    val remindType: String = "NONE", // "NONE", "FIXED", "SMART"
    val fixedRemindMinutes: Int = 15,
    val isCompleted: Boolean = false,
    val preNoticeSent: Boolean = false,
    val lastSmartNotifyAt: Long = 0L,
    val lastSmartTravelMin: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isExpired: Boolean
        get() = !isCompleted && (startTime < System.currentTimeMillis())

    val hasLocation: Boolean
        get() = locationName.isNotBlank() || (latitude != 0.0 && longitude != 0.0)

    companion object {
        const val REMIND_TYPE_NONE = "NONE"
        const val REMIND_TYPE_FIXED = "FIXED"
        const val REMIND_TYPE_SMART = "SMART"
    }
}
