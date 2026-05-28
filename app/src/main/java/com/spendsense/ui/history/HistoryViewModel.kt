package com.spendsense.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spendsense.agents.ClassifierAgent
import com.spendsense.data.model.Transaction
import com.spendsense.data.repository.InsightRepository
import com.spendsense.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val transactions: List<Transaction> = emptyList(),
    val selectedCategory: String? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val insightRepository: InsightRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        transactionRepository.getAllFlow(),
        _selectedCategory
    ) { transactions, category ->
        val filtered = if (category == null) transactions
                       else transactions.filter { it.category == category }
        HistoryUiState(
            transactions = filtered,
            selectedCategory = category,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState()
    )

    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
    }

    // User corrects a transaction's category; saves to merchant_map for future use.
    fun recategorise(transaction: Transaction, newCategory: String) {
        viewModelScope.launch {
            transactionRepository.update(transaction.copy(category = newCategory))
            // Persist correction so future transactions from same merchant get this category.
            transaction.merchant?.let { merchantName ->
                insightRepository.findMerchant(merchantName)?.let { cached ->
                    insightRepository.saveMerchant(cached.copy(categoryHint = newCategory))
                }
            }
        }
    }
}
