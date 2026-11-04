package com.lockin.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.lockin.app.data.Clock
import com.lockin.app.domain.Accent
import com.lockin.app.domain.Appearance
import com.lockin.app.domain.GuardSettings
import com.lockin.app.domain.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface SettingsRepository {
    val settings: Flow<GuardSettings>
    val onboardingComplete: Flow<Boolean>
    val appearance: Flow<Appearance>
    suspend fun setOnboardingComplete()
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setAccent(accent: Accent)
    suspend fun setPureBlack(enabled: Boolean)
    suspend fun setProtectionEnabled(enabled: Boolean)
    suspend fun setGuardedPackages(packages: Set<String>)
    suspend fun setCooldownMinutes(minutes: Int)
    suspend fun setSkipDelaySeconds(seconds: Int)

    /**
     * Lets guarded apps open without a prompt for the configured cooldown. With no cooldown this
     * lasts until [endSession], when the user leaves for the home screen or locks the phone.
     */
    suspend fun grantCooldownPass()

    suspend fun endSession()

    /** Suppresses prompts for [millis], never shortening a pass that is already running. */
    suspend fun grantPass(millis: Long)
}

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>,
    private val clock: Clock,
) : SettingsRepository {
    override val settings = store.data.map { prefs ->
        GuardSettings(
            protectionEnabled = prefs[PROTECTION] ?: true,
            guardedPackages = prefs[GUARDED].orEmpty(),
            cooldownMinutes = prefs[COOLDOWN] ?: GuardSettings.DEFAULT_COOLDOWN_MINUTES,
            skipDelaySeconds = prefs[SKIP_DELAY] ?: GuardSettings.DEFAULT_SKIP_DELAY_SECONDS,
            passUntil = prefs[PASS_UNTIL] ?: 0,
            sessionOpen = prefs[SESSION_OPEN] ?: false,
        )
    }.distinctUntilChanged()

    override val onboardingComplete: Flow<Boolean> =
        store.data.map { it[ONBOARDED] ?: false }.distinctUntilChanged()

    override val appearance = store.data.map { prefs ->
        Appearance(
            themeMode = enumOrDefault(prefs[THEME_MODE], ThemeMode.SYSTEM),
            dynamicColor = prefs[DYNAMIC_COLOR] ?: true,
            accent = enumOrDefault(prefs[ACCENT], Accent.INDIGO),
            pureBlack = prefs[PURE_BLACK] ?: false,
        )
    }.distinctUntilChanged()

    override suspend fun setOnboardingComplete() = set(ONBOARDED, true)
    override suspend fun setThemeMode(mode: ThemeMode) = set(THEME_MODE, mode.name)
    override suspend fun setDynamicColor(enabled: Boolean) = set(DYNAMIC_COLOR, enabled)
    override suspend fun setAccent(accent: Accent) = set(ACCENT, accent.name)
    override suspend fun setPureBlack(enabled: Boolean) = set(PURE_BLACK, enabled)
    override suspend fun setProtectionEnabled(enabled: Boolean) = set(PROTECTION, enabled)
    override suspend fun setGuardedPackages(packages: Set<String>) = set(GUARDED, packages)
    override suspend fun setCooldownMinutes(minutes: Int) = set(COOLDOWN, minutes)
    override suspend fun setSkipDelaySeconds(seconds: Int) = set(SKIP_DELAY, seconds)

    override suspend fun grantCooldownPass() {
        store.edit { prefs ->
            val minutes = prefs[COOLDOWN] ?: GuardSettings.DEFAULT_COOLDOWN_MINUTES
            if (minutes == GuardSettings.NO_COOLDOWN) {
                prefs[SESSION_OPEN] = true
            } else {
                extendPass(prefs, minutes * 60_000L)
            }
        }
    }

    override suspend fun endSession() = set(SESSION_OPEN, false)

    override suspend fun grantPass(millis: Long) {
        store.edit { prefs -> extendPass(prefs, millis) }
    }

    private fun extendPass(prefs: androidx.datastore.preferences.core.MutablePreferences, millis: Long) {
        prefs[PASS_UNTIL] = maxOf(prefs[PASS_UNTIL] ?: 0, clock.now() + millis)
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        store.edit { it[key] = value }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ACCENT = stringPreferencesKey("accent")
        val PURE_BLACK = booleanPreferencesKey("pure_black")
        val ONBOARDED = booleanPreferencesKey("onboarding_complete")
        val PROTECTION = booleanPreferencesKey("protection_enabled")
        val GUARDED = stringSetPreferencesKey("guarded_packages")
        val COOLDOWN = intPreferencesKey("cooldown_minutes")
        val SKIP_DELAY = intPreferencesKey("skip_delay_seconds")
        val PASS_UNTIL = longPreferencesKey("pass_until")
        val SESSION_OPEN = booleanPreferencesKey("session_open")
    }
}
