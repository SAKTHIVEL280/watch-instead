package com.watchinstead.app.feature.stats

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.watchinstead.app.R
import com.watchinstead.app.core.ui.EmptyState
import com.watchinstead.app.core.ui.SubScreen
import com.watchinstead.app.core.ui.labelRes
import com.watchinstead.app.domain.DayStat
import com.watchinstead.app.domain.Stats
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun StatsScreen(onBack: () -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SubScreen(stringResource(R.string.stats_title), onBack) { padding ->
        StatsContent(state, Modifier.padding(padding))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StatsContent(state: StatsState, modifier: Modifier = Modifier) {
    when {
        state.isLoading -> Box(modifier.fillMaxSize(), Alignment.Center) { LoadingIndicator() }
        !state.stats.hasData -> EmptyState(
            icon = Icons.Outlined.Insights,
            title = stringResource(R.string.stats_empty_title),
            body = stringResource(R.string.stats_empty_body),
            modifier = modifier.fillMaxWidth(),
        )
        else -> Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StreakCard(state.stats)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile(state.stats.weekResisted, stringResource(R.string.stats_resisted), Modifier.weight(1f))
                StatTile(state.stats.weekWatched, stringResource(R.string.stats_watched), Modifier.weight(1f))
                StatTile(state.stats.weekContinued, stringResource(R.string.stats_continued), Modifier.weight(1f))
            }
            WeekChart(state.stats.days)
            if (state.stats.reasons.isNotEmpty()) {
                val total = state.stats.reasons.sumOf { it.second }
                Section(stringResource(R.string.stats_reasons)) {
                    state.stats.reasons.forEach { (reason, count) ->
                        ShareRow(stringResource(reason.labelRes), count, total)
                    }
                }
            }
            if (state.apps.isNotEmpty()) {
                val top = state.apps.first().count
                Section(stringResource(R.string.stats_apps)) {
                    state.apps.forEach { ShareRow(it.label, it.count, top) }
                }
            }
        }
    }
}

@Composable
private fun StreakCard(stats: Stats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    stats.currentStreak.toString(),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    pluralStringResource(R.plurals.stats_streak_days, stats.currentStreak),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.stats_best_streak, stats.bestStreak),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(Icons.Outlined.LocalFireDepartment, null, Modifier.height(56.dp).width(56.dp))
        }
    }
}

@Composable
private fun StatTile(value: Int, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

/** Single series, so it needs no legend: the title names it and each non-zero bar is labelled. */
@Composable
private fun WeekChart(days: List<DayStat>) {
    val locale = LocalConfiguration.current.locales[0]
    val top = days.maxOf { it.resisted }.coerceAtLeast(1)
    val description = days.joinToString { day ->
        "${day.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)} ${day.resisted}"
    }
    Section(stringResource(R.string.stats_chart_title)) {
        Row(
            Modifier.fillMaxWidth().height(CHART_HEIGHT).semantics { contentDescription = description },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { day -> Bar(day, top, locale) }
        }
    }
}

@Composable
private fun Bar(day: DayStat, top: Int, locale: Locale) {
    val fraction by animateFloatAsState(
        day.resisted.toFloat() / top,
        MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "day bar",
    )
    Column(
        Modifier.width(BAR_SLOT_WIDTH),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (day.resisted > 0) {
            Text(day.resisted.toString(), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
        }
        val zero = day.resisted == 0
        Box(
            Modifier
                .width(BAR_WIDTH)
                .height(if (zero) ZERO_BAR_HEIGHT else MAX_BAR_HEIGHT * fraction.coerceAtLeast(MIN_BAR_FRACTION))
                .background(
                    if (zero) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp),
                ),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ShareRow(label: String, count: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(count.toString(), style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(
            progress = { count.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
            drawStopIndicator = {},
        )
    }
}

private val CHART_HEIGHT = 170.dp
private val MAX_BAR_HEIGHT = 110.dp
private val BAR_WIDTH = 18.dp
private val BAR_SLOT_WIDTH = 40.dp
private val ZERO_BAR_HEIGHT = 2.dp
private const val MIN_BAR_FRACTION = 0.06f
