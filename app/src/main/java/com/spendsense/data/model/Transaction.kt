package com.spendsense.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Maps to the `transactions` table. Every SMS-parsed transaction becomes one row.
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,         // UUID
    val amount: Double,
    val type: String,                   // "debit" | "credit"
    val merchant: String?,
    val category: String?,
    val account: String?,
    val timestamp: Long,                // Unix epoch ms
    val source: String,                 // always "sms"
    @ColumnInfo(name = "raw_text") val rawText: String
)
