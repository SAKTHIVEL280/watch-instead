package com.watchinstead.app.widget

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.watchinstead.app.R
import com.watchinstead.app.core.ui.openLink
import com.watchinstead.app.data.ApplicationScope
import com.watchinstead.app.data.LinkRepository
import com.watchinstead.app.data.settings.SettingsRepository
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
        val id = intent.getLongExtra(WatchInsteadWidget.LINK_ID_KEY, -1)
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
