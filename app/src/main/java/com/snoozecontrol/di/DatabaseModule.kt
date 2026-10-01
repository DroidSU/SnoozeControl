package com.snoozecontrol.di

import android.content.Context
import com.snoozecontrol.data.AlarmDao
import com.snoozecontrol.data.AlarmDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAlarmDatabase(
        @ApplicationContext context: Context
    ): AlarmDatabase {
        return AlarmDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideAlarmDao(
        database: AlarmDatabase
    ): AlarmDao {
        return database.alarmDao()
    }
}
