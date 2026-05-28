package com.spendsense.agents

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.spendsense.data.model.Transaction
import com.spendsense.data.repository.BudgetRepository
import com.spendsense.data.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fires budget alerts when a category reaches 80% or 100% of its monthly limit.
 * No LLM. Pure arithmetic against the budgets table.
 * Called synchronously by TransactionPipeline after each debit is saved.
 */
@Singleton
class BudgetAlertAgent @Inject constructor(
    @ApplicationContext private val context: Context,
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {

    companion object {
        const val CHANNEL_ID = "budget_alerts"
        const val CHANNEL_NAME = "Budget Alerts"
    }

    init {
        createNotificationChannel()
    }

    suspend fun check(transaction: Transaction) {
        if (transaction.type != "debit") return
        val category = transaction.category ?: return

        val budget = budgetRepository.getByCategory(category) ?: return
        if (budget.monthlyLimit <= 0) return

        val monthStart = currentMonthStart()
        val totalSpent = transactionRepository.sumByCategory(category, monthStart)
        val percentage = totalSpent / budget.monthlyLimit

        when {
            percentage >= 1.0 -> fireAlert(
                category = category,
                spent = totalSpent,
                limit = budget.monthlyLimit,
                isExceeded = true
            )
            percentage >= 0.8 -> fireAlert(
                category = category,
                spent = totalSpent,
                limit = budget.monthlyLimit,
                isExceeded = false
            )
        }
    }

    private fun fireAlert(category: String, spent: Double, limit: Double, isExceeded: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }

        val title = if (isExceeded) "Budget exceeded: $category" else "Budget warning: $category"
        val body = if (isExceeded)
            "You've spent ₹${fmt.format(spent)} in $category — ₹${fmt.format(spent - limit)} over your ₹${fmt.format(limit)} budget."
        else
            "You've spent ₹${fmt.format(spent)} in $category — 80% of your ₹${fmt.format(limit)} budget."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(if (isExceeded) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // Use category hash as notification ID so 80% and 100% alerts replace each other per category.
        notificationManager.notify(category.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when spending approaches or exceeds budget limits"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun currentMonthStart(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
