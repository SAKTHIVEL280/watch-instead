package com.watchinstead.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.watchinstead.app.core.theme.setWatchInsteadContent
import com.watchinstead.app.data.settings.SettingsRepository
import javax.inject.Inject
import com.watchinstead.app.feature.onboarding.OnboardingScreen
import com.watchinstead.app.intervention.GuardMonitorService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.onboardingComplete.value == null }
        // Resuming covers coming back from the permission screens, and is when a foreground service may start.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                settings.settings.collect { GuardMonitorService.sync(this@MainActivity, it) }
            }
        }
        setWatchInsteadContent(settings) {
            val complete by viewModel.onboardingComplete.collectAsStateWithLifecycle()
            AnimatedContent(complete, label = "app entry") { state ->
                when (state) {
                    true -> WatchInsteadNavHost()
                    false -> OnboardingScreen()
                    null -> Unit
                }
            }
        }
    }
}
