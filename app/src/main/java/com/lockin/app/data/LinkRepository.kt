package com.lockin.app.data

import com.lockin.app.data.local.SavedLinkDao
import com.lockin.app.data.local.toDomain
import com.lockin.app.data.local.toEntity
import com.lockin.app.data.metadata.MetadataFetcher
import com.lockin.app.domain.Category
import com.lockin.app.domain.ParsedLink
import com.lockin.app.domain.QueueSelector
import com.lockin.app.domain.SavedLink
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class SaveResult { SAVED, DUPLICATE }

interface LinkRepository {
    val links: Flow<List<SavedLink>>
    suspend fun save(link: ParsedLink, category: Category, note: String): SaveResult
    suspend fun find(id: Long): SavedLink?

    /** Corrects a saved video. Reports DUPLICATE if the new URL belongs to a different video. */
    suspend fun edit(id: Long, link: ParsedLink, category: Category, note: String): SaveResult
    suspend fun restore(link: SavedLink)

    /** Returns false when the URL is already saved. */
    suspend fun importLink(link: SavedLink): Boolean
    suspend fun queue(): List<SavedLink>
    suspend fun markShown(id: Long)
    suspend fun setWatched(id: Long, watched: Boolean)
    suspend fun delete(id: Long)
    suspend fun clearWatched()
    suspend fun refreshMissingMetadata()
}

@Singleton
class DefaultLinkRepository @Inject constructor(
    private val dao: SavedLinkDao,
    private val fetcher: MetadataFetcher,
    @ApplicationScope private val appScope: CoroutineScope,
    private val clock: Clock,
) : LinkRepository {
    override val links = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun save(link: ParsedLink, category: Category, note: String): SaveResult {
        val entity = SavedLink(
            url = link.url,
            platform = link.platform,
            category = category,
            note = note.trim(),
            createdAt = clock.now(),
        ).toEntity()
        val id = dao.insert(entity)
        if (id == -1L) return SaveResult.DUPLICATE
        // Outlives the share sheet, which finishes as soon as the save returns.
        appScope.launch { refresh(id, link) }
        return SaveResult.SAVED
    }

    override suspend fun find(id: Long) = dao.find(id)?.toDomain()

    override suspend fun edit(id: Long, link: ParsedLink, category: Category, note: String): SaveResult {
        val owner = dao.findByUrl(link.url)
        if (owner != null && owner.id != id) return SaveResult.DUPLICATE
        dao.updateDetails(id, category, note.trim())
        if (owner == null) {
            dao.updateUrl(id, link.url, link.platform)
            appScope.launch { refresh(id, link) }
        }
        return SaveResult.SAVED
    }

    private suspend fun refresh(id: Long, link: ParsedLink) {
        val metadata = fetcher.fetch(link.url, link.platform)
        dao.updateMetadata(id, metadata?.title, metadata?.thumbnailUrl, clock.now())
    }

    override suspend fun restore(link: SavedLink) = dao.upsert(link.toEntity(clock.now()))

    override suspend fun importLink(link: SavedLink) = dao.insert(link.toEntity(clock.now())) != -1L

    override suspend fun queue() = QueueSelector.order(dao.unwatched().map { it.toDomain() })

    override suspend fun markShown(id: Long) = dao.setLastShownAt(id, clock.now())

    override suspend fun setWatched(id: Long, watched: Boolean) =
        dao.setWatchedAt(id, if (watched) clock.now() else null)

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun clearWatched() = dao.deleteWatched()

    override suspend fun refreshMissingMetadata() {
        dao.missingMetadata().forEach { refresh(it.id, ParsedLink(it.url, it.platform)) }
    }
}
