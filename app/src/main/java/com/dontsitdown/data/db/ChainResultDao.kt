package com.dontsitdown.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dontsitdown.data.model.ChainResult
import com.dontsitdown.data.model.ReminderEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ChainResultDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(result: ChainResult)

    @Query("SELECT * FROM chain_results ORDER BY date DESC")
    fun observeAll(): Flow<List<ChainResult>>

    @Query("SELECT * FROM chain_results ORDER BY date DESC")
    suspend fun getAll(): List<ChainResult>

    @Query("SELECT * FROM chain_results WHERE date >= :from AND date < :to ORDER BY date")
    suspend fun between(from: Long, to: Long): List<ChainResult>

    @Insert
    suspend fun insertEvent(event: ReminderEvent)

    @Query("SELECT * FROM reminder_events WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp")
    suspend fun eventsBetween(from: Long, to: Long): List<ReminderEvent>
}
