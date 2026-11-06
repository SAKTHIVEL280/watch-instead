package com.watchinstead.app.intervention

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.watchinstead.app.domain.GuardSettings

object GuardStatus {
    fun isEnabled(context: Context): Boolean = hasUsageAccess(context) && Settings.canDrawOverlays(context)

    /** Opens the page for whichever permission is still missing, so tapping again moves on to the next. */
    fun openSettings(context: Context) {
        val intent = if (hasUsageAccess(context)) {
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
        } else {
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    /** Starts or stops the monitor to match [settings] and the granted permissions. */
    fun sync(context: Context, settings: GuardSettings) {
        val intent = Intent(context, GuardMonitorService::class.java)
        val wanted = settings.protectionEnabled && settings.guardedPackages.isNotEmpty() && isEnabled(context)
        if (wanted) ContextCompat.startForegroundService(context, intent) else context.stopService(intent)
    }

    private fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java)
        val uid = Process.myUid()
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, uid, context.packageName)
        }
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            context.checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }
}
