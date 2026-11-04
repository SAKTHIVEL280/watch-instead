package com.lockin.app.intervention

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
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
 * Events are subscribed to only for guarded packages and window content is never requested, which
 * is what the onboarding disclosure and the privacy policy promise. Keep them in sync.
 */
@AndroidEntryPoint
class GuardAccessibilityService : AccessibilityService() {
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var clock: Clock

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var settings: GuardSettings? = null
    private var subscribedPackages: Set<String>? = null
    private var lastPromptAt = 0L

    override fun onServiceConnected() {
        scope.launch {
            settingsRepository.settings.collect { latest ->
                settings = latest
                if (latest.guardedPackages != subscribedPackages) subscribe(latest.guardedPackages)
            }
        }
    }

    private fun subscribe(packages: Set<String>) {
        subscribedPackages = packages
        serviceInfo = serviceInfo.apply {
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
        val now = clock.now()
        if (InterventionPolicy.shouldIntervene(packageName, this.packageName, current, now, lastPromptAt)) {
            lastPromptAt = now
            startActivity(InterventionActivity.intent(this, packageName))
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
