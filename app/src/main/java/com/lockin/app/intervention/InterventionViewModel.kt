package com.lockin.app.intervention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.ApplicationScope
import com.lockin.app.data.LinkRepository
import com.lockin.app.data.apps.InstalledAppsRepository
import com.lockin.app.data.settings.SettingsRepository
import com.lockin.app.domain.SavedLink
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InterventionState(
    val isLoading: Boolean = true,
    val appLabel: String? = null,
    val queue: List<SavedLink> = emptyList(),
    val index: Int = 0,
    val secondsLeft: Int = 0,
    val totalSeconds: Int = 0,
) {
    val current: SavedLink? get() = queue.getOrNull(index)
}

@HiltViewModel
class InterventionViewModel @Inject constructor(
    private val links: LinkRepository,
    private val settings: SettingsRepository,
    private val apps: InstalledAppsRepository,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {
    private val _state = MutableStateFlow(InterventionState())
    val state: StateFlow<InterventionState> = _state
    private var job: Job? = null
    private var guardedPackage: String? = null

    /** Safe to call repeatedly: late events for the app already on screen must not reset the timer. */
    fun start(packageName: String?) {
        if (packageName == guardedPackage && job != null) return
        guardedPackage = packageName
        job?.cancel()
        job = viewModelScope.launch {
            val delaySeconds = settings.settings.first().skipDelaySeconds
            val queue = links.queue()
            _state.value = InterventionState(
                isLoading = false,
                appLabel = packageName?.let(apps::label),
                queue = queue,
                secondsLeft = delaySeconds,
                totalSeconds = delaySeconds,
            )
            queue.firstOrNull()?.let { links.markShown(it.id) }
            repeat(delaySeconds) {
                delay(1_000)
                _state.update { it.copy(secondsLeft = it.secondsLeft - 1) }
            }
        }
    }

    fun showNext() {
        val queue = _state.value.queue
        if (queue.size < 2) return
        val next = (_state.value.index + 1) % queue.size
        _state.update { it.copy(index = next) }
        viewModelScope.launch { links.markShown(queue[next].id) }
    }

    suspend fun passForContinue() = settings.grantCooldownPass()

    /** Must complete before the link opens: it may open in the guarded app itself. */
    suspend fun passForWatching() = settings.grantCooldownPass()

    /** Outlives the activity, which finishes as soon as the video opens. */
    fun markWatched(link: SavedLink) {
        appScope.launch { links.setWatched(link.id, true) }
    }

    /** Covers the moment the guarded app can resurface while the launcher animates in. */
    suspend fun passForGoingHome() = settings.grantPass(GO_HOME_GRACE_MILLIS)

    private companion object {
        const val GO_HOME_GRACE_MILLIS = 3_000L
    }
}
