package com.example.reminder

import android.content.Context
import com.example.data.model.TodoItem
import com.example.location.AmapClient
import com.example.location.LocationHelper
import kotlin.math.max
import kotlin.math.roundToInt

data class TravelEstimate(
    val drivingMinutes: Int,
    val transitMinutes: Int,
    val distanceKm: Double,
    val isViaApi: Boolean
) {
    val maxTravelMinutes: Int
        get() = max(drivingMinutes, transitMinutes)
}

sealed class SmartEvaluationResult {
    data class ShouldNotify(
        val travelEstimate: TravelEstimate,
        val reason: String // "FIRST_TIME", "COOLDOWN_EXPIRED", "TRAFFIC_WORSENED"
    ) : SmartEvaluationResult()

    data class Skip(val reason: String) : SmartEvaluationResult()
}

object SmartDepartureEngine {

    /**
     * Calculates travel estimate for a todo given user coordinates and optional Amap API Key.
     */
    suspend fun estimateTravelTime(
        context: Context,
        todo: TodoItem,
        userLat: Double,
        userLng: Double,
        amapKey: String? = null
    ): TravelEstimate {
        val distanceKm = LocationHelper.calculateDistanceKm(
            userLat, userLng,
            todo.latitude, todo.longitude
        )

        // Try Amap API if key is provided
        if (!amapKey.isNullOrBlank() && todo.latitude != 0.0 && todo.longitude != 0.0) {
            try {
                val origin = "$userLng,$userLat"
                val destination = "${todo.longitude},${todo.latitude}"
                val response = AmapClient.api.getDrivingRoute(origin, destination, amapKey)
                val durationSec = response.route?.paths?.firstOrNull()?.duration?.toLongOrNull()
                if (durationSec != null && durationSec > 0) {
                    val drivingMin = (durationSec / 60.0).roundToInt().coerceAtLeast(5)
                    val transitMin = (drivingMin * 1.35).roundToInt().coerceAtLeast(drivingMin)
                    return TravelEstimate(
                        drivingMinutes = drivingMin,
                        transitMinutes = transitMin,
                        distanceKm = distanceKm,
                        isViaApi = true
                    )
                }
            } catch (_: Exception) {
                // Fall back to robust local traffic model
            }
        }

        // Robust urban traffic model:
        // Driving: avg 30 km/h + 8 min parking/traffic lights buffer
        // Transit: avg 22 km/h + 15 min station walking/waiting buffer
        val baseDrivingMin = ((distanceKm / 30.0) * 60).roundToInt() + 8
        val baseTransitMin = ((distanceKm / 22.0) * 60).roundToInt() + 15

        val drivingMin = baseDrivingMin.coerceIn(10, 300)
        val transitMin = baseTransitMin.coerceIn(15, 360)

        return TravelEstimate(
            drivingMinutes = drivingMin,
            transitMinutes = transitMin,
            distanceKm = distanceKm,
            isViaApi = false
        )
    }

    /**
     * Evaluates whether a smart reminder should be pushed right now based on PRD Section 4.2.
     */
    fun evaluateSmartReminder(
        todo: TodoItem,
        travelEstimate: TravelEstimate,
        now: Long = System.currentTimeMillis()
    ): SmartEvaluationResult {
        // 1. Condition check: uncompleted + not expired + has location
        if (todo.isCompleted) {
            return SmartEvaluationResult.Skip("Item is already completed")
        }
        if (todo.startTime <= now) {
            return SmartEvaluationResult.Skip("Item is expired")
        }

        val maxTravelMin = travelEstimate.maxTravelMinutes
        val maxTravelMillis = maxTravelMin * 60 * 1000L

        // Trigger Formula: startTime - now - max(drivingDuration, transitDuration) < 2 hours
        // 即：余量时间不足 2 小时时触发提醒规划出发
        val twoHoursMillis = 2 * 60 * 60 * 1000L
        val marginMillis = (todo.startTime - now) - maxTravelMillis

        if (marginMillis >= twoHoursMillis) {
            return SmartEvaluationResult.Skip(
                "Margin is ${marginMillis / 60000}m, which is >= 2 hours. Departure planning not yet needed."
            )
        }

        // 2. Cooldown state machine:
        // - 若从未推送过 -> 推送
        if (todo.lastSmartNotifyAt == 0L) {
            return SmartEvaluationResult.ShouldNotify(
                travelEstimate = travelEstimate,
                reason = "FIRST_TIME"
            )
        }

        // - 否则若距上次推送 >= 30 分钟 -> 推送
        val thirtyMinMillis = 30 * 60 * 1000L
        if ((now - todo.lastSmartNotifyAt) >= thirtyMinMillis) {
            return SmartEvaluationResult.ShouldNotify(
                travelEstimate = travelEstimate,
                reason = "COOLDOWN_EXPIRED"
            )
        }

        // - 否则若当前最长耗时 - 上次记录耗时 > 15 分钟（交通恶化）-> 推送
        if ((maxTravelMin - todo.lastSmartTravelMin) > 15) {
            return SmartEvaluationResult.ShouldNotify(
                travelEstimate = travelEstimate,
                reason = "TRAFFIC_WORSENED"
            )
        }

        // 否则跳过本轮
        return SmartEvaluationResult.Skip("In 30-minute cooldown and traffic has not worsened > 15m")
    }
}
