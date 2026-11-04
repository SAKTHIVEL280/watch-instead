package com.lockin.app.feature.capture

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.lockin.app.R
import com.lockin.app.core.theme.setLockinContent
import com.lockin.app.data.settings.SettingsRepository
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
        setLockinContent(settings) {
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
