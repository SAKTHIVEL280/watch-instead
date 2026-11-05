package com.watchinstead.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.watchinstead.app.domain.Category
import com.watchinstead.app.domain.Platform
import com.watchinstead.app.domain.SavedLink

@Entity(tableName = "saved_links", indices = [Index(value = ["url"], unique = true)])
data class SavedLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val platform: Platform,
    val category: Category,
    val note: String,
    val title: String?,
    val thumbnailUrl: String?,
    val createdAt: Long,
    val watchedAt: Long?,
    val lastShownAt: Long?,
    val metadataAttemptedAt: Long?,
)

fun SavedLinkEntity.toDomain() = SavedLink(
    id, url, platform, category, note, title, thumbnailUrl, createdAt, watchedAt, lastShownAt,
)

fun SavedLink.toEntity(metadataAttemptedAt: Long? = null) = SavedLinkEntity(
    id, url, platform, category, note, title, thumbnailUrl, createdAt, watchedAt, lastShownAt,
    metadataAttemptedAt,
)
