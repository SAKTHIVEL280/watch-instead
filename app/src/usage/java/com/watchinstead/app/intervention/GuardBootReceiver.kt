package com.watchinstead.app.intervention

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.watchinstead.app.data.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** These two broadcasts may start the foreground service from the background. Updates stop the service too. */
@AndroidEntryPoint
class GuardBootReceiver : BroadcastReceiver() {
    @Inject lateinit var settings: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                GuardStatus.sync(context, settings.settings.first())
            } finally {
                pending.finish()
            }
        }
    }
}
