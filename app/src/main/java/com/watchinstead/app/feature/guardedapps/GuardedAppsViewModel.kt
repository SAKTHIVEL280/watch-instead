package com.watchinstead.app.feature.guardedapps

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchinstead.app.data.apps.InstalledApp
import com.watchinstead.app.data.apps.InstalledAppsRepository
import com.watchinstead.app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GuardedAppsState(
    val isLoading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val selected: Set<String> = emptySet(),
)

@HiltViewModel
class GuardedAppsViewModel @Inject constructor(
    private val installedApps: InstalledAppsRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val apps = MutableStateFlow<List<InstalledApp>?>(null)

    val state = combine(apps, settings.settings) { apps, guard ->
        GuardedAppsState(isLoading = apps == null, apps = apps.orEmpty(), selected = guard.guardedPackages)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GuardedAppsState())

    init {
        viewModelScope.launch { apps.value = installedApps.load() }
    }

    fun toggle(packageName: String) {
        viewModelScope.launch {
            val current = settings.settings.first().guardedPackages
            settings.setGuardedPackages(
                if (packageName in current) current - packageName else current + packageName,
            )
        }
    }

    suspend fun icon(packageName: String): ImageBitmap? = installedApps.icon(packageName)
}
