package com.watchinstead.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.watchinstead.app.domain.Category
import com.watchinstead.app.domain.Platform
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLinkDao {
    @Query("SELECT * FROM saved_links ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<SavedLinkEntity>>

    @Query("SELECT * FROM saved_links WHERE watchedAt IS NULL")
    suspend fun unwatched(): List<SavedLinkEntity>

    @Query("SELECT * FROM saved_links WHERE metadataAttemptedAt IS NULL")
    suspend fun missingMetadata(): List<SavedLinkEntity>

    @Query("SELECT * FROM saved_links WHERE id = :id")
    suspend fun find(id: Long): SavedLinkEntity?

    @Query("SELECT * FROM saved_links WHERE url = :url")
    suspend fun findByUrl(url: String): SavedLinkEntity?

    @Query("UPDATE saved_links SET category = :category, note = :note WHERE id = :id")
    suspend fun updateDetails(id: Long, category: Category, note: String)

    /** The old title and thumbnail describe the old video, so they are cleared for a fresh fetch. */
    @Query(
        "UPDATE saved_links SET url = :url, platform = :platform, title = NULL, thumbnailUrl = NULL, " +
            "metadataAttemptedAt = NULL WHERE id = :id",
    )
    suspend fun updateUrl(id: Long, url: String, platform: Platform)

    /** Returns -1 when the URL is already saved. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: SavedLinkEntity): Long

    @Upsert
    suspend fun upsert(link: SavedLinkEntity)

    @Query("UPDATE saved_links SET title = :title, thumbnailUrl = :thumbnailUrl, metadataAttemptedAt = :attemptedAt WHERE id = :id")
    suspend fun updateMetadata(id: Long, title: String?, thumbnailUrl: String?, attemptedAt: Long)

    @Query("UPDATE saved_links SET watchedAt = :watchedAt WHERE id = :id")
    suspend fun setWatchedAt(id: Long, watchedAt: Long?)

    @Query("UPDATE saved_links SET lastShownAt = :shownAt WHERE id = :id")
    suspend fun setLastShownAt(id: Long, shownAt: Long)

    @Query("DELETE FROM saved_links WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM saved_links WHERE watchedAt IS NOT NULL")
    suspend fun deleteWatched()
}
