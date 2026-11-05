package com.lockin.app.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(val packageName: String, val label: String, val isSuggested: Boolean)

@Singleton
class InstalledAppsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val packageManager: PackageManager = context.packageManager
    private val iconCache = LruCache<String, ImageBitmap>(ICON_CACHE_SIZE)

    /** Launchable apps, suggested distractions first. The launcher itself is left out. */
    suspend fun load(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val excluded = packageManager.queryIntentActivities(homeIntent, 0)
            .map { it.activityInfo.packageName }.toSet() + context.packageName
        packageManager.queryIntentActivities(launcherIntent, 0)
            .filter { it.activityInfo.packageName !in excluded }
            .distinctBy { it.activityInfo.packageName }
            .map {
                val name = it.activityInfo.packageName
                InstalledApp(name, it.loadLabel(packageManager).toString(), name in SUGGESTED_PACKAGES)
            }
            .sortedWith(compareBy({ !it.isSuggested }, { it.label.lowercase() }))
    }

    /** Cached, because rows re-enter composition constantly while the app list scrolls. */
    suspend fun icon(packageName: String): ImageBitmap? = iconCache[packageName]
        ?: withContext(Dispatchers.IO) {
            runCatching {
                packageManager.getApplicationIcon(packageName).toBitmap(ICON_PX, ICON_PX).asImageBitmap()
            }.getOrNull()?.also { iconCache.put(packageName, it) }
        }

    fun label(packageName: String): String? = runCatching {
        val info = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(info).toString()
    }.getOrNull()

    private companion object {
        const val ICON_PX = 120
        const val ICON_CACHE_SIZE = 200
        val SUGGESTED_PACKAGES = setOf(
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.google.android.youtube",
            "com.twitter.android",
            "com.snapchat.android",
            "com.facebook.katana",
            "com.reddit.frontpage",
        )
    }
}
