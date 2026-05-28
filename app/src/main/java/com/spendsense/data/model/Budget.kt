package com.spendsense.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// One row per spending category (Food, Rent, etc.). Tracks the monthly limit and running spend.
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: String,
    @ColumnInfo(name = "monthly_limit") val monthlyLimit: Double,
    @ColumnInfo(name = "current_spend") val currentSpend: Double = 0.0
)
