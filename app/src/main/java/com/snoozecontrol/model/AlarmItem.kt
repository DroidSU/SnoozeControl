package com.snoozecontrol.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.snoozecontrol.util.Utils

@Entity(tableName = "alarms")
data class AlarmItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean,
    val challengeType: ChallengeType = ChallengeType.MATH,
    val targetBarcode: String? = null,
    val repeatDays: String = "", // Comma-separated day numbers (1=Mon, 2=Tue, ..., 7=Sun) or empty for once
    val snoozeDurationMinutes: Int = 5,
    val maxSnoozeCount: Int = 3,
    val snoozeCount: Int = 0,
    val isBedtimeReminderEnabled: Boolean = true
) {
    val hour12: Int
        get() = Utils.toHour12(hour)

    val amPm: String
        get() = Utils.getAmPm(hour)

    val displayTime: String
        get() = Utils.format12HourWithAmPm(hour, minute)

    val selectedDays: Set<Int>
        get() = if (repeatDays.isBlank()) emptySet() else repeatDays.split(",")
            .mapNotNull { it.trim().toIntOrNull() }.toSet()

    val isDaily: Boolean
        get() = selectedDays.size == 7

    val isOnce: Boolean
        get() = selectedDays.isEmpty()

    fun getRepeatSummary(): String {
        val days = selectedDays
        return when {
            days.isEmpty() -> "Once"
            days.size == 7 -> "Daily"
            days == setOf(1, 2, 3, 4, 5) -> "Weekdays"
            days == setOf(6, 7) -> "Weekends"
            else -> {
                val dayNames = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                days.sorted().mapNotNull { dayNum ->
                    if (dayNum in 1..7) dayNames[dayNum - 1] else null
                }.joinToString(", ")
            }
        }
    }
}
