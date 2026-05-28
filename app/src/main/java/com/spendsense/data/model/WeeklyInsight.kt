package com.spendsense.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Stores the LLM-generated weekly summary text, one row per week.
@Entity(tableName = "weekly_insights")
data class WeeklyInsight(
    @PrimaryKey val id: String,    // UUID
    @ColumnInfo(name = "week_start") val weekStart: Long,       // Unix epoch ms, Monday 00:00
    @ColumnInfo(name = "summary_text") val summaryText: String,
    @ColumnInfo(name = "generated_at") val generatedAt: Long    // Unix epoch ms
)
