package com.lockin.app.domain

enum class Category { MOTIVATION, EDUCATION, CONSISTENCY, PRODUCTIVITY, OTHER }

enum class Platform { YOUTUBE, INSTAGRAM, TIKTOK, X, OTHER }

data class SavedLink(
    val id: Long = 0,
    val url: String,
    val platform: Platform,
    val category: Category,
    val note: String = "",
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val createdAt: Long,
    val watchedAt: Long? = null,
    val lastShownAt: Long? = null,
)

data class ParsedLink(val url: String, val platform: Platform)

data class GuardSettings(
    val protectionEnabled: Boolean = true,
    val guardedPackages: Set<String> = emptySet(),
    val cooldownMinutes: Int = DEFAULT_COOLDOWN_MINUTES,
    val skipDelaySeconds: Int = DEFAULT_SKIP_DELAY_SECONDS,
    val passUntil: Long = 0,
) {
    companion object {
        const val DEFAULT_COOLDOWN_MINUTES = 15
        const val DEFAULT_SKIP_DELAY_SECONDS = 5
        val COOLDOWN_OPTIONS = listOf(5, 15, 30, 60)
        val SKIP_DELAY_OPTIONS = listOf(3, 5, 10, 20)
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class Accent { INDIGO, ROSE, TEAL, AMBER }

data class Appearance(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val accent: Accent = Accent.INDIGO,
    val pureBlack: Boolean = false,
)

/** Saved videos required before onboarding can finish, so the first prompt is never empty. */
const val MIN_ONBOARDING_LINKS = 2
const val SUGGESTED_ONBOARDING_LINKS = 3
