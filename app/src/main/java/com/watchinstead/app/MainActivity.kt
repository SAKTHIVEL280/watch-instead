package com.lockin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lockin.app.core.theme.setLockinContent
import com.lockin.app.data.settings.SettingsRepository
import javax.inject.Inject
import com.lockin.app.feature.onboarding.OnboardingScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { viewModel.onboardingComplete.value == null }
        setLockinContent(settings) {
            val complete by viewModel.onboardingComplete.collectAsStateWithLifecycle()
            AnimatedContent(complete, label = "app entry") { state ->
                when (state) {
                    true -> LockinNavHost()
                    false -> OnboardingScreen()
                    null -> Unit
                }
            }
        }
    }
}
