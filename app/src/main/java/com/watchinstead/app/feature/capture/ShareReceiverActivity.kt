package com.watchinstead.app.feature.capture

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.watchinstead.app.R
import com.watchinstead.app.core.theme.setWatchInsteadContent
import com.watchinstead.app.data.settings.SettingsRepository
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint

/** Receives text shared from other apps and shows the capture sheet over them. */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository

    private val viewModel: CaptureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            viewModel.onInputChange(intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty())
        }
        setWatchInsteadContent(settings) {
            CaptureSheet(
                viewModel = viewModel,
                onDismiss = ::finish,
                onSaved = {
                    Toast.makeText(this, R.string.capture_saved, Toast.LENGTH_SHORT).show()
                    finish()
                },
            )
        }
    }
}
