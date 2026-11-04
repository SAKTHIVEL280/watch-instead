package com.lockin.app.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.LinkRepository
import com.lockin.app.data.SaveResult
import com.lockin.app.domain.Category
import com.lockin.app.domain.LinkParser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CaptureState(
    val input: String = "",
    val note: String = "",
    val category: Category = Category.MOTIVATION,
    val showInvalid: Boolean = false,
    val isDuplicate: Boolean = false,
    val isSaved: Boolean = false,
)

@HiltViewModel
class CaptureViewModel @Inject constructor(private val links: LinkRepository) : ViewModel() {
    private val _state = MutableStateFlow(CaptureState())
    val state: StateFlow<CaptureState> = _state

    fun onInputChange(value: String) =
        _state.update { it.copy(input = value, showInvalid = false, isDuplicate = false) }

    fun onNoteChange(value: String) = _state.update { it.copy(note = value.take(MAX_NOTE_LENGTH)) }

    fun onCategoryChange(value: Category) = _state.update { it.copy(category = value) }

    /** Clears the form so the same sheet can capture several videos in a row. */
    fun reset() {
        _state.value = CaptureState()
    }

    fun save() {
        val current = _state.value
        if (current.isSaved) return
        val parsed = LinkParser.parse(current.input)
        if (parsed == null) {
            _state.update { it.copy(showInvalid = true) }
            return
        }
        viewModelScope.launch {
            when (links.save(parsed, current.category, current.note)) {
                SaveResult.SAVED -> _state.update { it.copy(isSaved = true) }
                SaveResult.DUPLICATE -> _state.update { it.copy(isDuplicate = true) }
            }
        }
    }

    private companion object {
        const val MAX_NOTE_LENGTH = 200
    }
}
