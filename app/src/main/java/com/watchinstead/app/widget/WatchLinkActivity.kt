package com.lockin.app.widget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.lockin.app.R
import com.lockin.app.core.ui.openLink
import com.lockin.app.data.ApplicationScope
import com.lockin.app.data.LinkRepository
import com.lockin.app.data.settings.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Invisible step behind the widget's Watch button: the saved video may open in a guarded app. */
@AndroidEntryPoint
class WatchLinkActivity : ComponentActivity() {
    @Inject lateinit var links: LinkRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra(LockinWidget.LINK_ID_KEY, -1)
        lifecycleScope.launch {
            val link = links.find(id)
            if (link != null) {
                settings.grantCooldownPass()
                if (openLink(link.url)) {
                    appScope.launch { links.setWatched(link.id, true) }
                } else {
                    Toast.makeText(this@WatchLinkActivity, R.string.library_open_failed, Toast.LENGTH_SHORT).show()
                }
            }
            finish()
        }
    }
}
