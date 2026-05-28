package com.spendsense.data.repository

import com.spendsense.data.db.InsightDao
import com.spendsense.data.db.MerchantMapDao
import com.spendsense.data.db.TransactionDao
import com.spendsense.data.model.MerchantMap
import com.spendsense.data.model.WeeklyInsight
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightRepository @Inject constructor(
    private val insightDao: InsightDao,
    private val transactionDao: TransactionDao,
    private val merchantMapDao: MerchantMapDao
) {
    fun getAllInsightsFlow(): Flow<List<WeeklyInsight>> = insightDao.getAllFlow()

    suspend fun saveInsight(insight: WeeklyInsight) = insightDao.insert(insight)

    suspend fun getLatestInsight(): WeeklyInsight? = insightDao.getLatest()

    suspend fun findMerchant(rawMerchant: String): MerchantMap? =
        merchantMapDao.find(rawMerchant)

    suspend fun saveMerchant(entry: MerchantMap) = merchantMapDao.upsert(entry)

    suspend fun getTransactionsForWeek(weekStart: Long, weekEnd: Long): List<com.spendsense.data.model.Transaction> =
        transactionDao.getDebitsFrom(weekStart)
            .filter { it.timestamp < weekEnd }
}
