package com.watchinstead.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchinstead.app.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    /** Null until the stored value is known; the splash screen stays up until then. */
    val onboardingComplete: StateFlow<Boolean?> = settings.onboardingComplete
        .map<Boolean, Boolean?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
