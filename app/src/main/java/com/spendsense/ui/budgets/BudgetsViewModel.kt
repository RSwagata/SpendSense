package com.spendsense.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.model.Budget
import com.spendsense.data.repository.BudgetRepository
import com.spendsense.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class BudgetsUiState(
    val budgets: List<Budget> = emptyList(),
    val categorySpend: Map<String, Double> = emptyMap(),
    val isLoading: Boolean = true
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val monthStart: Long get() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    val uiState: StateFlow<BudgetsUiState> = combine(
        budgetRepository.getAllFlow(),
        transactionRepository.getAllFlow()
    ) { budgets, transactions ->
        val thisMonth = transactions.filter { it.timestamp >= monthStart && it.type == "debit" }
        val spendMap = thisMonth
            .groupBy { it.category ?: "Other" }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
        BudgetsUiState(
            budgets = budgets,
            categorySpend = spendMap,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetsUiState()
    )

    fun saveBudget(category: String, limit: Double) {
        viewModelScope.launch {
            budgetRepository.upsert(Budget(category = category, monthlyLimit = limit))
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            budgetRepository.delete(budget)
        }
    }
}
