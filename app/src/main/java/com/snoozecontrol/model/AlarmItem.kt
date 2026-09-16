package com.snoozecontrol.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "alarms")
data class AlarmItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean,
    val challengeType: ChallengeType = ChallengeType.MATH,
    val targetBarcode: String? = null
) {
    val displayTime: String
        get() = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
}
