package com.lockin.app.data.backup

import com.lockin.app.data.LinkRepository
import com.lockin.app.data.StatsRepository
import com.lockin.app.data.settings.SettingsRepository
import com.lockin.app.domain.Accent
import com.lockin.app.domain.GuardSettings
import com.lockin.app.domain.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

data class ImportResult(val added: Int, val skipped: Int)

@Singleton
class BackupRepository @Inject constructor(
    private val links: LinkRepository,
    private val settings: SettingsRepository,
    private val stats: StatsRepository,
) {
    suspend fun export(): String {
        val guard = settings.settings.first()
        val appearance = settings.appearance.first()
        return BackupCodec.encode(
            BackupFile(
                links = links.links.first().map(BackupCodec::toBackup),
                settings = BackupSettings(
                    guardedPackages = guard.guardedPackages,
                    cooldownMinutes = guard.cooldownMinutes,
                    skipDelaySeconds = guard.skipDelaySeconds,
                    themeMode = appearance.themeMode.name,
                    dynamicColor = appearance.dynamicColor,
                    accent = appearance.accent.name,
                    pureBlack = appearance.pureBlack,
                    askWhy = guard.askWhy,
                    personalMessages = guard.personalMessages,
                    focusSchedules = guard.focusSchedules,
                ),
                events = stats.events.first().map(BackupCodec::toBackup),
            ),
        )
    }

    /** Merges into what is already here: existing videos are kept. Null means the file was unreadable. */
    suspend fun import(text: String): ImportResult? {
        val file = BackupCodec.decode(text) ?: return null
        val imported = file.links.mapNotNull(BackupCodec::toSavedLink).count { links.importLink(it) }
        file.events.mapNotNull(BackupCodec::toPromptEvent).forEach { stats.importEvent(it) }
        apply(file.settings)
        return ImportResult(imported, file.links.size - imported)
    }

    private suspend fun apply(backup: BackupSettings) {
        val current = settings.settings.first()
        settings.setGuardedPackages(current.guardedPackages + backup.guardedPackages)
        settings.setPersonalMessages(
            (current.personalMessages + BackupCodec.sanitiseMessages(backup.personalMessages))
                .take(GuardSettings.MAX_MESSAGES).toSet(),
        )
        val knownIds = current.focusSchedules.map { it.id }
        settings.setFocusSchedules(
            current.focusSchedules +
                BackupCodec.sanitiseSchedules(backup.focusSchedules).filter { it.id !in knownIds },
        )
        backup.askWhy?.let { settings.setAskWhy(it) }
        backup.cooldownMinutes?.takeIf { it in GuardSettings.COOLDOWN_OPTIONS }?.let { settings.setCooldownMinutes(it) }
        backup.skipDelaySeconds?.takeIf { it in GuardSettings.SKIP_DELAY_OPTIONS }?.let { settings.setSkipDelaySeconds(it) }
        ThemeMode.entries.firstOrNull { it.name == backup.themeMode }?.let { settings.setThemeMode(it) }
        backup.dynamicColor?.let { settings.setDynamicColor(it) }
        Accent.entries.firstOrNull { it.name == backup.accent }?.let { settings.setAccent(it) }
        backup.pureBlack?.let { settings.setPureBlack(it) }
    }
}
