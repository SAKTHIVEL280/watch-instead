package com.lockin.app.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lockin.app.data.LinkRepository
import com.lockin.app.data.settings.SettingsRepository
import com.lockin.app.domain.Category
import com.lockin.app.domain.QueueSelector
import com.lockin.app.domain.SavedLink
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab { QUEUE, WATCHED }

data class LibraryState(
    val tab: LibraryTab = LibraryTab.QUEUE,
    val filter: Category? = null,
    val links: List<SavedLink> = emptyList(),
    val noAppsGuarded: Boolean = false,
    val isLoading: Boolean = true,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val links: LinkRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val tab = MutableStateFlow(LibraryTab.QUEUE)
    private val filter = MutableStateFlow<Category?>(null)

    val state = combine(links.links, tab, filter, settings.settings) { all, tab, filter, guard ->
        val inTab = when (tab) {
            LibraryTab.QUEUE -> QueueSelector.order(all)
            LibraryTab.WATCHED -> all.filter { it.watchedAt != null }.sortedByDescending { it.watchedAt }
        }
        LibraryState(
            tab = tab,
            filter = filter,
            links = inTab.filter { filter == null || it.category == filter },
            noAppsGuarded = guard.guardedPackages.isEmpty(),
            isLoading = false,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryState())

    init {
        viewModelScope.launch { links.refreshMissingMetadata() }
    }

    fun selectTab(value: LibraryTab) {
        tab.value = value
    }

    fun selectFilter(value: Category?) {
        filter.value = value
    }

    fun setWatched(link: SavedLink, watched: Boolean) {
        viewModelScope.launch { links.setWatched(link.id, watched) }
    }

    fun delete(link: SavedLink) {
        viewModelScope.launch { links.delete(link.id) }
    }

    fun restore(link: SavedLink) {
        viewModelScope.launch { links.restore(link) }
    }

    /** A saved reel can open inside a guarded app, so that app must not prompt over it. */
    fun beforeOpening(open: () -> Unit) {
        viewModelScope.launch {
            settings.grantCooldownPass()
            open()
        }
    }
}
