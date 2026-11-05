package com.watchinstead.app.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.watchinstead.app.data.Clock
import com.watchinstead.app.data.LinkRepository
import com.watchinstead.app.data.StatsRepository
import com.watchinstead.app.data.apps.InstalledAppsRepository
import com.watchinstead.app.domain.Stats
import com.watchinstead.app.domain.StatsCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class AppStat(val label: String, val count: Int)

data class StatsState(
    val isLoading: Boolean = true,
    val stats: Stats = Stats(),
    val apps: List<AppStat> = emptyList(),
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    stats: StatsRepository,
    links: LinkRepository,
    apps: InstalledAppsRepository,
    clock: Clock,
) : ViewModel() {
    val state = combine(stats.events, links.links) { events, saved ->
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(clock.now()).atZone(zone).toLocalDate()
        val result = StatsCalculator.calculate(events, saved.mapNotNull { it.watchedAt }, today, zone)
        StatsState(
            isLoading = false,
            stats = result,
            // An uninstalled app no longer has a label, so its package name is shown instead.
            apps = result.apps.map { (name, count) -> AppStat(apps.label(name) ?: name, count) },
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsState())
}
