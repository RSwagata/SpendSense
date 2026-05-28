package com.spendsense.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.spendsense.data.model.*

@Database(
    entities = [
        Transaction::class,
        Budget::class,
        MerchantMap::class,
        WeeklyInsight::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun merchantMapDao(): MerchantMapDao
    abstract fun insightDao(): InsightDao
}
