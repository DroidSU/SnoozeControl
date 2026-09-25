package com.snoozecontrol.util

import android.content.Context
import com.snoozecontrol.R
import java.util.Calendar
import java.util.Locale

object Utils {
    fun getGreeting(context: Context): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> context.getString(R.string.good_morning)
            in 12..16 -> context.getString(R.string.good_afternoon)
            in 17..20 -> context.getString(R.string.good_evening)
            else -> context.getString(R.string.good_night)
        }
    }

    /**
     * Converts a 24-hour hour value (0..23) to a 12-hour value (1..12).
     */
    fun toHour12(hour24: Int): Int {
        return when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
    }

    /**
     * Returns "AM" or "PM" for a given 24-hour hour value (0..23).
     */
    fun getAmPm(hour24: Int): String {
        return if (hour24 < 12) "AM" else "PM"
    }

    /**
     * Returns true if 24-hour hour value is AM (0..11).
     */
    fun isAm(hour24: Int): Boolean {
        return hour24 < 12
    }

    /**
     * Converts 12-hour hour (1..12) and AM flag to 24-hour hour (0..23).
     */
    fun toHour24(hour12: Int, isAm: Boolean): Int {
        return when {
            isAm && hour12 == 12 -> 0
            isAm -> hour12
            !isAm && hour12 == 12 -> 12
            else -> hour12 + 12
        }
    }

    /**
     * Formats 12-hour digits directly (e.g., hour12=7, minute=30 -> "07:30").
     */
    fun formatTimeDigits(hour12: Int, minute: Int): String {
        return String.format(Locale.getDefault(), "%02d:%02d", hour12, minute)
    }

    /**
     * Formats 24-hour hour and minute into a full 12-hour time string with AM/PM (e.g., "07:30 AM").
     */
    fun format12HourWithAmPm(hour24: Int, minute: Int): String {
        val h12 = toHour12(hour24)
        val amPm = getAmPm(hour24)
        return String.format(Locale.getDefault(), "%02d:%02d %s", h12, minute, amPm)
    }
}
