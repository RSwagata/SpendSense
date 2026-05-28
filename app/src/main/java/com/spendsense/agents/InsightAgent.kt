package com.spendsense.agents

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.spendsense.data.model.Transaction
import com.spendsense.data.model.WeeklyInsight
import com.spendsense.data.repository.InsightRepository
import com.spendsense.llm.LlmProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.NumberFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates a weekly spending summary using the configured LLM.
 * Triggered by InsightWorker every Sunday at 8am.
 *
 * The prompt sends only aggregated data — no raw SMS text, no account numbers.
 * Privacy: only the merchant names and category totals are included.
 */
@Singleton
class InsightAgent @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: InsightRepository,
    private val llmProvider: LlmProvider
) {

    companion object {
        const val CHANNEL_ID = "weekly_insights"
        const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    suspend fun generateAndNotify() {
        val now = System.currentTimeMillis()
        val weekStart = getWeekStart(now)
        val prevWeekStart = weekStart - 7 * 24 * 60 * 60 * 1000L

        val thisWeekTxns = repository.getTransactionsForWeek(weekStart, now)
        val prevWeekTxns = repository.getTransactionsForWeek(prevWeekStart, weekStart)

        if (thisWeekTxns.isEmpty()) return

        val summary = buildSummary(thisWeekTxns, prevWeekTxns)

        val insight = WeeklyInsight(
            id = UUID.randomUUID().toString(),
            weekStart = weekStart,
            summaryText = summary,
            generatedAt = now
        )
        repository.saveInsight(insight)
        fireNotification(summary)
    }

    private suspend fun buildSummary(
        thisWeek: List<Transaction>,
        prevWeek: List<Transaction>
    ): String {
        val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }

        val thisDebits = thisWeek.filter { it.type == "debit" }
        val prevDebits = prevWeek.filter { it.type == "debit" }

        val thisTotal = thisDebits.sumOf { it.amount }
        val prevTotal = prevDebits.sumOf { it.amount }

        val categoryTotals = thisDebits.groupBy { it.category ?: "Other" }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }

        val prevCategoryTotals = prevDebits.groupBy { it.category ?: "Other" }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }

        val frequentMerchants = thisDebits.groupBy { it.merchant ?: "Unknown" }
            .filter { (_, txns) -> txns.size >= 3 }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } to txns.size }

        val largestTxn = thisDebits.maxByOrNull { it.amount }

        val lateNight = thisDebits.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hour in 23..23 || hour in 0..2
        }

        val totalChange = thisTotal - prevTotal
        val changeStr = if (totalChange >= 0) "₹${fmt.format(totalChange)} more" else "₹${fmt.format(-totalChange)} less"

        val prompt = buildString {
            appendLine("Generate a friendly, conversational 3-4 sentence spending summary for an Indian user.")
            appendLine("Tone: warm, personal, like a financial friend. Use ₹ for rupees.")
            appendLine()
            appendLine("Data:")
            appendLine("- This week total: ₹${fmt.format(thisTotal)} ($changeStr than last week's ₹${fmt.format(prevTotal)})")
            appendLine("- Category breakdown:")
            categoryTotals.forEach { (cat, amt) ->
                val prev = prevCategoryTotals[cat] ?: 0.0
                val diff = amt - prev
                val diffStr = if (diff >= 0) "+₹${fmt.format(diff)}" else "-₹${fmt.format(-diff)}"
                appendLine("  $cat: ₹${fmt.format(amt)} ($diffStr vs last week)")
            }
            if (frequentMerchants.isNotEmpty()) {
                appendLine("- Frequent merchants (3+ times):")
                frequentMerchants.forEach { (merchant, pair) ->
                    appendLine("  $merchant: ₹${fmt.format(pair.first)} across ${pair.second} visits")
                }
            }
            largestTxn?.let {
                appendLine("- Largest single transaction: ₹${fmt.format(it.amount)} at ${it.merchant ?: "Unknown"}")
            }
            val credits = thisWeek.filter { it.type == "credit" }.sumOf { it.amount }
            if (credits > 0) {
                val savingsRate = ((credits - thisTotal) / credits * 100).toInt()
                appendLine("- Credits this week: ₹${fmt.format(credits)}, savings rate: ${savingsRate}%")
            }
            if (lateNight.isNotEmpty()) {
                val lnTotal = lateNight.sumOf { it.amount }
                appendLine("- Late-night spending (11pm–2am): ₹${fmt.format(lnTotal)} across ${lateNight.size} transactions")
            }
        }

        val llmResponse = llmProvider.complete(prompt, maxTokens = 250)

        return llmResponse.ifBlank { buildFallbackSummary(thisTotal, prevTotal, categoryTotals, fmt) }
    }

    private fun buildFallbackSummary(
        thisTotal: Double,
        prevTotal: Double,
        categories: List<Map.Entry<String, Double>>,
        fmt: NumberFormat
    ): String {
        val change = thisTotal - prevTotal
        val changeStr = if (change >= 0) "₹${fmt.format(change)} more" else "₹${fmt.format(-change)} less"
        val topCategory = categories.firstOrNull()?.let { "${it.key}: ₹${fmt.format(it.value)}" } ?: ""
        return "You spent ₹${fmt.format(thisTotal)} this week — $changeStr than last week. ${if (topCategory.isNotEmpty()) "Top category: $topCategory." else ""}"
    }

    private fun fireNotification(summary: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "My spending this week:\n\n$summary\n\n— SpendSense")
            setPackage("com.whatsapp")
        }
        val pendingShare = PendingIntent.getActivity(
            context, 0, shareIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Your weekly spending summary is ready")
            .setContentText(summary.take(100))
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_share, "Share to WhatsApp", pendingShare)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Weekly Insights",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Weekly spending summary notifications"
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun getWeekStart(now: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // Roll back to Monday
        val daysBack = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -daysBack)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
