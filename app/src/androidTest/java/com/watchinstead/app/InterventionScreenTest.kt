package com.lockin.app

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.lockin.app.domain.Category
import com.lockin.app.domain.Platform
import com.lockin.app.domain.SavedLink
import com.lockin.app.intervention.InterventionScreen
import com.lockin.app.intervention.InterventionState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class InterventionScreenTest {
    @get:Rule val compose = createComposeRule()

    private val link = SavedLink(
        id = 1, url = "https://example.com/talk", platform = Platform.OTHER,
        category = Category.MOTIVATION, title = "Great talk", createdAt = 1,
    )

    @Test fun continueIsLockedDuringCountdown() {
        compose.setContent {
            InterventionScreen(
                InterventionState(isLoading = false, queue = listOf(link), secondsLeft = 3), {}, {}, {}, {},
            )
        }
        compose.onNodeWithText("Continue anyway (3)").assertIsNotEnabled()
    }

    @Test fun watchOpensTheCurrentVideo() {
        var watched: SavedLink? = null
        compose.setContent {
            InterventionScreen(
                InterventionState(isLoading = false, queue = listOf(link)), { watched = it }, {}, {}, {},
            )
        }
        compose.onNodeWithText("Watch now").performClick()
        assertEquals(link, watched)
    }
}
