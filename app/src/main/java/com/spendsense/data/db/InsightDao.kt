package com.spendsense.data.db

import androidx.room.*
import com.spendsense.data.model.WeeklyInsight
import kotlinx.coroutines.flow.Flow

@Dao
interface InsightDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(insight: WeeklyInsight)

    @Query("SELECT * FROM weekly_insights ORDER BY week_start DESC")
    fun getAllFlow(): Flow<List<WeeklyInsight>>

    @Query("SELECT * FROM weekly_insights ORDER BY week_start DESC LIMIT 1")
    suspend fun getLatest(): WeeklyInsight?
}
