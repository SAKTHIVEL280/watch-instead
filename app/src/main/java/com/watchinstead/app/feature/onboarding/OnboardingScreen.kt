package com.watchinstead.app.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watchinstead.app.R
import com.watchinstead.app.core.ui.LinkThumbnail
import com.watchinstead.app.core.ui.MorphingBlob
import com.watchinstead.app.domain.MIN_ONBOARDING_LINKS
import com.watchinstead.app.domain.SUGGESTED_ONBOARDING_LINKS
import com.watchinstead.app.domain.SavedLink
import com.watchinstead.app.feature.backup.BackupViewModel
import com.watchinstead.app.feature.backup.rememberBackupActions
import com.watchinstead.app.feature.capture.CaptureSheet
import com.watchinstead.app.feature.capture.CaptureViewModel
import com.watchinstead.app.feature.guardedapps.GuardedAppsList
import com.watchinstead.app.intervention.GuardStatus

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    captureViewModel: CaptureViewModel = hiltViewModel(),
    backupViewModel: BackupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.step != OnboardingStep.WELCOME) { viewModel.back() }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().wrapContentWidth().widthIn(max = 600.dp).padding(24.dp),
        ) {
            if (state.step != OnboardingStep.WELCOME) {
                Text(
                    stringResource(R.string.onboarding_step, state.step.ordinal, OnboardingStep.entries.size - 1),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
            }
            AnimatedContent(
                targetState = state.step,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    val spatial = spring<IntOffset>(dampingRatio = 0.75f, stiffness = 300f)
                    (slideInHorizontally(spatial) { it / 3 * direction } + fadeIn()) togetherWith
                        (slideOutHorizontally(spatial) { -it / 3 * direction } + fadeOut())
                },
                label = "onboarding step",
            ) { step ->
                when (step) {
                    OnboardingStep.WELCOME -> WelcomeStep(viewModel::next)
                    OnboardingStep.VIDEOS -> VideosStep(state, viewModel::remove, viewModel::next, captureViewModel, backupViewModel)
                    OnboardingStep.APPS -> AppsStep(state.canContinue, viewModel::next)
                    OnboardingStep.PERMISSION -> PermissionStep(viewModel::finish)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Title(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineLargeEmphasized,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shapes = ButtonDefaults.shapes(),
        modifier = Modifier.fillMaxWidth().heightIn(min = ButtonDefaults.MediumContainerHeight),
        contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
    ) {
        Text(text)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            MorphingBlob(Icons.Outlined.Lock, size = 200.dp)
        }
        Text(
            stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.displayMediumEmphasized,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(stringResource(R.string.onboarding_welcome_body), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        PrimaryButton(stringResource(R.string.onboarding_get_started), onClick = onNext)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VideosStep(
    state: OnboardingState,
    onRemove: (SavedLink) -> Unit,
    onNext: () -> Unit,
    captureViewModel: CaptureViewModel,
    backupViewModel: BackupViewModel,
) {
    val backup = rememberBackupActions(backupViewModel)
    var showCapture by rememberSaveable { mutableStateOf(false) }
    val count = state.links.size
    val progress by animateFloatAsState(
        (count.toFloat() / SUGGESTED_ONBOARDING_LINKS).coerceAtMost(1f),
        MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "videos progress",
    )

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Title(stringResource(R.string.onboarding_videos_title))
        Text(
            stringResource(R.string.onboarding_videos_body, MIN_ONBOARDING_LINKS, SUGGESTED_ONBOARDING_LINKS),
            style = MaterialTheme.typography.bodyLarge,
        )
        LinearWavyProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text(
            stringResource(R.string.onboarding_videos_progress, count, SUGGESTED_ONBOARDING_LINKS),
            style = MaterialTheme.typography.labelLarge,
        )
        LazyColumn(Modifier.weight(1f)) {
            items(state.links, key = { it.id }) { link ->
                ListItem(
                    headlineContent = {
                        Text(
                            link.title ?: link.url.removePrefix("https://").removePrefix("www."),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = { LinkThumbnail(link.thumbnailUrl, link.platform, Modifier.size(48.dp)) },
                    trailingContent = {
                        IconButton(onClick = { onRemove(link) }) {
                            Icon(Icons.Outlined.Close, stringResource(R.string.library_delete))
                        }
                    },
                )
            }
        }
        FilledTonalButton(
            onClick = { showCapture = true },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Add, null)
            Text(stringResource(R.string.library_add), Modifier.padding(start = 8.dp))
        }
        TextButton(onClick = backup.import, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.onboarding_restore))
        }
        if (!state.canContinue) {
            Text(
                stringResource(R.string.onboarding_videos_needed, MIN_ONBOARDING_LINKS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryButton(stringResource(R.string.action_continue), state.canContinue, onNext)
    }

    if (showCapture) {
        val close = {
            showCapture = false
            captureViewModel.reset()
        }
        CaptureSheet(captureViewModel, onDismiss = close, onSaved = close)
    }
}

@Composable
private fun AppsStep(canContinue: Boolean, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Title(stringResource(R.string.onboarding_apps_title))
        Text(stringResource(R.string.onboarding_apps_body), style = MaterialTheme.typography.bodyLarge)
        Box(Modifier.weight(1f)) { GuardedAppsList() }
        if (!canContinue) {
            Text(
                stringResource(R.string.onboarding_apps_needed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PrimaryButton(stringResource(R.string.action_continue), canContinue, onNext)
    }
}

@Composable
private fun PermissionStep(onFinish: () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(false) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        granted = GuardStatus.isEnabled(context)
        onPauseOrDispose { }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Title(stringResource(R.string.onboarding_permission_title))
        Text(stringResource(R.string.onboarding_permission_body), style = MaterialTheme.typography.bodyLarge)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    R.string.onboarding_disclosure_what,
                    R.string.onboarding_disclosure_why,
                    R.string.onboarding_disclosure_never,
                    R.string.onboarding_disclosure_local,
                ).forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
            }
        }
        if (granted) {
            Text(stringResource(R.string.onboarding_permission_granted), style = MaterialTheme.typography.titleMedium)
            PrimaryButton(stringResource(R.string.onboarding_finish), onClick = onFinish)
        } else {
            PrimaryButton(stringResource(R.string.onboarding_agree)) { GuardStatus.openSettings(context) }
            TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_not_now))
            }
            TextButton(onClick = { showHelp = !showHelp }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_help_title))
            }
            if (showHelp) {
                Text(stringResource(R.string.onboarding_help_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
