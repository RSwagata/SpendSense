package com.spendsense.data.repository

import com.spendsense.data.db.TransactionDao
import com.spendsense.data.model.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val dao: TransactionDao
) {
    fun getAllFlow(): Flow<List<Transaction>> = dao.getAllFlow()

    fun getByRange(from: Long, to: Long): Flow<List<Transaction>> = dao.getByRange(from, to)

    fun getByCategory(category: String): Flow<List<Transaction>> = dao.getByCategory(category)

    suspend fun insertIfNotDuplicate(transaction: Transaction): Boolean {
        val duplicates = dao.countDuplicates(transaction.amount, transaction.account, transaction.timestamp)
        if (duplicates > 0) return false
        dao.insert(transaction)
        return true
    }

    suspend fun getDebitsFrom(monthStart: Long): List<Transaction> = dao.getDebitsFrom(monthStart)

    suspend fun sumByCategory(category: String, monthStart: Long): Double =
        dao.sumByCategory(category, monthStart) ?: 0.0

    suspend fun update(transaction: Transaction) = dao.update(transaction)
}
