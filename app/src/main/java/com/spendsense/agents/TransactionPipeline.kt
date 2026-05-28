package com.spendsense.agents

import com.spendsense.data.repository.InsightRepository
import com.spendsense.data.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the full SMS → Transaction pipeline.
 * Flow: Ingest → Parse merchant → Classify → Save → Budget check
 */
@Singleton
class TransactionPipeline @Inject constructor(
    private val ingestionAgent: IngestionAgent,
    private val parserAgent: ParserAgent,
    private val classifierAgent: ClassifierAgent,
    private val budgetAlertAgent: BudgetAlertAgent,
    private val transactionRepository: TransactionRepository,
    private val insightRepository: InsightRepository
) {
    suspend fun process(smsBody: String, receivedAt: Long = System.currentTimeMillis()) {
        val raw = ingestionAgent.parse(smsBody, receivedAt) ?: return

        val cleanMerchant = parserAgent.resolve(raw.merchant ?: "")

        val categoryHint = insightRepository.findMerchant(raw.merchant ?: "")?.categoryHint
        val category = classifierAgent.classify(cleanMerchant, categoryHint)

        val transaction = raw.copy(merchant = cleanMerchant, category = category)
        val inserted = transactionRepository.insertIfNotDuplicate(transaction)

        if (inserted && transaction.type == "debit") {
            budgetAlertAgent.check(transaction)
        }
    }
}
