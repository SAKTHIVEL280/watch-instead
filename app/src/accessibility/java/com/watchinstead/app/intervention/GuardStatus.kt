package com.watchinstead.app.intervention

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.watchinstead.app.domain.GuardSettings

object GuardStatus {
    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(context, GuardAccessibilityService::class.java)
        return context.getSystemService(AccessibilityManager::class.java)
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any {
                val info = it.resolveInfo.serviceInfo
                ComponentName(info.packageName, info.name) == expected
            }
    }

    /** Android runs the Accessibility service itself, so there is nothing to start. */
    @Suppress("UNUSED_PARAMETER")
    fun sync(context: Context, settings: GuardSettings) = Unit

    fun openSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
