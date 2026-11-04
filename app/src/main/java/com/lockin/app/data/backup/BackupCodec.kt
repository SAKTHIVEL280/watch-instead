package com.lockin.app.data.backup

import com.lockin.app.domain.Category
import com.lockin.app.domain.LinkParser
import com.lockin.app.domain.SavedLink
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
data class BackupSettings(
    val guardedPackages: Set<String> = emptySet(),
    val cooldownMinutes: Int? = null,
    val skipDelaySeconds: Int? = null,
    val themeMode: String? = null,
    val dynamicColor: Boolean? = null,
    val accent: String? = null,
    val pureBlack: Boolean? = null,
)

@Serializable
data class BackupFile(
    val version: Int = BackupCodec.VERSION,
    val app: String = BackupCodec.APP_ID,
    val links: List<BackupLink> = emptyList(),
    val settings: BackupSettings = BackupSettings(),
)

/** Backups are user-editable files from outside the app, so nothing in them is trusted on import. */
object BackupCodec {
    const val VERSION = 1
    const val APP_ID = "lockin"
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
        return file.takeIf { it.app == APP_ID && it.version in 1..VERSION }
    }

    fun toBackup(link: SavedLink) = BackupLink(
        link.url, link.category.name, link.note, link.title, link.thumbnailUrl, link.createdAt, link.watchedAt,
    )

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
}
