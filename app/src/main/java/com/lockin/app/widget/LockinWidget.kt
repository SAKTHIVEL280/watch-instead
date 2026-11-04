package com.lockin.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.lockin.app.MainActivity
import com.lockin.app.R
import com.lockin.app.data.Clock
import com.lockin.app.data.LinkRepository
import com.lockin.app.data.StatsRepository
import com.lockin.app.domain.SavedLink
import com.lockin.app.domain.StatsCalculator
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun links(): LinkRepository
    fun stats(): StatsRepository
    fun clock(): Clock
}

private data class WidgetData(val next: SavedLink?, val queued: Int, val streak: Int)

class LockinWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        provideContent {
            GlanceTheme { WidgetContent(data) }
        }
    }

    private suspend fun load(context: Context): WidgetData {
        val entry = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        val queue = entry.links().queue()
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(entry.clock().now()).atZone(zone).toLocalDate()
        val watched = entry.links().links.first().mapNotNull { it.watchedAt }
        val stats = StatsCalculator.calculate(entry.stats().events.first(), watched, today, zone)
        return WidgetData(queue.firstOrNull(), queue.size, stats.currentStreak)
    }

    companion object {
        private val SMALL = DpSize(110.dp, 110.dp)
        private val WIDE = DpSize(250.dp, 110.dp)
        const val LINK_ID_KEY = "link_id"
    }
}

@Composable
private fun WidgetContent(data: WidgetData) {
    val context = LocalContext.current
    val resources = context.resources
    val colors = GlanceTheme.colors
    val isWide = LocalSize.current.width >= 200.dp
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            resources.getQuantityString(R.plurals.widget_streak, data.streak, data.streak),
            style = TextStyle(color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
        Spacer(GlanceModifier.height(4.dp))
        val next = data.next
        val headline = next?.let { it.title ?: it.note.ifBlank { null } }
            ?: resources.getString(
                if (next == null) R.string.widget_empty else R.string.widget_next_untitled,
            )
        Text(
            headline,
            style = TextStyle(color = colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold),
            maxLines = if (isWide) 2 else 3,
        )
        if (next != null) {
            Spacer(GlanceModifier.height(8.dp))
            if (isWide) {
                Button(
                    text = resources.getString(R.string.intervention_watch),
                    onClick = actionStartActivity<WatchLinkActivity>(
                        actionParametersOf(ActionParameters.Key<Long>(LockinWidget.LINK_ID_KEY) to next.id),
                    ),
                    modifier = GlanceModifier.fillMaxWidth(),
                )
            } else {
                Text(
                    resources.getQuantityString(R.plurals.widget_queue, data.queued, data.queued),
                    style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

class LockinWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LockinWidget()
}
