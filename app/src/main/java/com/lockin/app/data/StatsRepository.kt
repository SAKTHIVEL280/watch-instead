package com.lockin.app.data

import com.lockin.app.data.local.PromptEventDao
import com.lockin.app.data.local.toDomain
import com.lockin.app.data.local.toEntity
import com.lockin.app.domain.OpenReason
import com.lockin.app.domain.PromptEvent
import com.lockin.app.domain.PromptOutcome
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface StatsRepository {
    val events: Flow<List<PromptEvent>>
    suspend fun record(packageName: String, outcome: PromptOutcome, reason: OpenReason?)

    /** Returns false when the event is already stored. */
    suspend fun importEvent(event: PromptEvent): Boolean
    suspend fun clear()
}

@Singleton
class DefaultStatsRepository @Inject constructor(
    private val dao: PromptEventDao,
    private val clock: Clock,
) : StatsRepository {
    override val events = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun record(packageName: String, outcome: PromptOutcome, reason: OpenReason?) {
        dao.insert(PromptEvent(clock.now(), packageName, outcome, reason).toEntity())
    }

    override suspend fun importEvent(event: PromptEvent) = dao.insert(event.toEntity()) != -1L

    override suspend fun clear() = dao.clear()
}
