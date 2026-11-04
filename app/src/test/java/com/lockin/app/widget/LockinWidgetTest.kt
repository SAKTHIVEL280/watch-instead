package com.lockin.app.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import com.lockin.app.domain.Category
import com.lockin.app.domain.Platform
import com.lockin.app.domain.SavedLink
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class LockinWidgetTest {
    private val link = SavedLink(
        id = 1, url = "https://example.com/a", platform = Platform.OTHER, category = Category.OTHER,
        title = "Great talk", createdAt = 1,
    )

    private fun render(size: DpSize, data: WidgetData, check: androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest.() -> Unit) =
        runGlanceAppWidgetUnitTest {
            setAppWidgetSize(size)
            setContext(ApplicationProvider.getApplicationContext())
            provideComposable { GlanceTheme { WidgetContent(data) } }
            check()
        }

    @Test fun `wide widget shows streak, video and a watch button`() =
        render(DpSize(250.dp, 110.dp), WidgetData(link, 3, 4)) {
            onNode(hasText("4 day streak")).assertExists()
            onNode(hasText("Great talk")).assertExists()
            onNode(hasText("Watch now")).assertExists()
        }

    @Test fun `small widget shows the queue size instead of a button`() =
        render(DpSize(110.dp, 110.dp), WidgetData(link, 3, 4)) {
            onNode(hasText("3 to watch")).assertExists()
            onNode(hasText("Watch now")).assertDoesNotExist()
        }

    @Test fun `empty queue invites saving and offers no button`() =
        render(DpSize(250.dp, 110.dp), WidgetData(null, 0, 0)) {
            onNode(hasText("Save a video to get started")).assertExists()
            onNode(hasText("Watch now")).assertDoesNotExist()
        }

    @Test fun `video without a title falls back to its note, then a generic line`() {
        render(DpSize(250.dp, 110.dp), WidgetData(link.copy(title = null, note = "For exams"), 1, 0)) {
            onNode(hasText("For exams")).assertExists()
        }
        render(DpSize(250.dp, 110.dp), WidgetData(link.copy(title = null), 1, 0)) {
            onNode(hasText("Your next saved video")).assertExists()
        }
    }
}
