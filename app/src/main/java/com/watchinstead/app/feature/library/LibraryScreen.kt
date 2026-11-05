package com.lockin.app.feature.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lockin.app.R
import com.lockin.app.core.ui.ConnectedChoice
import com.lockin.app.core.ui.EmptyState
import com.lockin.app.core.ui.LinkThumbnail
import com.lockin.app.core.ui.StatusBanner
import com.lockin.app.core.ui.labelRes
import com.lockin.app.core.ui.openLink
import com.lockin.app.domain.Category
import com.lockin.app.domain.SavedLink
import com.lockin.app.feature.capture.CaptureSheet
import com.lockin.app.feature.capture.CaptureViewModel
import com.lockin.app.intervention.GuardStatus
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

private enum class LibraryBanner { GUARD_OFF, NO_APPS }

@Composable
private fun Shortcuts(scheduleCount: Int, messageCount: Int, onFocus: () -> Unit, onMessages: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Shortcut(
            icon = Icons.Outlined.Schedule,
            title = stringResource(R.string.library_focus),
            count = scheduleCount,
            countLabel = pluralStringResource(R.plurals.settings_schedule_count, scheduleCount, scheduleCount),
            onClick = onFocus,
            modifier = Modifier.weight(1f),
        )
        Shortcut(
            icon = Icons.Outlined.ChatBubbleOutline,
            title = stringResource(R.string.library_messages),
            count = messageCount,
            countLabel = pluralStringResource(R.plurals.settings_message_count, messageCount, messageCount),
            onClick = onMessages,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Shortcut(
    icon: ImageVector,
    title: String,
    count: Int,
    countLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    if (count == 0) stringResource(R.string.library_not_set) else countLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryScreen(
    onOpenSettings: () -> Unit,
    onChooseApps: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenMessages: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
    captureViewModel: CaptureViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showCapture by rememberSaveable { mutableStateOf(false) }
    var guardEnabled by remember { mutableStateOf(true) }

    LifecycleResumeEffect(Unit) {
        guardEnabled = GuardStatus.isEnabled(context)
        onPauseOrDispose { }
    }

    val markedWatched = stringResource(R.string.library_marked_watched)
    val movedBack = stringResource(R.string.library_moved_back)
    val deleted = stringResource(R.string.library_deleted)
    val undo = stringResource(R.string.action_undo)
    val openFailed = stringResource(R.string.library_open_failed)

    fun showUndo(message: String, onUndo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message, undo, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }

    fun toggleWatched(link: SavedLink) {
        val nowWatched = link.watchedAt == null
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        viewModel.setWatched(link, nowWatched)
        showUndo(if (nowWatched) markedWatched else movedBack) { viewModel.setWatched(link, !nowWatched) }
    }

    fun delete(link: SavedLink) {
        viewModel.delete(link)
        showUndo(deleted) { viewModel.restore(link) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Outlined.Insights, stringResource(R.string.stats_title))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(R.string.library_settings))
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCapture = true },
                expanded = !listState.canScrollBackward,
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text(stringResource(R.string.library_add)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val banner = when {
                    !guardEnabled -> LibraryBanner.GUARD_OFF
                    state.noAppsGuarded && !state.isLoading -> LibraryBanner.NO_APPS
                    else -> null
                }
                AnimatedVisibility(banner != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    if (banner == LibraryBanner.GUARD_OFF) {
                        StatusBanner(
                            title = stringResource(R.string.banner_service_off_title),
                            body = stringResource(R.string.banner_service_off_body),
                            action = stringResource(R.string.banner_turn_on),
                            onAction = { GuardStatus.openSettings(context) },
                        )
                    } else {
                        StatusBanner(
                            title = stringResource(R.string.banner_no_apps_title),
                            body = stringResource(R.string.banner_no_apps_body),
                            action = stringResource(R.string.banner_choose_apps),
                            onAction = onChooseApps,
                        )
                    }
                }
            }
            item { Shortcuts(state.scheduleCount, state.messageCount, onOpenFocus, onOpenMessages) }
            item {
                ConnectedChoice(
                    options = LibraryTab.entries,
                    selected = state.tab,
                    onSelect = viewModel::selectTab,
                    label = {
                        stringResource(
                            if (it == LibraryTab.QUEUE) R.string.library_tab_queue else R.string.library_tab_watched,
                        )
                    },
                )
            }
            item { CategoryFilters(state.filter, viewModel::selectFilter) }
            when {
                state.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { LoadingIndicator() }
                }
                state.links.isEmpty() -> item {
                    val isQueue = state.tab == LibraryTab.QUEUE
                    EmptyState(
                        icon = Icons.Outlined.Bookmarks,
                        title = stringResource(
                            if (isQueue) R.string.library_empty_queue_title else R.string.library_empty_watched_title,
                        ),
                        body = stringResource(
                            if (isQueue) R.string.library_empty_queue_body else R.string.library_empty_watched_body,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> items(state.links, key = { it.id }, contentType = { "link" }) { link ->
                    SwipeableLinkCard(
                        link = link,
                        onOpen = {
                            viewModel.beforeOpening {
                                if (!context.openLink(link.url)) scope.launch { snackbar.showSnackbar(openFailed) }
                            }
                        },
                        onEdit = {
                            captureViewModel.startEditing(link)
                            showCapture = true
                        },
                        onToggleWatched = { toggleWatched(link) },
                        onDelete = { delete(link) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (showCapture) {
        CaptureSheet(
            viewModel = captureViewModel,
            onDismiss = {
                showCapture = false
                captureViewModel.reset()
            },
            onSaved = {
                showCapture = false
                captureViewModel.reset()
            },
        )
    }
}

@Composable
private fun CategoryFilters(selected: Category?, onSelect: (Category?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.library_filter_all)) },
            )
        }
        items(Category.entries) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(stringResource(category.labelRes)) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableLinkCard(
    link: SavedLink,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onToggleWatched: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val towardsEnd = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Row(
                Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalArrangement = if (towardsEnd) Arrangement.Start else Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (towardsEnd) Icons.Outlined.CheckCircle else Icons.Outlined.DeleteOutline, null)
            }
        },
        onDismiss = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> onToggleWatched()
                SwipeToDismissBoxValue.EndToStart -> onDelete()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            scope.launch { dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
        },
    ) {
        LinkCard(link, onOpen, onEdit, onToggleWatched, onDelete)
    }
}

@Composable
private fun LinkCard(
    link: SavedLink,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onToggleWatched: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LinkThumbnail(link.thumbnailUrl, link.platform, Modifier.size(88.dp).aspectRatio(1f))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    link.title ?: link.note.ifBlank { link.url.removePrefix("https://").removePrefix("www.") },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (link.title != null && link.note.isNotBlank()) {
                    Text(
                        link.note,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    stringResource(link.category.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Box {
                // Swipes are not reachable from every input method, so the menu offers the same actions.
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, stringResource(R.string.library_more_actions))
                }
                DropdownMenu(menuOpen, { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_edit)) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (link.watchedAt == null) R.string.library_mark_watched else R.string.library_mark_unwatched,
                                ),
                            )
                        },
                        leadingIcon = {
                            Icon(if (link.watchedAt == null) Icons.Outlined.CheckCircle else Icons.Outlined.Replay, null)
                        },
                        onClick = {
                            menuOpen = false
                            onToggleWatched()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.library_delete)) },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}
