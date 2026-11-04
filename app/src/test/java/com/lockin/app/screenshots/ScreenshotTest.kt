package com.lockin.app.screenshots

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.lockin.app.R
import com.lockin.app.core.theme.LockinTheme
import androidx.test.core.app.ApplicationProvider
import com.lockin.app.data.apps.InstalledAppsRepository
import com.lockin.app.domain.Accent
import com.lockin.app.domain.FocusSchedule
import com.lockin.app.domain.OpenReason
import com.lockin.app.domain.PromptEvent
import com.lockin.app.domain.PromptOutcome
import com.lockin.app.feature.focus.FocusSchedulesScreen
import com.lockin.app.feature.focus.FocusViewModel
import com.lockin.app.feature.messages.MessagesScreen
import com.lockin.app.feature.messages.MessagesViewModel
import com.lockin.app.feature.stats.StatsScreen
import com.lockin.app.feature.stats.StatsViewModel
import com.lockin.app.domain.Appearance
import com.lockin.app.domain.Category
import com.lockin.app.domain.ParsedLink
import com.lockin.app.domain.Platform
import com.lockin.app.domain.SavedLink
import com.lockin.app.domain.ThemeMode
import com.lockin.app.data.backup.BackupRepository
import com.lockin.app.feature.backup.BackupViewModel
import com.lockin.app.feature.capture.CaptureForm
import com.lockin.app.feature.capture.CaptureState
import com.lockin.app.feature.capture.CaptureViewModel
import com.lockin.app.feature.library.LibraryScreen
import com.lockin.app.feature.library.LibraryViewModel
import com.lockin.app.feature.onboarding.OnboardingScreen
import com.lockin.app.feature.onboarding.OnboardingViewModel
import com.lockin.app.feature.settings.SettingsScreen
import com.lockin.app.feature.settings.SettingsViewModel
import com.lockin.app.intervention.InterventionScreen
import com.lockin.app.intervention.InterventionState
import com.lockin.app.testing.FakeLinkRepository
import com.lockin.app.testing.FakeSettingsRepository
import com.lockin.app.testing.FakeStatsRepository
import com.lockin.app.testing.MainDispatcherRule
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders key screens to build/screenshots so that visual changes can be reviewed without a device. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w400dp-h840dp-xxhdpi", application = android.app.Application::class)
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val light = Appearance(ThemeMode.LIGHT, dynamicColor = false)
    private val dark = Appearance(ThemeMode.DARK, dynamicColor = false, accent = Accent.TEAL)
    private val links = FakeLinkRepository()
    private val settings = FakeSettingsRepository()
    private val stats = FakeStatsRepository()
    private val video = SavedLink(
        1, "https://www.youtube.com/watch?v=a", Platform.YOUTUBE, Category.MOTIVATION,
        note = "Rewatch before exams", title = "How to stop wasting your mornings", createdAt = 1,
    )

    private fun seed() = runBlocking {
        settings.setGuardedPackages(setOf("insta"))
        links.save(ParsedLink("https://www.youtube.com/watch?v=a", Platform.YOUTUBE), Category.MOTIVATION, "Rewatch before exams")
        links.save(ParsedLink("https://www.instagram.com/reel/b", Platform.INSTAGRAM), Category.EDUCATION, "")
    }

    private fun shot(name: String, appearance: Appearance, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { LockinTheme(appearance, content) }
        compose.mainClock.advanceTimeBy(2_000)
        compose.onRoot().captureRoboImage("build/screenshots/$name.png")
    }

    private fun capture() = CaptureViewModel(links)
    private fun backup() = BackupViewModel(BackupRepository(links, settings, stats))

    private fun library(name: String, appearance: Appearance) {
        seed()
        val viewModel = LibraryViewModel(links, settings)
        val capture = capture()
        shot(name, appearance) { LibraryScreen({}, {}, {}, viewModel, capture) }
    }

    @Test fun library() = library("library", light)
    @Test fun libraryDark() = library("library_dark", dark)
    @Test fun settingsLight() {
        val viewModel = SettingsViewModel(settings, links, stats)
        val backup = backup()
        shot("settings", light.copy(accent = Accent.ROSE)) { SettingsScreen({}, {}, {}, {}, {}, {}, viewModel, backup) }
    }
    @Test fun onboardingWelcome() {
        val viewModel = OnboardingViewModel(links, settings)
        val capture = capture()
        val backup = backup()
        shot("onboarding_welcome", light) { OnboardingScreen(viewModel, capture, backup) }
    }
    @Test fun onboardingVideos() {
        seed()
        val viewModel = OnboardingViewModel(links, settings)
        viewModel.next()
        val capture = capture()
        val backup = backup()
        shot("onboarding_videos", light) { OnboardingScreen(viewModel, capture, backup) }
    }
    @Test fun intervention() = shot("intervention", light) {
        InterventionScreen(
            InterventionState(false, "Instagram", listOf(video, video.copy(id = 2)), secondsLeft = 3, totalSeconds = 5),
            {}, {}, {}, {},
        )
    }
    @Test fun settingsAmber() {
        val viewModel = SettingsViewModel(settings, links, stats)
        val appearance = Appearance(ThemeMode.LIGHT, dynamicColor = false, accent = Accent.AMBER)
        settings.appearance.value = appearance
        val backup = backup()
        shot("settings_amber", appearance) { SettingsScreen({}, {}, {}, {}, {}, {}, viewModel, backup) }
    }
    @Test fun pureBlack() {
        val viewModel = SettingsViewModel(settings, links, stats)
        val appearance = Appearance(ThemeMode.DARK, dynamicColor = false, accent = Accent.ROSE, pureBlack = true)
        settings.appearance.value = appearance
        val backup = backup()
        shot("settings_pure_black", appearance) { SettingsScreen({}, {}, {}, {}, {}, {}, viewModel, backup) }
    }
    @Test fun interventionLargeFont() = shot("intervention_large_font", light) {
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
            InterventionScreen(
                InterventionState(false, "Instagram", listOf(video, video.copy(id = 2)), secondsLeft = 3, totalSeconds = 5),
                {}, {}, {}, {},
            )
        }
    }
    @Test fun captureForm() = shot("capture_form", light) {
        Surface { CaptureForm(CaptureState(category = Category.CONSISTENCY), {}, {}, {}, {}) }
    }
    @Test fun interventionFull() = shot("intervention_full", light) {
        InterventionScreen(
            InterventionState(
                false, "Instagram", listOf(video, video.copy(id = 2)), secondsLeft = 28, totalSeconds = 30,
                personalMessage = "Exams in 12 days. You've got this.", focusName = "Study",
                askWhy = true, reason = OpenReason.BORED,
            ),
            {}, {}, {}, {},
        )
    }
    @Test fun statsScreen() {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        stats.events.value = listOf(0L, 1L, 1L, 2L, 3L, 3L, 3L, 6L).mapIndexed { index, ago ->
            val outcome = if (index == 7) PromptOutcome.CONTINUED else PromptOutcome.WENT_HOME
            PromptEvent(now - ago * day - index, if (index % 3 == 0) "tiktok" else "insta", outcome, OpenReason.entries[index % 4])
        }
        val viewModel = StatsViewModel(
            stats, links, InstalledAppsRepository(ApplicationProvider.getApplicationContext()), { now },
        )
        shot("stats", dark) { StatsScreen({}, viewModel) }
    }
    @Test fun focusScreen() {
        runBlocking { settings.setFocusSchedules(listOf(FocusSchedule("a", "Study", setOf(1, 2, 3, 4, 5), 540, 1020), FocusSchedule("b", "Sleep", setOf(1, 2, 3, 4, 5, 6, 7), 1380, 360, false))) }
        val viewModel = FocusViewModel(settings)
        shot("focus", light) { FocusSchedulesScreen({}, viewModel) }
    }
    @Test fun messagesScreen() {
        runBlocking { settings.setPersonalMessages(setOf("Exams in 12 days.", "Your future self is watching.")) }
        val viewModel = MessagesViewModel(settings)
        shot("messages", light) { MessagesScreen({}, viewModel) }
    }
    @Test fun editForm() = shot("capture_edit", light) {
        Surface { CaptureForm(CaptureState(input = video.url, note = "Fixed note", editingId = 1), {}, {}, {}, {}) }
    }
    @Test fun launcherIcon() = shot("launcher_icon", light) {
        Box(Modifier.size(216.dp).clip(CircleShape)) {
            Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.size(216.dp))
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(216.dp))
        }
    }
    @Config(qualifiers = "night")
    @Test fun launcherIconNight() = shot("launcher_icon_night", light) {
        Box(Modifier.size(216.dp).clip(CircleShape)) {
            Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.size(216.dp))
            Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size(216.dp))
        }
    }
    @Test fun launcherIconThemed() = shot("launcher_icon_themed", light) {
        Image(
            painterResource(R.drawable.ic_launcher_monochrome), null,
            Modifier.size(216.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(androidx.compose.ui.graphics.Color.DarkGray),
        )
    }
    @Test fun interventionEmpty() = shot("intervention_empty", dark) {
        InterventionScreen(InterventionState(false, "TikTok"), {}, {}, {}, {})
    }
}
