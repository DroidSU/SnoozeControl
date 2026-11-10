package com.snoozecontrol.di

import android.content.Context
import com.snoozecontrol.data.billing.BillingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

    @Provides
    @Singleton
    fun provideBillingRepository(
        @ApplicationContext context: Context
    ): BillingRepository {
        return BillingRepository(context)
    }
}
