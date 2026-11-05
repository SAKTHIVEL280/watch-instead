package com.lockin.app.feature.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lockin.app.R
import com.lockin.app.core.ui.EmptyState
import com.lockin.app.core.ui.SubScreen
import com.lockin.app.domain.GuardSettings

@Composable
fun MessagesScreen(onBack: () -> Unit, viewModel: MessagesViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }

    SubScreen(
        title = stringResource(R.string.messages_title),
        onBack = onBack,
        floatingActionButton = {
            if (messages.size < GuardSettings.MAX_MESSAGES) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editing = null
                        showEditor = true
                    },
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text(stringResource(R.string.messages_add)) },
                )
            }
        },
    ) { padding ->
        if (messages.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.FormatQuote,
                title = stringResource(R.string.messages_empty_title),
                body = stringResource(R.string.messages_empty_body),
                modifier = Modifier.padding(padding).fillMaxWidth(),
            )
        } else {
            LazyColumn(contentPadding = padding) {
                items(messages, key = { it }) { message ->
                    ListItem(
                        headlineContent = { Text(message) },
                        trailingContent = {
                            IconButton(onClick = { viewModel.delete(message) }) {
                                Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.library_delete))
                            }
                        },
                        modifier = Modifier.clickable {
                            editing = message
                            showEditor = true
                        },
                    )
                }
            }
        }
    }

    if (showEditor) {
        MessageDialog(
            initial = editing.orEmpty(),
            onDismiss = { showEditor = false },
            onSave = {
                viewModel.save(editing, it)
                showEditor = false
            },
        )
    }
}

@Composable
private fun MessageDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.messages_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(GuardSettings.MAX_MESSAGE_LENGTH) },
                placeholder = { Text(stringResource(R.string.messages_placeholder)) },
                supportingText = { Text("${text.length}/${GuardSettings.MAX_MESSAGE_LENGTH}") },
                maxLines = 3,
                modifier = Modifier.padding(top = 4.dp),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.capture_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
