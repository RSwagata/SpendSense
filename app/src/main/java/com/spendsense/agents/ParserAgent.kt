package com.spendsense.agents

import android.content.Context
import android.util.Log
import com.spendsense.data.model.MerchantMap
import com.spendsense.data.repository.InsightRepository
import com.spendsense.llm.LlmProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves raw_merchant strings to clean human-readable names.
 * Resolution order (cheapest first):
 *   1. DB merchant_map cache
 *   2. Bundled UPI handle lookup (upi_handles.json)
 *   3. LLM call
 *   4. Fallback to "Unknown"
 *
 * After step 3, saves result to DB so the LLM is never called again for the same merchant.
 */
@Singleton
class ParserAgent @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: InsightRepository,
    private val llmProvider: LlmProvider
) {

    private val upiHandles: Map<String, String> by lazy { loadUpiHandles() }

    suspend fun resolve(rawMerchant: String): String {
        if (rawMerchant.isBlank()) return "Unknown"

        // 1. DB cache
        val cached = repository.findMerchant(rawMerchant)
        if (cached != null) return cached.cleanName

        // 2. Bundled lookup
        val fromLookup = resolveFromLookup(rawMerchant)
        if (fromLookup != null) {
            saveMerchant(rawMerchant, fromLookup, "rule")
            return fromLookup
        }

        // 3. LLM
//        Log.i("Parseragent", "LLM Calling --- ")
        val fromLlm = resolveFromLlm(rawMerchant)
        if (fromLlm.isNotBlank()) {
            saveMerchant(rawMerchant, fromLlm, "llm")
            return fromLlm
        }

        // 4. Fallback
        saveMerchant(rawMerchant, "Unknown", "rule")
        return "Unknown"
    }

    private fun resolveFromLookup(raw: String): String? {
        val lower = raw.lowercase()

        // Direct key match
        upiHandles[lower]?.let { return it }

        // UPI VPA format: name@handle or UPI/name/ref@handle
        val vpa = extractVpa(lower)
        if (vpa != null) {
            // Try merchant name part (before @)
            val merchantPart = vpa.substringBefore("@").substringAfterLast("/")
            upiHandles[merchantPart]?.let { return it }

            // Try handle part (after @), e.g. "oksbi" → "SBI"
            val handle = vpa.substringAfter("@")
            upiHandles[handle]?.let { return it }

            // Try known merchant prefixes in the merchant part
            for ((key, value) in upiHandles) {
                if (merchantPart.contains(key)) return value
            }
        }

        // Plain name match — substring search against known merchants
        for ((key, value) in upiHandles) {
            if (lower.contains(key)) return value
        }

        return null
    }

    private fun extractVpa(raw: String): String? {
        // Matches patterns like: name@handle, UPI/name/ref@handle, PYTM/name
        val atIndex = raw.indexOf('@')
        if (atIndex > 0) return raw
        return null
    }

    private suspend fun resolveFromLlm(raw: String): String {
        val prompt = """You are a merchant name resolver for Indian bank transactions.
Given a raw UPI/SMS merchant identifier, return ONLY the clean merchant name with no extra words.
Examples:
  upi/swiggy/912837@oksbi → Swiggy
  pytm/9182736450 → Paytm
  big bazaar store 0042 → Big Bazaar
  9876543210@ybl → UPI Transfer

Raw identifier: $raw
Clean name:"""

        return try {
            val response = llmProvider.complete(prompt, maxTokens = 200).trim()
            android.util.Log.d("ParserAgent", "LLM raw=$raw response=$response")
            response.lines().firstOrNull()?.trim() ?: ""
        } catch (e: Exception) {
            android.util.Log.e("ParserAgent", "LLM call failed for raw=$raw", e)
            ""
        }
    }

    private suspend fun saveMerchant(raw: String, clean: String, source: String) {
        repository.saveMerchant(
            MerchantMap(
                rawMerchant = raw,
                cleanName = clean,
                categoryHint = null,
                source = source,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    private fun loadUpiHandles(): Map<String, String> {
        return try {
            val json = context.assets.open("upi_handles.json").bufferedReader().readText()
            val obj = Json.parseToJsonElement(json).jsonObject
            obj.entries.associate { (k, v) -> k.lowercase() to v.jsonPrimitive.content }
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
