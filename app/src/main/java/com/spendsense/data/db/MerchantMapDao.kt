package com.spendsense.data.db

import androidx.room.*
import com.spendsense.data.model.MerchantMap
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantMapDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MerchantMap)

    @Query("SELECT * FROM merchant_map WHERE raw_merchant = :rawMerchant LIMIT 1")
    suspend fun find(rawMerchant: String): MerchantMap?

    @Query("SELECT * FROM merchant_map ORDER BY created_at DESC")
    fun getAllFlow(): Flow<List<MerchantMap>>
}
