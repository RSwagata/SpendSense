package com.spendsense.data.repository

import com.spendsense.data.db.BudgetDao
import com.spendsense.data.model.Budget
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val dao: BudgetDao
) {
    fun getAllFlow(): Flow<List<Budget>> = dao.getAllFlow()

    suspend fun getByCategory(category: String): Budget? = dao.getByCategory(category)

    suspend fun upsert(budget: Budget) = dao.upsert(budget)

    suspend fun addSpend(category: String, amount: Double) = dao.addSpend(category, amount)

    suspend fun resetAllSpend() = dao.resetAllSpend()

    suspend fun delete(budget: Budget) = dao.delete(budget)
}
