package com.lockin.app.feature.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.settings.SettingsRepository
import com.lockin.app.domain.GuardSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MessagesViewModel @Inject constructor(private val settings: SettingsRepository) : ViewModel() {
    val messages = settings.settings.map { it.personalMessages.sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Adds [text], or replaces [original] when editing. Blank text is ignored. */
    fun save(original: String?, text: String) {
        val message = text.trim().take(GuardSettings.MAX_MESSAGE_LENGTH)
        if (message.isEmpty()) return
        viewModelScope.launch {
            val current = settings.settings.first().personalMessages
            val isNew = original == null && message !in current
            if (isNew && current.size >= GuardSettings.MAX_MESSAGES) return@launch
            settings.setPersonalMessages(current - setOfNotNull(original) + message)
        }
    }

    fun delete(message: String) {
        viewModelScope.launch {
            settings.setPersonalMessages(settings.settings.first().personalMessages - message)
        }
    }
}
