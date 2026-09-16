package com.snoozecontrol.data

import androidx.room.*
import com.snoozecontrol.model.AlarmItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms")
    fun getAllAlarms(): Flow<List<AlarmItem>>

    @Query("SELECT * FROM alarms WHERE isEnabled = 1")
    suspend fun getEnabledAlarms(): List<AlarmItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlarm(alarm: AlarmItem): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmItem)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmItem)

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Int): AlarmItem?
}
