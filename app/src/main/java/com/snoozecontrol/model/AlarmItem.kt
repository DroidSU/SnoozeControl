package com.snoozecontrol.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.snoozecontrol.util.Utils
import java.util.Calendar

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
    val isBedtimeReminderEnabled: Boolean = true,
    val snoozedUntilMillis: Long? = null,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String = "Default Alarm Sound",
    val mathDifficulty: MathDifficulty = MathDifficulty.MEDIUM,
    val skippedOccurrenceMillis: Long? = null
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

    fun calculateNextCalendar(): Calendar {
        val now = Calendar.getInstance()
        val nowMs = now.timeInMillis

        if (snoozedUntilMillis != null && snoozedUntilMillis > nowMs + 1000L) {
            return Calendar.getInstance().apply {
                timeInMillis = snoozedUntilMillis
            }
        }

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val days = selectedDays
        if (days.isEmpty()) {
            if (calendar.timeInMillis <= nowMs + 1000L) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar
        }

        val calDayMap = mapOf(
            1 to Calendar.MONDAY,
            2 to Calendar.TUESDAY,
            3 to Calendar.WEDNESDAY,
            4 to Calendar.THURSDAY,
            5 to Calendar.FRIDAY,
            6 to Calendar.SATURDAY,
            7 to Calendar.SUNDAY
        )
        val targetCalDays = days.mapNotNull { calDayMap[it] }.toSet()

        for (i in 0..7) {
            val testCal = calendar.clone() as Calendar
            testCal.add(Calendar.DAY_OF_YEAR, i)
            if (testCal.timeInMillis <= nowMs + 1000L) continue
            if (skippedOccurrenceMillis != null && testCal.timeInMillis <= skippedOccurrenceMillis) continue

            if (testCal.get(Calendar.DAY_OF_WEEK) in targetCalDays) {
                return testCal
            }
        }

        if (calendar.timeInMillis <= nowMs + 1000L) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar
    }

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
