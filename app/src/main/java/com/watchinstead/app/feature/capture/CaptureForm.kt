package com.watchinstead.app.feature.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watchinstead.app.R
import com.watchinstead.app.core.ui.labelRes
import com.watchinstead.app.domain.Category

/** Bottom sheet used by the share target, the library and onboarding. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(
    viewModel: CaptureViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onSaved()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        CaptureForm(
            state = state,
            onInputChange = viewModel::onInputChange,
            onNoteChange = viewModel::onNoteChange,
            onCategoryChange = viewModel::onCategoryChange,
            onSave = viewModel::save,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CaptureForm(
    state: CaptureState,
    onInputChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = when {
        state.showInvalid -> stringResource(R.string.capture_no_link)
        state.isDuplicate -> stringResource(R.string.capture_duplicate)
        else -> null
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(if (state.editingId == null) R.string.capture_title else R.string.capture_edit_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        OutlinedTextField(
            value = state.input,
            onValueChange = onInputChange,
            label = { Text(stringResource(R.string.capture_link_label)) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        // Wraps instead of a connected row: five labels do not fit one line on a phone.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Category.entries.forEach { category ->
                FilterChip(
                    selected = category == state.category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(stringResource(category.labelRes)) },
                )
            }
        }
        OutlinedTextField(
            value = state.note,
            onValueChange = onNoteChange,
            label = { Text(stringResource(R.string.capture_note_label)) },
            maxLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = onSave, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.editingId == null) R.string.capture_save else R.string.capture_save_changes))
        }
    }
}
