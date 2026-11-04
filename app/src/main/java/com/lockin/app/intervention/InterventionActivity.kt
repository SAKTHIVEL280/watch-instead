package com.lockin.app.intervention

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.lockin.app.R
import com.lockin.app.core.theme.setLockinContent
import com.lockin.app.data.settings.SettingsRepository
import javax.inject.Inject
import com.lockin.app.core.ui.openLink
import com.lockin.app.domain.PromptOutcome
import com.lockin.app.domain.SavedLink
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class InterventionActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository

    private val viewModel: InterventionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.start(intent.getStringExtra(EXTRA_PACKAGE))
        setLockinContent(settings) {
            val state by viewModel.state.collectAsStateWithLifecycle()
            // Back must never drop the user straight into the app they were stopped from opening.
            BackHandler(onBack = ::goHome)
            InterventionScreen(
                state = state,
                onWatch = ::watch,
                onNext = viewModel::showNext,
                onGoHome = ::goHome,
                onContinue = ::continueToApp,
                onSelectReason = viewModel::selectReason,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.start(intent.getStringExtra(EXTRA_PACKAGE))
    }

    private fun watch(link: SavedLink) {
        lifecycleScope.launch {
            viewModel.passForWatching()
            if (openLink(link.url)) {
                viewModel.markWatched(link)
                viewModel.record(PromptOutcome.WATCHED)
                finish()
            } else {
                Toast.makeText(this@InterventionActivity, R.string.intervention_open_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goHome() {
        viewModel.record(PromptOutcome.WENT_HOME)
        lifecycleScope.launch {
            viewModel.passForGoingHome()
            startActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            finish()
        }
    }

    private fun continueToApp() {
        viewModel.record(PromptOutcome.CONTINUED)
        lifecycleScope.launch {
            viewModel.passForContinue()
            finish()
        }
    }

    companion object {
        private const val EXTRA_PACKAGE = "guarded_package"

        fun intent(context: Context, packageName: String): Intent =
            Intent(context, InterventionActivity::class.java)
                .putExtra(EXTRA_PACKAGE, packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
