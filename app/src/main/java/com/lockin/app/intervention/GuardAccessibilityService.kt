package com.lockin.app.intervention

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.lockin.app.data.Clock
import com.lockin.app.data.settings.SettingsRepository
import com.lockin.app.domain.GuardSettings
import com.lockin.app.domain.InterventionPolicy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Watches for guarded apps coming to the foreground.
 *
 * Events are subscribed to only for guarded packages, plus the launcher when no cooldown is set,
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
                val noCooldown = latest.cooldownMinutes == GuardSettings.NO_COOLDOWN
                val wanted = if (noCooldown) latest.guardedPackages + launcherPackages else latest.guardedPackages
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
        if (InterventionPolicy.shouldIntervene(packageName, this.packageName, current, now, lastPromptAt)) {
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
