package com.watchinstead.app.intervention

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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
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
import com.watchinstead.app.R
import com.watchinstead.app.core.ui.EmptyState
import com.watchinstead.app.core.ui.LinkThumbnail
import com.watchinstead.app.core.ui.labelRes
import com.watchinstead.app.domain.OpenReason
import com.watchinstead.app.domain.SavedLink

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InterventionScreen(
    state: InterventionState,
    onWatch: (SavedLink) -> Unit,
    onNext: () -> Unit,
    onGoHome: () -> Unit,
    onContinue: () -> Unit,
    onSelectReason: (OpenReason) -> Unit = {},
    onSkipReason: () -> Unit = {},
) {
    Surface(Modifier.fillMaxSize()) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { LoadingIndicator() }
            return@Surface
        }
        val entrance = remember { MutableTransitionState(false).apply { targetState = true } }
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            // Anchored to the bottom so that the choices sit within thumb reach.
            val minHeight = maxHeight
            AnimatedContent(state.askingWhy, label = "prompt step") { asking ->
                if (asking) {
                    ReasonStep(minHeight, onSelectReason, onSkipReason)
                } else {
                    InterventionContent(state, entrance, minHeight, onWatch, onNext, onGoHome, onContinue)
                }
            }
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
            val current = state.current
            if (current == null) {
                state.personalMessage?.let { Subtitle(it) }
                EmptyState(
                    icon = Icons.Outlined.SelfImprovement,
                    title = stringResource(R.string.intervention_empty_title),
                    body = stringResource(R.string.intervention_empty_body),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Subtitle(state.personalMessage ?: stringResource(R.string.intervention_subtitle))
                AnimatedContent(
                    targetState = current,
                    transitionSpec = {
                        (scaleIn(spring(dampingRatio = 0.65f, stiffness = 320f), initialScale = 0.9f) + fadeIn()) togetherWith
                            (scaleOut(targetScale = 1.04f) + fadeOut())
                    },
                    label = "saved video",
                ) { link -> VideoPreview(link, compact = minHeight < COMPACT_BELOW_HEIGHT) }
                Button(
                    onClick = { onWatch(current) },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = ButtonDefaults.MediumContainerHeight),
                    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
                ) {
                    Text(stringResource(R.string.intervention_watch))
                }
            }
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onGoHome, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text(stringResource(R.string.intervention_home))
                }
                if (state.queue.size > 1) {
                    FilledTonalButton(onClick = onNext, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Text(stringResource(R.string.intervention_next))
                    }
                }
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
private val COMPACT_BELOW_HEIGHT = 640.dp

@Composable
private fun Subtitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ReasonStep(minHeight: Dp, onSelect: (OpenReason) -> Unit, onSkip: () -> Unit) {
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
        Text(
            stringResource(R.string.intervention_why),
            style = MaterialTheme.typography.displaySmallEmphasized,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Subtitle(stringResource(R.string.intervention_why_body))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OpenReason.entries.forEach { reason ->
                SuggestionChip(
                    onClick = { onSelect(reason) },
                    label = { Text(stringResource(reason.labelRes)) },
                )
            }
        }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.intervention_why_skip))
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
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (link.note.isNotBlank()) {
            Text(
                link.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
