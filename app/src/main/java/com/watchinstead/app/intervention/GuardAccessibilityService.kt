package com.watchinstead.app.intervention

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Watches for guarded apps coming to the foreground.
 *
 * Events are subscribed to only for guarded packages, plus the launcher when no cooldown or a focus schedule is set,
 * and window content is never requested. This is what the onboarding disclosure and the privacy
 * policy promise. Keep them in sync.
 */
@AndroidEntryPoint
class GuardAccessibilityService : AccessibilityService() {
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var clock: Clock

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var settings: GuardSettings? = null
    private var subscribedPackages: Set<String>? = null
    private var launcherPackages: Set<String> = emptySet()
    private var lastPromptAt = 0L

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = endSession()
    }

    override fun onServiceConnected() {
        launcherPackages = packageManager
            .queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
            .toSet()
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        scope.launch {
            // The phone may have shut down mid-session, which no event reports.
            settingsRepository.endSession()
            settingsRepository.settings.collect { latest ->
                settings = latest
                // Sessions end at the launcher, and focus hours run on sessions whatever the cooldown.
                val usesSessions = latest.cooldownMinutes == GuardSettings.NO_COOLDOWN ||
                    latest.focusSchedules.any { it.enabled }
                val wanted = if (usesSessions) latest.guardedPackages + launcherPackages else latest.guardedPackages
                val packages = if (latest.guardedPackages.isEmpty()) emptySet() else wanted
                if (packages != subscribedPackages) subscribe(packages)
            }
        }
    }

    private fun endSession() {
        if (settings?.sessionOpen == true) scope.launch { settingsRepository.endSession() }
    }

    private fun subscribe(packages: Set<String>) {
        val info = serviceInfo ?: return
        subscribedPackages = packages
        serviceInfo = info.apply {
            // Null or empty packageNames means every app, so an empty list must disable events instead.
            eventTypes = if (packages.isEmpty()) 0 else AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            packageNames = packages.toTypedArray()
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
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
            startActivity(InterventionActivity.intent(this, packageName))
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        unregisterReceiver(screenOffReceiver)
        scope.cancel()
        super.onDestroy()
    }
}
