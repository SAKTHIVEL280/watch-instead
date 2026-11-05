package com.lockin.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.lockin.app.data.ApplicationScope
import com.lockin.app.data.LinkRepository
import com.lockin.app.data.StatsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/** Refreshes widgets whenever the data they show changes. The day rollover is covered by updatePeriodMillis. */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val links: LinkRepository,
    private val stats: StatsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    @OptIn(FlowPreview::class)
    fun start() {
        scope.launch {
            combine(links.links, stats.events) { _, _ -> }
                .debounce(DEBOUNCE_MILLIS)
                .collect { LockinWidget().updateAll(context) }
        }
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 500L
    }
}
