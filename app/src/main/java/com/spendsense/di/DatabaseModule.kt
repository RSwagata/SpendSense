package com.spendsense.di

import android.content.Context
import androidx.room.Room
import com.spendsense.data.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Tells Hilt to keep these bindings alive for the entire app lifetime.
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "spendsense.db")
            .build()

    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()
    @Provides fun provideMerchantMapDao(db: AppDatabase): MerchantMapDao = db.merchantMapDao()
    @Provides fun provideInsightDao(db: AppDatabase): InsightDao = db.insightDao()
}
