package com.lockin.app.testing

import com.lockin.app.data.LinkRepository
import com.lockin.app.data.SaveResult
import com.lockin.app.data.settings.SettingsRepository
import com.lockin.app.domain.Accent
import com.lockin.app.domain.Appearance
import com.lockin.app.domain.Category
import com.lockin.app.domain.GuardSettings
import com.lockin.app.domain.ParsedLink
import com.lockin.app.domain.QueueSelector
import com.lockin.app.domain.SavedLink
import com.lockin.app.domain.ThemeMode
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

    override suspend fun restore(link: SavedLink) = links.update { it + link }
    override suspend fun queue() = QueueSelector.order(links.value)
    override suspend fun markShown(id: Long) = Unit
    override suspend fun setWatched(id: Long, watched: Boolean) = links.update { all ->
        all.map { if (it.id == id) it.copy(watchedAt = if (watched) 1 else null) else it }
    }
    override suspend fun delete(id: Long) = links.update { all -> all.filterNot { it.id == id } }
    override suspend fun clearWatched() = links.update { all -> all.filter { it.watchedAt == null } }
    override suspend fun refreshMissingMetadata() = Unit
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
    override suspend fun grantCooldownPass() { cooldownPasses++ }
    override suspend fun grantPass(millis: Long) = Unit
}
