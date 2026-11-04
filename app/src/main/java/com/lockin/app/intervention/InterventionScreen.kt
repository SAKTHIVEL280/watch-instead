package com.lockin.app.intervention

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lockin.app.R
import com.lockin.app.core.ui.EmptyState
import com.lockin.app.core.ui.LinkThumbnail
import com.lockin.app.core.ui.labelRes
import com.lockin.app.domain.OpenReason
import com.lockin.app.domain.SavedLink

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InterventionScreen(
    state: InterventionState,
    onWatch: (SavedLink) -> Unit,
    onNext: () -> Unit,
    onGoHome: () -> Unit,
    onContinue: () -> Unit,
    onSelectReason: (OpenReason) -> Unit = {},
) {
    Surface(Modifier.fillMaxSize()) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { LoadingIndicator() }
            return@Surface
        }
        val entrance = remember { MutableTransitionState(false).apply { targetState = true } }
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            // Anchored to the bottom so that the choices sit within thumb reach.
            InterventionContent(state, entrance, maxHeight, onWatch, onNext, onGoHome, onContinue, onSelectReason)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InterventionContent(
    state: InterventionState,
    entrance: MutableTransitionState<Boolean>,
    minHeight: Dp,
    onWatch: (SavedLink) -> Unit,
    onNext: () -> Unit,
    onGoHome: () -> Unit,
    onContinue: () -> Unit,
    onSelectReason: (OpenReason) -> Unit,
) {
    Box {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minHeight)
                .wrapContentWidth()
                .widthIn(max = 600.dp)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.Bottom),
        ) {
            state.focusName?.let {
                Text(
                    stringResource(R.string.intervention_focus, it),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            AnimatedVisibility(
                visibleState = entrance,
                enter = slideInVertically(spring(dampingRatio = 0.7f, stiffness = 300f)) { it / 2 } + fadeIn(),
            ) {
            Text(
                state.appLabel?.let { stringResource(R.string.intervention_title, it) }
                    ?: stringResource(R.string.intervention_title_generic),
                style = MaterialTheme.typography.displaySmallEmphasized,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() },
            )
            }
            state.personalMessage?.let { PersonalMessage(it) }
            val current = state.current
            if (current == null) {
                EmptyState(
                    icon = Icons.Outlined.SelfImprovement,
                    title = stringResource(R.string.intervention_empty_title),
                    body = stringResource(R.string.intervention_empty_body),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    stringResource(R.string.intervention_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        (scaleIn(spring(dampingRatio = 0.65f, stiffness = 320f), initialScale = 0.9f) + fadeIn()) togetherWith
                            (scaleOut(targetScale = 1.04f) + fadeOut())
                    },
                    label = "saved video",
                ) { link -> VideoPreview(link, compact = state.askWhy || state.personalMessage != null) }
                Button(
                    onClick = { onWatch(current) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = ButtonDefaults.MediumContainerHeight),
                    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                ) {
                    Text(stringResource(R.string.intervention_watch))
                }
                if (state.queue.size > 1) {
                    FilledTonalButton(
                        onClick = onNext,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.intervention_next))
                    }
                }
            }
            if (state.askWhy) ReasonPicker(state.reason, onSelectReason)
            OutlinedButton(onClick = onGoHome, shapes = ButtonDefaults.shapes(), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.intervention_home))
            }
            TextButton(
                onClick = onContinue,
                enabled = state.secondsLeft <= 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AnimatedVisibility(state.secondsLeft > 0) {
                    val remaining by animateFloatAsState(
                        state.secondsLeft.toFloat() / state.totalSeconds.coerceAtLeast(1),
                        tween(1_000, easing = LinearEasing),
                        label = "continue countdown",
                    )
                    CircularProgressIndicator(
                        progress = { remaining },
                        modifier = Modifier.padding(end = 8.dp).size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(
                    if (state.secondsLeft > 0) {
                        stringResource(R.string.intervention_continue_in, state.secondsLeft)
                    } else {
                        stringResource(R.string.intervention_continue)
                    },
                )
            }
        }
    }
}

private const val COMPACT_ASPECT_RATIO = 3f

@Composable
private fun PersonalMessage(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Text(message, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun ReasonPicker(selected: OpenReason?, onSelect: (OpenReason) -> Unit) {
    Column {
        Text(
            stringResource(R.string.intervention_why),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpenReason.entries.forEach { reason ->
                FilterChip(
                    selected = reason == selected,
                    onClick = { onSelect(reason) },
                    label = { Text(stringResource(reason.labelRes)) },
                )
            }
        }
    }
}

@Composable
private fun VideoPreview(link: SavedLink, compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinkThumbnail(link.thumbnailUrl, link.platform, Modifier.fillMaxWidth().aspectRatio(if (compact) COMPACT_ASPECT_RATIO else 16f / 9f))
        Text(
            link.title ?: link.url.removePrefix("https://").removePrefix("www."),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        if (link.note.isNotBlank()) {
            Text(link.note, style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            stringResource(link.category.labelRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
