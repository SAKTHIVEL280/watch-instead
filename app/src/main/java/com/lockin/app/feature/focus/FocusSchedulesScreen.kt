package com.lockin.app.feature.focus

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lockin.app.R
import com.lockin.app.core.ui.EmptyState
import com.lockin.app.core.ui.SubScreen
import com.lockin.app.domain.FocusPolicy
import com.lockin.app.domain.FocusSchedule
import com.lockin.app.domain.GuardSettings
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

private const val DEFAULT_START = 9 * 60
private const val DEFAULT_END = 17 * 60
private val WEEKDAYS = setOf(1, 2, 3, 4, 5)

@Composable
fun FocusSchedulesScreen(onBack: () -> Unit, viewModel: FocusViewModel = hiltViewModel()) {
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<FocusSchedule?>(null) }

    SubScreen(
        title = stringResource(R.string.focus_title),
        onBack = onBack,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editing = FocusSchedule(UUID.randomUUID().toString(), "", WEEKDAYS, DEFAULT_START, DEFAULT_END)
                },
                icon = { Icon(Icons.Outlined.Add, null) },
                text = { Text(stringResource(R.string.focus_add)) },
            )
        },
    ) { padding ->
        if (schedules.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.Schedule,
                title = stringResource(R.string.focus_empty_title),
                body = stringResource(R.string.focus_empty_body),
                modifier = Modifier.padding(padding).fillMaxWidth(),
            )
        } else {
            LazyColumn(contentPadding = padding) {
                item {
                    Text(
                        stringResource(R.string.focus_explainer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(schedules, key = { it.id }) { schedule ->
                    ListItem(
                        headlineContent = { Text(schedule.name) },
                        supportingContent = { Text(summary(schedule, LocalConfiguration.current.locales[0])) },
                        trailingContent = {
                            Switch(schedule.enabled, { viewModel.setEnabled(schedule, it) })
                        },
                        modifier = Modifier.clickable { editing = schedule },
                    )
                }
            }
        }
    }

    editing?.let { draft ->
        ScheduleEditor(
            initial = draft,
            canDelete = schedules.any { it.id == draft.id },
            onDismiss = { editing = null },
            onSave = {
                viewModel.save(it)
                editing = null
            },
            onDelete = {
                viewModel.delete(draft)
                editing = null
            },
        )
    }
}

private fun dayName(day: Int, locale: Locale) = DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, locale)

private fun formatTime(minute: Int): String = LocalTime.of(minute / 60, minute % 60)
    .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

private fun summary(schedule: FocusSchedule, locale: Locale): String {
    val days = schedule.days.sorted().joinToString(", ") { dayName(it, locale) }
    return "$days · ${formatTime(schedule.startMinute)} – ${formatTime(schedule.endMinute)}"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScheduleEditor(
    initial: FocusSchedule,
    canDelete: Boolean,
    onDismiss: () -> Unit,
    onSave: (FocusSchedule) -> Unit,
    onDelete: () -> Unit,
) {
    var draft by remember(initial.id) { mutableStateOf(initial) }
    var picking by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val locale = LocalConfiguration.current.locales[0]

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.focus_editor_title), style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft = draft.copy(name = it.take(GuardSettings.MAX_SCHEDULE_NAME_LENGTH)) },
                label = { Text(stringResource(R.string.focus_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..7).forEach { day ->
                    FilterChip(
                        selected = day in draft.days,
                        onClick = {
                            val days = if (day in draft.days) draft.days - day else draft.days + day
                            draft = draft.copy(days = days)
                        },
                        label = { Text(dayName(day, locale)) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { picking = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.focus_from, formatTime(draft.startMinute)))
                }
                FilledTonalButton(onClick = { picking = false }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.focus_to, formatTime(draft.endMinute)))
                }
            }
            if (draft.endMinute < draft.startMinute) {
                Text(stringResource(R.string.focus_overnight), style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onSave(draft.copy(name = draft.name.trim())) },
                enabled = FocusPolicy.isValid(draft),
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.capture_save))
            }
            if (canDelete) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.library_delete))
                }
            }
        }
    }

    picking?.let { isStart ->
        TimeDialog(
            minute = if (isStart) draft.startMinute else draft.endMinute,
            onDismiss = { picking = null },
            onConfirm = {
                draft = if (isStart) draft.copy(startMinute = it) else draft.copy(endMinute = it)
                picking = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(minute: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(minute / 60, minute % 60, DateFormat.is24HourFormat(context))
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(stringResource(R.string.capture_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
