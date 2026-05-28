package com.spendsense.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Cache: raw_merchant string → clean display name. Prevents repeat LLM calls for known merchants.
@Entity(tableName = "merchant_map")
data class MerchantMap(
    @PrimaryKey @ColumnInfo(name = "raw_merchant") val rawMerchant: String,
    @ColumnInfo(name = "clean_name") val cleanName: String,
    @ColumnInfo(name = "category_hint") val categoryHint: String?,
    val source: String,    // "rule" | "llm" | "user"
    @ColumnInfo(name = "created_at") val createdAt: Long    // Unix epoch ms
)
