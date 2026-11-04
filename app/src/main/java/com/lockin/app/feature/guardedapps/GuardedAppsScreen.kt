package com.lockin.app.feature.guardedapps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lockin.app.R
import com.lockin.app.core.ui.AppIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardedAppsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guarded_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        GuardedAppsList(contentPadding = padding)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GuardedAppsList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    viewModel: GuardedAppsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.isLoading) {
        Box(modifier.fillMaxSize(), Alignment.Center) { LoadingIndicator() }
        return
    }
    val (suggested, others) = state.apps.partition { it.isSuggested }
    LazyColumn(modifier.fillMaxSize(), contentPadding = contentPadding) {
        if (suggested.isNotEmpty()) item { SectionHeader(stringResource(R.string.guarded_suggested)) }
        items(suggested, key = { it.packageName }) { app ->
            AppRow(app.label, app.packageName, app.packageName in state.selected, viewModel)
        }
        item { SectionHeader(stringResource(R.string.guarded_all)) }
        items(others, key = { it.packageName }) { app ->
            AppRow(app.label, app.packageName, app.packageName in state.selected, viewModel)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun AppRow(label: String, packageName: String, checked: Boolean, viewModel: GuardedAppsViewModel) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { AppIcon(packageName, 40.dp, viewModel::icon) },
        trailingContent = { Checkbox(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Checkbox,
            onValueChange = { viewModel.toggle(packageName) },
        ),
    )
}
