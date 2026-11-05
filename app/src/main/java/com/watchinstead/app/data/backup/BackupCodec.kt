package com.watchinstead.app.data.backup

import com.watchinstead.app.domain.Category
import com.watchinstead.app.domain.FocusPolicy
import com.watchinstead.app.domain.FocusSchedule
import com.watchinstead.app.domain.GuardSettings
import com.watchinstead.app.domain.LinkParser
import com.watchinstead.app.domain.OpenReason
import com.watchinstead.app.domain.PromptEvent
import com.watchinstead.app.domain.PromptOutcome
import com.watchinstead.app.domain.SavedLink
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BackupLink(
    val url: String,
    val category: String,
    val note: String = "",
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val createdAt: Long,
    val watchedAt: Long? = null,
)

@Serializable
data class BackupEvent(
    val timestamp: Long,
    val packageName: String,
    val outcome: String,
    val reason: String? = null,
)

@Serializable
data class BackupSettings(
    val guardedPackages: Set<String> = emptySet(),
    val cooldownMinutes: Int? = null,
    val skipDelaySeconds: Int? = null,
    val themeMode: String? = null,
    val dynamicColor: Boolean? = null,
    val accent: String? = null,
    val pureBlack: Boolean? = null,
    val askWhy: Boolean? = null,
    val personalMessages: Set<String> = emptySet(),
    val focusSchedules: List<FocusSchedule> = emptyList(),
)

@Serializable
data class BackupFile(
    val version: Int = BackupCodec.VERSION,
    val app: String = BackupCodec.APP_ID,
    val links: List<BackupLink> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
    val events: List<BackupEvent> = emptyList(),
)

/** Backups are user-editable files from outside the app, so nothing in them is trusted on import. */
object BackupCodec {
    const val VERSION = 3
    const val APP_ID = "watchinstead"
    private const val FIRST_RENAMED_VERSION = 3
    private const val MAX_NOTE_LENGTH = 200

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(file: BackupFile): String = json.encodeToString(file)

    /** Returns null for anything that is not a backup this version can read. */
    fun decode(text: String): BackupFile? {
        val file = try {
            json.decodeFromString<BackupFile>(text)
        } catch (_: SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        // Files from before the rename carry the previous app id, so only current ones are matched on it.
        val validApp = file.app == APP_ID || file.version < FIRST_RENAMED_VERSION
        return file.takeIf { validApp && it.version in 1..VERSION }
    }

    fun toBackup(link: SavedLink) = BackupLink(
        link.url, link.category.name, link.note, link.title, link.thumbnailUrl, link.createdAt, link.watchedAt,
    )

    fun toBackup(event: PromptEvent) =
        BackupEvent(event.timestamp, event.packageName, event.outcome.name, event.reason?.name)

    /** Re-parses the URL so that only http(s) links ever reach the system's link handling. */
    fun toSavedLink(link: BackupLink): SavedLink? {
        val parsed = LinkParser.parse(link.url) ?: return null
        return SavedLink(
            url = parsed.url,
            platform = parsed.platform,
            category = Category.entries.firstOrNull { it.name == link.category } ?: Category.OTHER,
            note = link.note.take(MAX_NOTE_LENGTH),
            title = link.title,
            thumbnailUrl = link.thumbnailUrl?.takeIf { it.startsWith("https://") },
            createdAt = link.createdAt,
            watchedAt = link.watchedAt,
        )
    }

    fun toPromptEvent(event: BackupEvent): PromptEvent? {
        val outcome = PromptOutcome.entries.firstOrNull { it.name == event.outcome } ?: return null
        return PromptEvent(
            event.timestamp,
            event.packageName,
            outcome,
            OpenReason.entries.firstOrNull { it.name == event.reason },
        )
    }

    fun sanitiseMessages(messages: Set<String>): Set<String> = messages
        .map { it.trim().take(GuardSettings.MAX_MESSAGE_LENGTH) }
        .filter { it.isNotEmpty() }
        .toSet()

    fun sanitiseSchedules(schedules: List<FocusSchedule>): List<FocusSchedule> =
        schedules.filter(FocusPolicy::isValid)
}
