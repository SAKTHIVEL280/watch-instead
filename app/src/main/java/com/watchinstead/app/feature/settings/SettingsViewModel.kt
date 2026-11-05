package com.watchinstead.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchinstead.app.data.LinkRepository
import com.watchinstead.app.data.StatsRepository
import com.watchinstead.app.data.settings.SettingsRepository
import com.watchinstead.app.domain.Accent
import com.watchinstead.app.domain.Appearance
import com.watchinstead.app.domain.GuardSettings
import com.watchinstead.app.domain.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val links: LinkRepository,
    private val stats: StatsRepository,
) : ViewModel() {
    val state = settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GuardSettings())

    val appearance = settings.appearance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Appearance())

    fun setThemeMode(mode: ThemeMode) = launch { settings.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = launch { settings.setDynamicColor(enabled) }
    fun setAccent(accent: Accent) = launch { settings.setAccent(accent) }
    fun setPureBlack(enabled: Boolean) = launch { settings.setPureBlack(enabled) }
    fun setProtection(enabled: Boolean) = launch { settings.setProtectionEnabled(enabled) }
    fun setCooldown(minutes: Int) = launch { settings.setCooldownMinutes(minutes) }
    fun setSkipDelay(seconds: Int) = launch { settings.setSkipDelaySeconds(seconds) }
    fun clearWatched() = launch { links.clearWatched() }
    fun clearStats() = launch { stats.clear() }
    fun setAskWhy(enabled: Boolean) = launch { settings.setAskWhy(enabled) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
