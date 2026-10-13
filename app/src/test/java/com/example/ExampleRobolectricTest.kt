package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.TodoItem
import com.example.reminder.SmartDepartureEngine
import com.example.reminder.SmartEvaluationResult
import com.example.reminder.TravelEstimate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("智能待办", appName)
    }

    @Test
    fun `smart reminder triggers when remaining time minus travel is under 2 hours`() {
        val now = 1000000000000L
        // Start time is 1 hour and 30 minutes from now (90 mins)
        // Travel duration is 40 mins
        // Remaining margin = 90 - 40 = 50 mins, which is < 120 mins (2 hours)
        val todo = TodoItem(
            title = "客户会议",
            startTime = now + (90 * 60 * 1000L),
            locationName = "科技园区",
            latitude = 31.2,
            longitude = 121.5,
            remindType = TodoItem.REMIND_TYPE_SMART,
            lastSmartNotifyAt = 0L
        )

        val estimate = TravelEstimate(
            drivingMinutes = 35,
            transitMinutes = 40,
            distanceKm = 15.0,
            isViaApi = false
        )

        val result = SmartDepartureEngine.evaluateSmartReminder(
            todo = todo,
            travelEstimate = estimate,
            now = now
        )

        assertTrue(result is SmartEvaluationResult.ShouldNotify)
    }
}
