package com.watchinstead.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import com.watchinstead.app.domain.OpenReason
import com.watchinstead.app.domain.PromptEvent
import com.watchinstead.app.domain.PromptOutcome
import kotlinx.coroutines.flow.Flow

/** The unique index is what stops a backup imported twice from doubling the stats. */
@Entity(tableName = "prompt_events", indices = [Index(value = ["timestamp", "packageName"], unique = true)])
data class PromptEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val packageName: String,
    val outcome: PromptOutcome,
    val reason: OpenReason?,
)

fun PromptEventEntity.toDomain() = PromptEvent(timestamp, packageName, outcome, reason)

fun PromptEvent.toEntity() = PromptEventEntity(
    timestamp = timestamp, packageName = packageName, outcome = outcome, reason = reason,
)

@Dao
interface PromptEventDao {
    @Query("SELECT * FROM prompt_events ORDER BY timestamp")
    fun observeAll(): Flow<List<PromptEventEntity>>

    /** Returns -1 when the event already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(event: PromptEventEntity): Long

    @Query("DELETE FROM prompt_events")
    suspend fun clear()
}
