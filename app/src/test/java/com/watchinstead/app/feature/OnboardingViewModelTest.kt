package com.watchinstead.app.feature

import com.watchinstead.app.domain.Category
import com.watchinstead.app.domain.ParsedLink
import com.watchinstead.app.domain.Platform
import com.watchinstead.app.feature.onboarding.OnboardingStep
import com.watchinstead.app.feature.onboarding.OnboardingViewModel
import com.watchinstead.app.testing.FakeLinkRepository
import com.watchinstead.app.testing.FakeSettingsRepository
import com.watchinstead.app.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val links = FakeLinkRepository()
    private val settings = FakeSettingsRepository()
    private val viewModel by lazy { OnboardingViewModel(links, settings) }

    private var videosAdded = 0

    private suspend fun addVideos(count: Int) = repeat(count) {
        links.save(ParsedLink("https://a.com/${videosAdded++}", Platform.OTHER), Category.OTHER, "")
    }

    @Test fun `videos step stays locked until two videos exist`() = runTest {
        viewModel.next()
        addVideos(1)
        viewModel.next()
        assertEquals(OnboardingStep.VIDEOS, viewModel.state.value.step)

        addVideos(1)
        viewModel.next()
        assertEquals(OnboardingStep.APPS, viewModel.state.value.step)
    }

    @Test fun `removing a video locks the step again`() = runTest {
        viewModel.next()
        addVideos(2)
        viewModel.remove(viewModel.state.value.links.first())
        assertFalse(viewModel.state.value.canContinue)
    }

    @Test fun `finish is refused without videos and apps`() = runTest {
        viewModel.finish()
        assertFalse(settings.onboardingComplete.value)

        addVideos(2)
        viewModel.finish()
        assertFalse(settings.onboardingComplete.value)

        settings.setGuardedPackages(setOf("insta"))
        viewModel.finish()
        assertTrue(settings.onboardingComplete.value)
    }

    @Test fun `back steps through and stops at welcome`() {
        viewModel.next()
        assertTrue(viewModel.back())
        assertFalse(viewModel.back())
    }
}
