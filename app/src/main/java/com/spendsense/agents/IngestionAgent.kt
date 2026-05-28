package com.spendsense.agents

import com.spendsense.data.model.Transaction
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses raw SMS text into a Transaction using per-bank regex patterns.
 * No LLM is used — this must be fast and offline.
 *
 * Resolution order:
 * 1. Per-bank patterns (SBI, HDFC, ICICI, Axis, Kotak, Yes Bank, PNB, BOB)
 * 2. Generic UPI debit/credit pattern
 * 3. Generic amount extraction fallback
 * Returns null if no pattern matches.
 */
@Singleton
class IngestionAgent @Inject constructor() {

    fun parse(smsText: String, receivedAt: Long = System.currentTimeMillis()): Transaction? {
        val normalised = smsText.trim()

        return tryBankPatterns(normalised, receivedAt)
            ?: tryGenericUpi(normalised, receivedAt)
            ?: tryGenericAmount(normalised, receivedAt)
    }

    // ── Bank-specific patterns ────────────────────────────────────────────────

    private fun tryBankPatterns(sms: String, ts: Long): Transaction? {
        for (parser in BANK_PARSERS) {
            val result = parser(sms, ts)
            if (result != null) return result
        }
        return null
    }

    private val BANK_PARSERS: List<(String, Long) -> Transaction?> = listOf(
        ::parseSbi,
        ::parseHdfc,
        ::parseIcici,
        ::parseAxis,
        ::parseKotak,
        ::parsePnb,
        ::parseBob,
        ::parseYesBank,
        ::parsePaytm,
        ::parsePhonePe,
        ::parseGooglePay,
    )

    // SBI: "Your a/c no. XX1234 is debited for Rs.450.00 on 24-05-26. Info: UPI/swiggy/912837@oksbi"
    private fun parseSbi(sms: String, ts: Long): Transaction? {
        val debitMatch = Regex(
            """a/c\s+(?:no\.?\s+)?([A-Z0-9X]+)\s+is\s+debited\s+for\s+Rs\.?([\d,]+\.?\d*)\s+on\s+([\d\-/]+).*?Info:\s*(.+)""",
            RegexOption.IGNORE_CASE
        ).find(sms) ?: return null

        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = debitMatch.groupValues[2].replace(",", "").toDoubleOrNull() ?: return null,
            type = "debit",
            merchant = null,
            category = null,
            account = debitMatch.groupValues[1],
            timestamp = ts,
            source = "sms",
            rawText = sms
        ).withRawMerchant(debitMatch.groupValues[4].trim())
    }

    // HDFC: "Rs.450.00 debited from a/c **1234 on 24-05-26:14:32:00 IST. UPI Ref:912837. If not you? helpdesk..."
    private fun parseHdfc(sms: String, ts: Long): Transaction? {
        val match = Regex(
            """Rs\.?([\d,]+\.?\d*)\s+(debited|credited)\s+from\s+a/c\s+\*+(\d+).*?(?:UPI|Info|Ref)[:\s]+([A-Za-z0-9@/\-_]+)""",
            RegexOption.IGNORE_CASE
        ).find(sms) ?: return null

        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null,
            type = if (match.groupValues[2].lowercase() == "debited") "debit" else "credit",
            merchant = null,
            category = null,
            account = "XX${match.groupValues[3]}",
            timestamp = ts,
            source = "sms",
            rawText = sms
        ).withRawMerchant(match.groupValues[4].trim())
    }

    // ICICI: "ICICI Bank Acct XX1234 debited for Rs 450.00 on 24-May-26; UPI:swiggy@oksbi"
    private fun parseIcici(sms: String, ts: Long): Transaction? {
        val match = Regex(
            """ICICI\s+Bank\s+Acct\s+([A-Z0-9X]+)\s+(debited|credited)\s+for\s+Rs\.?\s*([\d,]+\.?\d*).*?(?:UPI:|Info:)\s*([A-Za-z0-9@/\-_]+)""",
            RegexOption.IGNORE_CASE
        ).find(sms) ?: return null

        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = match.groupValues[3].replace(",", "").toDoubleOrNull() ?: return null,
            type = if (match.groupValues[2].lowercase() == "debited") "debit" else "credit",
            merchant = null,
            category = null,
            account = match.groupValues[1],
            timestamp = ts,
            source = "sms",
            rawText = sms
        ).withRawMerchant(match.groupValues[4].trim())
    }

    // Axis: "INR 450.00 debited from Axis Bank A/c XX1234 for UPI txn. UPI Ref: 912837. Merchant: swiggy@oksbi"
    private fun parseAxis(sms: String, ts: Long): Transaction? {
        val match = Regex(
            """INR\s+([\d,]+\.?\d*)\s+(debited|credited)\s+from\s+Axis\s+Bank\s+A/c\s+([A-Z0-9X]+).*?(?:Merchant:|UPI Ref:|Info:)\s*([A-Za-z0-9@/\-_]+)""",
            RegexOption.IGNORE_CASE
        ).find(sms) ?: return null

        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null,
            type = if (match.groupValues[2].lowercase() == "debited") "debit" else "credit",
            merchant = null,
            category = null,
            account = match.groupValues[3],
            timestamp = ts,
            source = "sms",
            rawText = sms
        ).withRawMerchant(match.groupValues[4].trim())
    }

    // Kotak: "Kotak Bk: Rs.450.00 debited from a/c XX1234 on 24-05-26. UPI:swiggy@oksbi. Bal:Rs.12000"
    private fun parseKotak(sms: String, ts: Long): Transaction? {
        val match = Regex(
            """Kotak\s+Bk.*?Rs\.?([\d,]+\.?\d*)\s+(debited|credited)\s+from\s+a/c\s+([A-Z0-9X]+).*?UPI:([A-Za-z0-9@/\-_]+)""",
            RegexOption.IGNORE_CASE
        ).find(sms) ?: return null

        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null,
            type = if (match.groupValues[2].lowercase() == "debited") "debit" else "credit",
            merchant = null,
            category = null,
            account = match.groupValues[3],
            timestamp = ts,
            source = "sms",
            rawText = sms
        ).withRawMerchant(match.groupValues[4].trim())
    }

    // PNB: "PNB: Your A/C No. XX1234 debited by Rs.450.00 on 24/05/26. UPI Ref: swiggy@oksbi."
    private fun parsePnb(sms: String, ts: Long): Transaction? {
        val match = Regex(
            """PNB.*?A/C\s+No\.\s+([A-Z0-9X]+)\s+(debited|credited)\s+by\s+Rs\.?([\d,]+\.?\d*).*?UPI\s+Ref:\s+([A-Za-z0-9@/\-_]+)""",
            RegexOption.IGNORE_CASE
        ).find(sms) ?: return null

        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = match.groupValues[3].replace(",", "").toDoubleOrNull() ?: return null,
            type = if (match.groupValues[2].lowercase() == "debited") "debit" else "credit",
            merchant = null,
            category = null,
            account = match.groupValues[1],
            timestamp = ts,
            source = "sms",
            rawText = sms
        ).withRawMerchant(match.groupValues[4].trim())
    }

    // BOB: "Dear Customer, Rs.450.00 has been debited from your a/c XX1234. UPI Ref: swiggy@oksbi"
    private fun parseBob(sms: String, ts: Long): Transaction? {
        val match = Regex(
            """Rs\.?([\d,]+\.?\d*)\s+has\s+been\s+(debited|credited)\s+from\s+your\s+a/c\s+([A-Z0-9X]+).*?UPI\s+Ref:\s+([A-Za-z0-9@/\-_]+)""",
            RegexOption.IGNORE_CASE
        ).find(sms) ?: return null

        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null,
            type = if (match.groupValues[2].lowercase() == "debited") "debit" else "credit",
            merchant = null,
            category = null,
            account = match.groupValues[3],
            timestamp = ts,
            source = "sms",
            rawText = sms
        ).withRawMerchant(match.groupValues[4].trim())
    }

    // Yes Bank: "YBL: INR 450.00 debited from A/c XX1234 via UPI. VPA: swiggy@oksbi. Avl Bal: INR 12000"
    private fun parseYesBank(sms: String, ts: Long): Transaction? {
        val match = Regex(
            """YBL.*?INR\s+([\d,]+\.?\d*)\s+(debited|credited)\s+from\s+A/c\s+([A-Z0-9X]+).*?VPA:\s+([A-Za-z0-9@/\-_]+)""",
            RegexOption.IGNORE_CASE
        ).find(sms) ?: return null

        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null,
            type = if (match.groupValues[2].lowercase() == "debited") "debit" else "credit",
            merchant = null,
            category = null,
            account = match.groupValues[3],
            timestamp = ts,
            source = "sms",
            rawText = sms
        ).withRawMerchant(match.groupValues[4].trim())
    }

    // Paytm: "Paytm: Rs.450 paid to Swiggy. Txn ID: 912837. Your Paytm wallet bal: Rs.5000"
    private fun parsePaytm(sms: String, ts: Long): Transaction? {
        val match = Regex(
            """Paytm.*?Rs\.?([\d,]+\.?\d*)\s+(?:paid to|debited|received from)\s+(.+?)[\.\,]""",
            RegexOption.IGNORE_CASE
        ).find(sms) ?: return null

        val isDebit = sms.contains("paid to", ignoreCase = true) ||
            sms.contains("debited", ignoreCase = true)

        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null,
            type = if (isDebit) "debit" else "credit",
            merchant = null,
            category = null,
            account = "PYTM",
            timestamp = ts,
            source = "sms",
            rawText = sms
        ).withRawMerchant("PYTM/${match.groupValues[2].trim()}")
    }

    // PhonePe: "PhonePe: Rs.450 sent to 9182736450@ybl on 24-05-26"
    private fun parsePhonePe(sms: String, ts: Long): Transaction? {
        val match = Regex(
            """PhonePe.*?Rs\.?([\d,]+\.?\d*)\s+(sent|received)\s+(?:to|from)\s+([A-Za-z0-9@/\-_]+)""",
            RegexOption.IGNORE_CASE
        ).find(sms) ?: return null

        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null,
            type = if (match.groupValues[2].lowercase() == "sent") "debit" else "credit",
            merchant = null,
            category = null,
            account = "PHONEPE",
            timestamp = ts,
            source = "sms",
            rawText = sms
        ).withRawMerchant(match.groupValues[3].trim())
    }

    // Google Pay: "You paid Rs.450 to Swiggy via GPay on 24 May 26"
    private fun parseGooglePay(sms: String, ts: Long): Transaction? {
        val match = Regex(
            """(?:You paid|You received)\s+Rs\.?([\d,]+\.?\d*)\s+(?:to|from)\s+(.+?)\s+via\s+GPay""",
            RegexOption.IGNORE_CASE
        ).find(sms) ?: return null

        val isDebit = sms.startsWith("You paid", ignoreCase = true)

        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null,
            type = if (isDebit) "debit" else "credit",
            merchant = null,
            category = null,
            account = "GPAY",
            timestamp = ts,
            source = "sms",
            rawText = sms
        ).withRawMerchant(match.groupValues[2].trim())
    }

    // ── Generic fallbacks ─────────────────────────────────────────────────────

    // Catches most UPI SMS that don't match a specific bank pattern.
    private fun tryGenericUpi(sms: String, ts: Long): Transaction? {
        val match = Regex(
            """(?:Rs\.?|INR)\s*([\d,]+\.?\d*)\s+(?:debited|credited|paid|sent|received).*?([A-Za-z0-9.\-_]+@[A-Za-z0-9.\-_]+)""",
            RegexOption.IGNORE_CASE
        ).find(sms) ?: return null

        val isDebit = sms.contains(Regex("debited|paid|sent", RegexOption.IGNORE_CASE))

        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null,
            type = if (isDebit) "debit" else "credit",
            merchant = null,
            category = null,
            account = null,
            timestamp = ts,
            source = "sms",
            rawText = sms
        ).withRawMerchant(match.groupValues[2].trim())
    }

    // Last resort — just extract the amount and mark merchant as unknown.
    private fun tryGenericAmount(sms: String, ts: Long): Transaction? {
        val bankKeywords = Regex("debited|credited|paid|wallet|transaction|txn|upi|account|a/c|bank", RegexOption.IGNORE_CASE)
        if (!sms.contains(bankKeywords)) return null

        val amountMatch = Regex("""(?:Rs\.?|INR)\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)
            .find(sms) ?: return null

        val isDebit = sms.contains(Regex("debited|paid|sent", RegexOption.IGNORE_CASE))

        return Transaction(
            id = UUID.randomUUID().toString(),
            amount = amountMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null,
            type = if (isDebit) "debit" else "credit",
            merchant = null,
            category = null,
            account = null,
            timestamp = ts,
            source = "sms",
            rawText = sms
        )
    }

    // Extension that copies the transaction and sets merchant to the raw string
    // (later resolved by ParserAgent).
    private fun Transaction.withRawMerchant(raw: String): Transaction =
        copy(merchant = raw.ifBlank { null })
}
