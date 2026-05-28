package com.spendsense.agents

import com.spendsense.llm.LlmProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Assigns a spending category to each transaction.
 * Classification order:
 *   1. Keyword rule table (fast, offline)
 *   2. LLM call if no rule matches
 *   3. "Other" if LLM returns nothing
 *
 * User corrections are applied by updating the merchant_map.category_hint
 * and are picked up here on the next transaction from the same merchant.
 */
@Singleton
class ClassifierAgent @Inject constructor(
    private val llmProvider: LlmProvider
) {

    companion object {
        val CATEGORIES = listOf(
            "Food", "Transport", "Utilities", "Rent", "Shopping",
            "Medical", "Entertainment", "Savings", "Other"
        )
    }

    // keyword → category. Checked case-insensitively against the merchant name.
    private val KEYWORD_RULES: List<Pair<List<String>, String>> = listOf(
        // Food & dining
        listOf(
            "swiggy", "zomato", "food", "restaurant", "cafe", "kitchen", "eats",
            "domino", "pizza", "kfc", "mcdonald", "burger", "subway", "starbucks",
            "barista", "chaayos", "haldiram", "bikanervala", "biryani", "dhaba",
            "hotel", "tiffin", "mess", "canteen", "bakery", "sweet shop",
            "instamart", "zepto", "blinkit", "dunzo", "grofers", "bigbasket",
            "mother dairy", "amul", "d-mart", "dmart", "reliance fresh",
            "more supermarket", "big bazaar", "spencers"
        ) to "Food",

        // Transport
        listOf(
            "uber", "ola", "rapido", "cab", "taxi", "auto", "metro", "bus", "train",
            "irctc", "redbus", "makemytrip", "goibibo", "ixigo", "easemytrip",
            "petrol", "fuel", "shell", "bharat petroleum", "hp petrol",
            "indian oil", "iocl", "fastag", "toll", "parking"
        ) to "Transport",

        // Utilities
        listOf(
            "airtel", "jio", "bsnl", "vi ", "vodafone", "electricity", "bescom",
            "tsspdcl", "msedcl", "mahanagar gas", "igl", "adani gas", "gas bill",
            "water bill", "water tax", "broadband", "wifi", "internet", "cable tv",
            "dish tv", "tata sky", "d2h"
        ) to "Utilities",

        // Rent
        listOf("rent", "housing", "pg", "hostel", "apartment", "flat", "lease", "maintenance") to "Rent",

        // Shopping
        listOf(
            "amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "snapdeal",
            "shopsy", "puma", "adidas", "nike", "reebok", "bata", "liberty",
            "tanishq", "malabar", "kalyan", "croma", "reliance digital",
            "vijay sales", "poorvika", "shopping", "mart", "store", "shop",
            "fashion", "apparel", "clothing", "shoes", "jewellery", "ikea",
            "urban ladder", "pepperfry", "wakefit"
        ) to "Shopping",

        // Medical
        listOf(
            "pharmacy", "medical", "hospital", "clinic", "doctor", "apollo",
            "medplus", "netmeds", "pharmeasy", "1mg", "tata 1mg", "practo",
            "lybrate", "health", "diagnostic", "lab", "pathology", "dentist",
            "optician", "ayurveda", "homoeopathy"
        ) to "Medical",

        // Entertainment
        listOf(
            "netflix", "hotstar", "spotify", "prime video", "sonyliv", "zee5",
            "bookmyshow", "pvr", "inox", "cinepolis", "movie", "theatre",
            "concert", "event", "game", "gaming", "steam", "playstation",
            "xbox", "youtube premium", "gaana", "wynk", "jiosaavn"
        ) to "Entertainment",

        // Savings & investments
        listOf(
            "mutual fund", "sip", "zerodha", "groww", "upstox", "kuvera",
            "paytm money", "icicidirect", "hdfc securities", "sbi securities",
            "rd ", "fd ", "recurring deposit", "fixed deposit", "ppf", "nps",
            "insurance", "lic", "max life", "hdfc life", "bajaj allianz",
            "savings", "investment", "demat"
        ) to "Savings",
    )

    suspend fun classify(merchantName: String, categoryHint: String? = null): String {
        // User correction from DB takes highest priority
        if (!categoryHint.isNullOrBlank() && categoryHint in CATEGORIES) {
            return categoryHint
        }

        val lower = merchantName.lowercase()

        // Keyword rule table
        for ((keywords, category) in KEYWORD_RULES) {
            if (keywords.any { lower.contains(it) }) return category
        }

        // LLM fallback
        val fromLlm = classifyWithLlm(merchantName)
        if (fromLlm in CATEGORIES) return fromLlm

        return "Other"
    }

    private suspend fun classifyWithLlm(merchantName: String): String {
        val prompt = """Classify this Indian merchant into exactly one category.
Categories: Food, Transport, Utilities, Rent, Shopping, Medical, Entertainment, Savings, Other
Merchant: $merchantName
Category (single word only):"""

        return llmProvider.complete(prompt, maxTokens = 10).trim()
            .lines().firstOrNull()?.trim()?.replaceFirstChar { it.uppercase() } ?: "Other"
    }
}
