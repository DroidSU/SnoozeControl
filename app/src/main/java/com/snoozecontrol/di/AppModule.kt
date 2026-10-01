package com.snoozecontrol.di

import android.content.Context
import com.snoozecontrol.scheduler.AlarmScheduler
import com.snoozecontrol.scheduler.AndroidAlarmScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAlarmScheduler(
        @ApplicationContext context: Context
    ): AlarmScheduler {
        return AndroidAlarmScheduler(context)
    }
}
