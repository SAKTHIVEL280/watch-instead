package com.watchinstead.app.testing

import com.watchinstead.app.data.LinkRepository
import com.watchinstead.app.data.SaveResult
import com.watchinstead.app.data.StatsRepository
import com.watchinstead.app.data.settings.SettingsRepository
import com.watchinstead.app.domain.Accent
import com.watchinstead.app.domain.Appearance
import com.watchinstead.app.domain.Category
import com.watchinstead.app.domain.FocusSchedule
import com.watchinstead.app.domain.GuardSettings
import com.watchinstead.app.domain.OpenReason
import com.watchinstead.app.domain.PromptEvent
import com.watchinstead.app.domain.PromptOutcome
import com.watchinstead.app.domain.ParsedLink
import com.watchinstead.app.domain.QueueSelector
import com.watchinstead.app.domain.SavedLink
import com.watchinstead.app.domain.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeLinkRepository : LinkRepository {
    override val links = MutableStateFlow<List<SavedLink>>(emptyList())
    private var nextId = 1L

    override suspend fun save(link: ParsedLink, category: Category, note: String): SaveResult {
        if (links.value.any { it.url == link.url }) return SaveResult.DUPLICATE
        links.update {
            it + SavedLink(nextId++, link.url, link.platform, category, note, createdAt = nextId)
        }
        return SaveResult.SAVED
    }

    override suspend fun find(id: Long) = links.value.firstOrNull { it.id == id }
    override suspend fun edit(id: Long, link: ParsedLink, category: Category, note: String): SaveResult {
        if (links.value.any { it.url == link.url && it.id != id }) return SaveResult.DUPLICATE
        links.update { all ->
            all.map {
                if (it.id == id) it.copy(url = link.url, platform = link.platform, category = category, note = note.trim()) else it
            }
        }
        return SaveResult.SAVED
    }
    override suspend fun restore(link: SavedLink) = links.update { it + link }
    override suspend fun importLink(link: SavedLink): Boolean {
        if (links.value.any { it.url == link.url }) return false
        links.update { it + link.copy(id = nextId++) }
        return true
    }
    override suspend fun queue() = QueueSelector.order(links.value)
    override suspend fun markShown(id: Long) = Unit
    override suspend fun setWatched(id: Long, watched: Boolean) = links.update { all ->
        all.map { if (it.id == id) it.copy(watchedAt = if (watched) 1 else null) else it }
    }
    override suspend fun delete(id: Long) = links.update { all -> all.filterNot { it.id == id } }
    override suspend fun clearWatched() = links.update { all -> all.filter { it.watchedAt == null } }
    override suspend fun refreshMissingMetadata() = Unit
}

class FakeStatsRepository : StatsRepository {
    override val events = MutableStateFlow<List<PromptEvent>>(emptyList())
    override suspend fun record(packageName: String, outcome: PromptOutcome, reason: OpenReason?) =
        events.update { it + PromptEvent(it.size.toLong(), packageName, outcome, reason) }
    override suspend fun importEvent(event: PromptEvent): Boolean {
        if (events.value.any { it.timestamp == event.timestamp && it.packageName == event.packageName }) return false
        events.update { it + event }
        return true
    }
    override suspend fun clear() { events.value = emptyList() }
}

class FakeSettingsRepository : SettingsRepository {
    override val settings = MutableStateFlow(GuardSettings())
    override val onboardingComplete = MutableStateFlow(false)
    override val appearance = MutableStateFlow(Appearance())
    var cooldownPasses = 0

    override suspend fun setThemeMode(mode: ThemeMode) = appearance.update { it.copy(themeMode = mode) }
    override suspend fun setDynamicColor(enabled: Boolean) = appearance.update { it.copy(dynamicColor = enabled) }
    override suspend fun setAccent(accent: Accent) = appearance.update { it.copy(accent = accent) }
    override suspend fun setPureBlack(enabled: Boolean) = appearance.update { it.copy(pureBlack = enabled) }

    override suspend fun setOnboardingComplete() { onboardingComplete.value = true }
    override suspend fun setProtectionEnabled(enabled: Boolean) =
        settings.update { it.copy(protectionEnabled = enabled) }
    override suspend fun setGuardedPackages(packages: Set<String>) =
        settings.update { it.copy(guardedPackages = packages) }
    override suspend fun setCooldownMinutes(minutes: Int) = settings.update { it.copy(cooldownMinutes = minutes) }
    override suspend fun setSkipDelaySeconds(seconds: Int) = settings.update { it.copy(skipDelaySeconds = seconds) }
    override suspend fun setAskWhy(enabled: Boolean) = settings.update { it.copy(askWhy = enabled) }
    override suspend fun setPersonalMessages(messages: Set<String>) = settings.update { it.copy(personalMessages = messages) }
    override suspend fun setFocusSchedules(schedules: List<FocusSchedule>) = settings.update { it.copy(focusSchedules = schedules) }
    override suspend fun grantCooldownPass() { cooldownPasses++ }
    override suspend fun endSession() = settings.update { it.copy(sessionOpen = false) }
    override suspend fun grantPass(millis: Long) = Unit
}
