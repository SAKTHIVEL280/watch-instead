package com.watchinstead.app.intervention

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.watchinstead.app.MainActivity
import com.watchinstead.app.R
import com.watchinstead.app.data.Clock
import com.watchinstead.app.data.settings.SettingsRepository
import com.watchinstead.app.domain.FocusPolicy
import com.watchinstead.app.domain.GuardSettings
import com.watchinstead.app.domain.InterventionPolicy
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Watches for guarded apps coming to the foreground, using usage events polled while the screen is on.
 *
 * Accessibility is deliberately not used: payment apps refuse to run beside an Accessibility service
 * from an app installed outside a store. Usage access reports every app, so only guarded packages and
 * the launcher are acted on, nothing else is kept, and screen content is never available. This is what
 * the onboarding disclosure and the privacy policy promise. Keep them in sync.
 */
@AndroidEntryPoint
class GuardMonitorService : Service() {
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var clock: Clock

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var settings: GuardSettings? = null
    private var launcherPackages: Set<String> = emptySet()
    private var lastPromptAt = 0L
    private var polling: Job? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON) {
                startPolling()
            } else {
                polling?.cancel()
                endSession()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        launcherPackages = packageManager
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
            .toSet()
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter(Intent.ACTION_SCREEN_ON).apply { addAction(Intent.ACTION_SCREEN_OFF) },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        scope.launch {
            // The phone may have shut down mid-session, which no event reports.
            settingsRepository.endSession()
            settingsRepository.settings.collect { settings = it }
        }
        if (getSystemService(PowerManager::class.java).isInteractive) startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } catch (_: IllegalStateException) {
            // A system restart can land outside the foreground start window. Opening the app restarts this.
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startPolling() {
        polling?.cancel()
        polling = scope.launch {
            val usageStats = getSystemService(UsageStatsManager::class.java)
            var polledTo = clock.now()
            while (isActive) {
                delay(POLL_MILLIS)
                // A clock moved backwards would otherwise make the range empty until it caught up.
                val now = clock.now()
                val packageName = withContext(Dispatchers.IO) {
                    latestForegroundPackage(usageStats, minOf(polledTo, now), now)
                }
                polledTo = now
                if (packageName != null) onForeground(packageName)
            }
        }
    }

    @Suppress("DEPRECATION") // ACTIVITY_RESUMED is the same value but needs API 29.
    private fun latestForegroundPackage(usageStats: UsageStatsManager, from: Long, to: Long): String? {
        // Null until the user first unlocks after boot.
        val events = usageStats.queryEvents(from, to) ?: return null
        val event = UsageEvents.Event()
        var latest: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) latest = event.packageName
        }
        return latest
    }

    private fun onForeground(packageName: String) {
        val current = settings ?: return
        if (packageName in launcherPackages && packageName !in current.guardedPackages) {
            endSession()
            return
        }
        val now = clock.now()
        val localNow = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val focusActive = FocusPolicy.activeSchedule(current.focusSchedules, localNow) != null
        val intervene = InterventionPolicy.shouldIntervene(
            packageName, this.packageName, current, now, lastPromptAt, focusActive,
        )
        if (intervene) {
            lastPromptAt = now
            // Allowed from the background only because the app can draw over other apps.
            startActivity(InterventionActivity.intent(this, packageName))
        }
    }

    private fun endSession() {
        if (settings?.sessionOpen == true) scope.launch { settingsRepository.endSession() }
    }

    private fun notification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.guard_notification_channel),
            NotificationManager.IMPORTANCE_MIN,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(getString(R.string.guard_notification_title))
            .setContentText(getString(R.string.guard_notification_body))
            .setContentIntent(openApp)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    override fun onDestroy() {
        unregisterReceiver(screenReceiver)
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        /** Slowest a prompt can lag behind opening an app. Polling stops while the screen is off. */
        const val POLL_MILLIS = 750L
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "app_guard"
    }
}
