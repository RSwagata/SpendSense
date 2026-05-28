package com.spendsense.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.data.model.Budget
import com.spendsense.data.model.Transaction
import com.spendsense.data.repository.BudgetRepository
import com.spendsense.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val recentTransactions: List<Transaction> = emptyList(),
    val categoryTotals: Map<String, Double> = emptyMap(),
    val budgets: List<Budget> = emptyList(),
    val totalSpentThisMonth: Double = 0.0,
    val totalCreditThisMonth: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
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

    val uiState: StateFlow<DashboardUiState> = combine(
        transactionRepository.getAllFlow(),
        budgetRepository.getAllFlow()
    ) { transactions, budgets ->
        val thisMonth = transactions.filter { it.timestamp >= monthStart }
        val debits = thisMonth.filter { it.type == "debit" }
        val credits = thisMonth.filter { it.type == "credit" }

        DashboardUiState(
            recentTransactions = transactions.take(10),
            categoryTotals = debits
                .groupBy { it.category ?: "Other" }
                .mapValues { (_, txns) -> txns.sumOf { it.amount } },
            budgets = budgets,
            totalSpentThisMonth = debits.sumOf { it.amount },
            totalCreditThisMonth = credits.sumOf { it.amount },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )
}
