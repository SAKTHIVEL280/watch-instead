package com.watchinstead.app.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchinstead.app.data.settings.SettingsRepository
import com.watchinstead.app.domain.FocusPolicy
import com.watchinstead.app.domain.FocusSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class FocusViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val schedules = settings.settings.map { it.focusSchedules }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Adds or replaces by id. Invalid schedules are refused here as well as in the editor. */
    fun save(schedule: FocusSchedule) {
        if (!FocusPolicy.isValid(schedule)) return
        update { list ->
            if (list.any { it.id == schedule.id }) {
                list.map { if (it.id == schedule.id) schedule else it }
            } else {
                list + schedule
            }
        }
    }

    fun setEnabled(schedule: FocusSchedule, enabled: Boolean) =
        update { list -> list.map { if (it.id == schedule.id) it.copy(enabled = enabled) else it } }

    fun delete(schedule: FocusSchedule) = update { list -> list.filterNot { it.id == schedule.id } }

    private fun update(change: (List<FocusSchedule>) -> List<FocusSchedule>) {
        viewModelScope.launch { settings.setFocusSchedules(change(settings.settings.first().focusSchedules)) }
    }
}
