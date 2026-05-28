package com.spendsense.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.data.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    if (state.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { MonthSummaryCard(state) }

        if (state.categoryTotals.isNotEmpty()) {
            item { CategoryBreakdownCard(state.categoryTotals) }
        }

        if (state.budgets.isNotEmpty()) {
            item { BudgetProgressCard(state.budgets, state.categoryTotals) }
        }

        item {
            Text(
                "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (state.recentTransactions.isEmpty()) {
            item {
                EmptyStateCard()
            }
        } else {
            items(state.recentTransactions) { txn ->
                TransactionRow(txn)
            }
        }
    }
}

@Composable
private fun MonthSummaryCard(state: DashboardUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "This Month",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                formatRupees(state.totalSpentThisMonth),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabelValue("Credited", formatRupees(state.totalCreditThisMonth), Color(0xFF2E7D32))
                LabelValue("Debited", formatRupees(state.totalSpentThisMonth), Color(0xFFC62828))
            }
        }
    }
}

@Composable
private fun CategoryBreakdownCard(totals: Map<String, Double>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Spending by Category", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            totals.entries.sortedByDescending { it.value }.forEach { (category, amount) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(categoryColor(category), shape = RoundedCornerShape(4.dp))
                        )
                        Text(category, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(formatRupees(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressCard(budgets: List<com.spendsense.data.model.Budget>, spendMap: Map<String, Double>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Budget Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            budgets.forEach { budget ->
                val spent = spendMap[budget.category] ?: 0.0
                val progress = if (budget.monthlyLimit > 0) (spent / budget.monthlyLimit).coerceIn(0.0, 1.0) else 0.0
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(budget.category, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${formatRupees(spent)} / ${formatRupees(budget.monthlyLimit)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = if (progress >= 1.0) MaterialTheme.colorScheme.error
                               else if (progress >= 0.8) Color(0xFFFF6D00)
                               else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(txn: Transaction) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(20.dp),
                color = categoryColor(txn.category ?: "Other").copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = categoryIcon(txn.category ?: "Other"),
                        contentDescription = null,
                        tint = categoryColor(txn.category ?: "Other"),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    txn.merchant ?: "Unknown",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    "${txn.category ?: "Other"} · ${formatDate(txn.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "${if (txn.type == "debit") "-" else "+"}${formatRupees(txn.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (txn.type == "debit") MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
            )
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("No transactions yet", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Transactions will appear automatically when you receive bank SMS messages.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String, color: Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

private fun formatRupees(amount: Double): String =
    "₹${NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }.format(amount)}"

private fun formatDate(ts: Long): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ts))

private fun categoryColor(category: String): Color = when (category) {
    "Food" -> Color(0xFFE65100)
    "Transport" -> Color(0xFF1565C0)
    "Utilities" -> Color(0xFF6A1B9A)
    "Rent" -> Color(0xFF4E342E)
    "Shopping" -> Color(0xFFAD1457)
    "Medical" -> Color(0xFF00695C)
    "Entertainment" -> Color(0xFF283593)
    "Savings" -> Color(0xFF2E7D32)
    else -> Color(0xFF546E7A)
}

private fun categoryIcon(category: String) = when (category) {
    "Food" -> Icons.Default.Restaurant
    "Transport" -> Icons.Default.DirectionsCar
    "Utilities" -> Icons.Default.ElectricBolt
    "Rent" -> Icons.Default.Home
    "Shopping" -> Icons.Default.ShoppingCart
    "Medical" -> Icons.Default.LocalHospital
    "Entertainment" -> Icons.Default.Movie
    "Savings" -> Icons.Default.Savings
    else -> Icons.Default.AccountBalanceWallet
}
