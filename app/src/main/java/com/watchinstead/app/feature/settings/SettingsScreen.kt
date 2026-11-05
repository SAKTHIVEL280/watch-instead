package com.lockin.app.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lockin.app.BuildConfig
import com.lockin.app.R
import com.lockin.app.core.ui.SubScreen
import com.lockin.app.core.ui.ConnectedChoice
import com.lockin.app.core.theme.isDark
import com.lockin.app.core.theme.supportsDynamicColor
import com.lockin.app.core.theme.swatch
import com.lockin.app.domain.Accent
import com.lockin.app.feature.backup.BackupViewModel
import com.lockin.app.feature.backup.rememberBackupActions
import com.lockin.app.domain.Appearance
import com.lockin.app.domain.GuardSettings
import com.lockin.app.domain.ThemeMode
import com.lockin.app.intervention.GuardStatus

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onGuardedApps: () -> Unit,
    onPrivacy: () -> Unit,
    onLicenses: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
) {
    val settings by viewModel.state.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val backup = rememberBackupActions(backupViewModel)
    var guardEnabled by remember { mutableStateOf(true) }
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    var confirmClearStats by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        guardEnabled = GuardStatus.isEnabled(context)
        onPauseOrDispose { }
    }

    SubScreen(stringResource(R.string.settings_title), onBack) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            SectionHeader(stringResource(R.string.settings_section_appearance))
            AppearanceSettings(appearance, viewModel)
            SectionHeader(stringResource(R.string.settings_section_protection))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_protection)) },
                supportingContent = { Text(stringResource(R.string.settings_protection_body)) },
                trailingContent = { Switch(checked = settings.protectionEnabled, onCheckedChange = null) },
                modifier = Modifier.toggleable(
                    value = settings.protectionEnabled,
                    role = Role.Switch,
                    onValueChange = viewModel::setProtection,
                ),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_guard_status)) },
                supportingContent = {
                    Text(stringResource(if (guardEnabled) R.string.settings_guard_on else R.string.settings_guard_off))
                },
                modifier = Modifier.clickable { GuardStatus.openSettings(context) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_guarded_apps)) },
                supportingContent = {
                    val count = settings.guardedPackages.size
                    Text(pluralStringResource(R.plurals.settings_guarded_count, count, count))
                },
                modifier = Modifier.clickable(onClick = onGuardedApps),
            )
            SwitchRow(
                title = stringResource(R.string.settings_ask_why),
                body = stringResource(R.string.settings_ask_why_body),
                checked = settings.askWhy,
                onChange = viewModel::setAskWhy,
            )
            ChoiceSetting(
                title = stringResource(R.string.settings_cooldown),
                body = stringResource(
                    if (settings.cooldownMinutes == GuardSettings.NO_COOLDOWN) {
                        R.string.settings_cooldown_none_body
                    } else {
                        R.string.settings_cooldown_body
                    },
                ),
                options = GuardSettings.COOLDOWN_OPTIONS,
                selected = settings.cooldownMinutes,
                onSelect = viewModel::setCooldown,
                label = {
                    if (it == GuardSettings.NO_COOLDOWN) {
                        stringResource(R.string.settings_cooldown_none)
                    } else {
                        stringResource(R.string.settings_cooldown_value, it)
                    }
                },
            )
            ChoiceSetting(
                title = stringResource(R.string.settings_skip_delay),
                body = stringResource(R.string.settings_skip_delay_body),
                options = GuardSettings.SKIP_DELAY_OPTIONS,
                selected = settings.skipDelaySeconds,
                onSelect = viewModel::setSkipDelay,
                label = { stringResource(R.string.settings_skip_delay_value, it) },
            )
            SectionHeader(stringResource(R.string.settings_section_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_export)) },
                supportingContent = { Text(stringResource(R.string.settings_export_body)) },
                modifier = Modifier.clickable(onClick = backup.export),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_import)) },
                supportingContent = { Text(stringResource(R.string.settings_import_body)) },
                modifier = Modifier.clickable(onClick = backup.import),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_clear_watched)) },
                modifier = Modifier.clickable { confirmClear = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_clear_stats)) },
                modifier = Modifier.clickable { confirmClearStats = true },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_privacy)) },
                modifier = Modifier.clickable(onClick = onPrivacy),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_licenses)) },
                modifier = Modifier.clickable(onClick = onLicenses),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME)) },
            )
        }
    }

    if (confirmClear) {
        ConfirmDialog(stringResource(R.string.settings_clear_watched_confirm), { confirmClear = false }, viewModel::clearWatched)
    }
    if (confirmClearStats) {
        ConfirmDialog(stringResource(R.string.settings_clear_stats_confirm), { confirmClearStats = false }, viewModel::clearStats)
    }
}

@Composable
private fun ConfirmDialog(message: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) { Text(stringResource(R.string.settings_clear)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun AppearanceSettings(appearance: Appearance, viewModel: SettingsViewModel) {
    ChoiceSetting(
        title = stringResource(R.string.settings_theme),
        body = stringResource(R.string.settings_theme_body),
        options = ThemeMode.entries,
        selected = appearance.themeMode,
        onSelect = viewModel::setThemeMode,
        label = {
            stringResource(
                when (it) {
                    ThemeMode.SYSTEM -> R.string.theme_system
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                },
            )
        },
    )
    if (supportsDynamicColor) {
        SwitchRow(
            title = stringResource(R.string.settings_dynamic_color),
            body = stringResource(R.string.settings_dynamic_color_body),
            checked = appearance.dynamicColor,
            onChange = viewModel::setDynamicColor,
        )
    }
    AnimatedVisibility(
        visible = !appearance.dynamicColor || !supportsDynamicColor,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        AccentPicker(appearance.accent, viewModel::setAccent)
    }
    AnimatedVisibility(
        visible = appearance.isDark(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        SwitchRow(
            title = stringResource(R.string.settings_pure_black),
            body = stringResource(R.string.settings_pure_black_body),
            checked = appearance.pureBlack,
            onChange = viewModel::setPureBlack,
        )
    }
}

@Composable
private fun SwitchRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(body) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    )
}

@Composable
private fun AccentPicker(selected: Accent, onSelect: (Accent) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.settings_accent), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Accent.entries.forEach { accent ->
                val isSelected = accent == selected
                val scale by animateFloatAsState(
                    if (isSelected) 1.15f else 1f,
                    MaterialTheme.motionScheme.fastSpatialSpec(),
                    label = "accent scale",
                )
                val name = stringResource(accent.labelRes)
                Box(
                    Modifier
                        .size(48.dp)
                        .scale(scale)
                        .clip(if (isSelected) MaterialTheme.shapes.medium else CircleShape)
                        .background(accent.swatch())
                        .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(accent) })
                        .semantics { contentDescription = name },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) Icon(Icons.Outlined.Check, null, tint = Color.White)
                }
            }
        }
    }
}

private val Accent.labelRes: Int
    get() = when (this) {
        Accent.INDIGO -> R.string.accent_indigo
        Accent.ROSE -> R.string.accent_rose
        Accent.TEAL -> R.string.accent_teal
        Accent.AMBER -> R.string.accent_amber
    }

@Composable
private fun <T> ChoiceSetting(
    title: String,
    body: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ConnectedChoice(options, selected, onSelect, label = label)
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    SubScreen(stringResource(R.string.settings_privacy), onBack) { padding ->
        Text(
            stringResource(R.string.privacy_body),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
        )
    }
}

private val OPEN_SOURCE_LIBRARIES = listOf(
    "AndroidX and Jetpack Compose" to "Apache License 2.0",
    "Material 3 for Compose" to "Apache License 2.0",
    "Kotlin and kotlinx libraries" to "Apache License 2.0",
    "Dagger Hilt" to "Apache License 2.0",
    "Coil" to "Apache License 2.0",
    "OkHttp" to "Apache License 2.0",
)

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    SubScreen(stringResource(R.string.settings_licenses), onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            items(OPEN_SOURCE_LIBRARIES) { (name, license) ->
                ListItem(headlineContent = { Text(name) }, supportingContent = { Text(license) })
            }
        }
    }
}
