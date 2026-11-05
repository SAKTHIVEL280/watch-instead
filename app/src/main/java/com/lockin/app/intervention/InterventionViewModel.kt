package com.lockin.app.intervention

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.ApplicationScope
import com.lockin.app.data.Clock
import com.lockin.app.data.LinkRepository
import com.lockin.app.data.StatsRepository
import com.lockin.app.data.apps.InstalledAppsRepository
import com.lockin.app.data.settings.SettingsRepository
import com.lockin.app.domain.FocusPolicy
import com.lockin.app.domain.GuardSettings
import com.lockin.app.domain.InterventionPolicy
import com.lockin.app.domain.OpenReason
import com.lockin.app.domain.PromptOutcome
import com.lockin.app.domain.SavedLink
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
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
    val personalMessage: String? = null,
    val focusName: String? = null,
    val askWhy: Boolean = false,
    val askingWhy: Boolean = false,
    val reason: OpenReason? = null,
) {
    val current: SavedLink? get() = queue.getOrNull(index)
}

@HiltViewModel
class InterventionViewModel @Inject constructor(
    private val links: LinkRepository,
    private val settings: SettingsRepository,
    private val apps: InstalledAppsRepository,
    private val stats: StatsRepository,
    private val clock: Clock,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {
    private val _state = MutableStateFlow(InterventionState())
    val state: StateFlow<InterventionState> = _state
    private var job: Job? = null
    private var guardedPackage: String? = null
    private var recorded = false

    /** Safe to call repeatedly: late events for the app already on screen must not reset the timer. */
    fun start(packageName: String?) {
        if (packageName == guardedPackage && job != null) return
        guardedPackage = packageName
        recorded = false
        job?.cancel()
        job = viewModelScope.launch {
            val guard = settings.settings.first()
            val now = Instant.ofEpochMilli(clock.now()).atZone(ZoneId.systemDefault()).toLocalDateTime()
            val focus = FocusPolicy.activeSchedule(guard.focusSchedules, now)
            val delaySeconds = if (focus == null) {
                guard.skipDelaySeconds
            } else {
                maxOf(guard.skipDelaySeconds, GuardSettings.FOCUS_SKIP_DELAY_SECONDS)
            }
            val queue = links.queue()
            _state.value = InterventionState(
                isLoading = false,
                appLabel = packageName?.let(apps::label),
                queue = queue,
                secondsLeft = delaySeconds,
                totalSeconds = delaySeconds,
                personalMessage = guard.personalMessages.randomOrNull(),
                focusName = focus?.name,
                askWhy = guard.askWhy,
            )
            queue.firstOrNull()?.let { links.markShown(it.id) }
            repeat(delaySeconds) {
                delay(1_000)
                _state.update { it.copy(secondsLeft = it.secondsLeft - 1) }
            }
        }
    }

    /** Returns true when the reason step has to come first, so the prompt itself stays uncluttered. */
    fun askWhyBeforeContinuing(): Boolean {
        val ask = _state.value.askWhy
        if (ask) _state.update { it.copy(askingWhy = true) }
        return ask
    }

    fun selectReason(reason: OpenReason) {
        _state.update { it.copy(reason = reason) }
    }

    /** Called once per prompt, however it ends. Outlives the activity, which is about to finish. */
    fun record(outcome: PromptOutcome) {
        val packageName = guardedPackage ?: return
        if (recorded) return
        recorded = true
        val reason = _state.value.reason
        appScope.launch { stats.record(packageName, outcome, reason) }
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

    suspend fun passForGoingHome() = settings.grantPass(InterventionPolicy.GO_HOME_GRACE_MILLIS)
}
