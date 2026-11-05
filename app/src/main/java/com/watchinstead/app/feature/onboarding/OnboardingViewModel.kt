package com.watchinstead.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchinstead.app.data.LinkRepository
import com.watchinstead.app.data.settings.SettingsRepository
import com.watchinstead.app.domain.MIN_ONBOARDING_LINKS
import com.watchinstead.app.domain.SavedLink
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class OnboardingStep { WELCOME, VIDEOS, APPS, PERMISSION }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val links: List<SavedLink> = emptyList(),
    val hasGuardedApps: Boolean = false,
) {
    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.WELCOME -> true
            OnboardingStep.VIDEOS -> links.size >= MIN_ONBOARDING_LINKS
            OnboardingStep.APPS -> hasGuardedApps
            OnboardingStep.PERMISSION -> false
        }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val links: LinkRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val step = MutableStateFlow(OnboardingStep.WELCOME)

    // Sharing from another app writes straight to the database, so this picks those videos up too.
    val state = combine(step, links.links, settings.settings) { step, links, guard ->
        OnboardingState(step, links, guard.guardedPackages.isNotEmpty())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, OnboardingState())

    fun next() {
        if (state.value.canContinue) step.value = OnboardingStep.entries[step.value.ordinal + 1]
    }

    /** Returns false on the first step, leaving the back press to the system. */
    fun back(): Boolean {
        if (step.value == OnboardingStep.WELCOME) return false
        step.value = OnboardingStep.entries[step.value.ordinal - 1]
        return true
    }

    fun remove(link: SavedLink) {
        viewModelScope.launch { links.delete(link.id) }
    }

    fun finish() {
        viewModelScope.launch {
            val hasLinks = links.links.first().size >= MIN_ONBOARDING_LINKS
            val hasApps = settings.settings.first().guardedPackages.isNotEmpty()
            if (hasLinks && hasApps) settings.setOnboardingComplete()
        }
    }
}
