package com.lockin.app.domain

import kotlinx.serialization.Serializable

enum class Category { MOTIVATION, EDUCATION, CONSISTENCY, PRODUCTIVITY, OTHER }

/** Why the user says they opened a guarded app. Stored by name, so never rename a value. */
enum class OpenReason { BORED, HABIT, STRESSED, NEED_SOMETHING }

/** How a prompt ended. Stored by name, so never rename a value. */
enum class PromptOutcome { WATCHED, WENT_HOME, CONTINUED }

data class PromptEvent(
    val timestamp: Long,
    val packageName: String,
    val outcome: PromptOutcome,
    val reason: OpenReason? = null,
)

/** Hours in which every open is prompted, whatever the cooldown. Days use ISO numbering, 1 is Monday. */
@Serializable
data class FocusSchedule(
    val id: String,
    val name: String,
    val days: Set<Int>,
    val startMinute: Int,
    val endMinute: Int,
    val enabled: Boolean = true,
)

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
    val sessionOpen: Boolean = false,
    val focusSchedules: List<FocusSchedule> = emptyList(),
    val personalMessages: Set<String> = emptySet(),
    val askWhy: Boolean = true,
) {
    companion object {
        const val DEFAULT_COOLDOWN_MINUTES = 15
        const val DEFAULT_SKIP_DELAY_SECONDS = 5
        const val NO_COOLDOWN = 0
        const val FOCUS_SKIP_DELAY_SECONDS = 30
        const val MAX_MESSAGE_LENGTH = 120
        const val MAX_MESSAGES = 20
        const val MAX_SCHEDULE_NAME_LENGTH = 30
        val COOLDOWN_OPTIONS = listOf(NO_COOLDOWN, 5, 15, 30, 60)
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
