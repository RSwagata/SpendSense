package com.spendsense.data.db

import androidx.room.*
import com.spendsense.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :from AND timestamp <= :to ORDER BY timestamp DESC")
    fun getByRange(from: Long, to: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY timestamp DESC")
    fun getByCategory(category: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = 'debit' AND timestamp >= :monthStart")
    suspend fun getDebitsFrom(monthStart: Long): List<Transaction>

    // Deduplication check — called before insert.
    @Query("SELECT COUNT(*) FROM transactions WHERE amount = :amount AND account = :account AND timestamp = :timestamp")
    suspend fun countDuplicates(amount: Double, account: String?, timestamp: Long): Int

    @Query("SELECT SUM(amount) FROM transactions WHERE category = :category AND type = 'debit' AND timestamp >= :monthStart")
    suspend fun sumByCategory(category: String, monthStart: Long): Double?

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): Transaction?

    @Update
    suspend fun update(transaction: Transaction)
}
