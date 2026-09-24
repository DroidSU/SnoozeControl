package com.snoozecontrol.util

import android.content.Context
import com.snoozecontrol.R
import java.util.Calendar

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
}
