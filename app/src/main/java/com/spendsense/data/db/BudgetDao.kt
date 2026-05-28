package com.spendsense.data.db

import androidx.room.*
import com.spendsense.data.model.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: Budget)

    @Query("SELECT * FROM budgets")
    fun getAllFlow(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE category = :category")
    suspend fun getByCategory(category: String): Budget?

    @Query("UPDATE budgets SET current_spend = current_spend + :amount WHERE category = :category")
    suspend fun addSpend(category: String, amount: Double)

    // Called at the start of each month to reset running totals.
    @Query("UPDATE budgets SET current_spend = 0")
    suspend fun resetAllSpend()

    @Delete
    suspend fun delete(budget: Budget)
}
