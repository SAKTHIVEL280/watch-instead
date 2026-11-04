package com.lockin.app.feature

import com.lockin.app.domain.Category
import com.lockin.app.feature.capture.CaptureViewModel
import com.lockin.app.testing.FakeLinkRepository
import com.lockin.app.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CaptureViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()
    private val links = FakeLinkRepository()
    private val viewModel = CaptureViewModel(links)

    @Test fun `invalid text shows an error and saves nothing`() {
        viewModel.onInputChange("just some words")
        viewModel.save()
        assertTrue(viewModel.state.value.showInvalid)
        assertTrue(links.links.value.isEmpty())
    }

    @Test fun `share text is saved with the chosen category and note`() {
        viewModel.onInputChange("Watch this https://youtu.be/abc?si=x")
        viewModel.onCategoryChange(Category.EDUCATION)
        viewModel.onNoteChange("  focus  ")
        viewModel.save()
        assertTrue(viewModel.state.value.isSaved)
        val saved = links.links.value.single()
        assertEquals("https://www.youtube.com/watch?v=abc", saved.url)
        assertEquals(Category.EDUCATION, saved.category)
    }

    @Test fun `same video saved twice reports a duplicate`() {
        repeat(2) {
            viewModel.onInputChange("https://youtu.be/abc")
            viewModel.save()
            if (it == 0) viewModel.reset()
        }
        assertTrue(viewModel.state.value.isDuplicate)
        assertFalse(viewModel.state.value.isSaved)
        assertEquals(1, links.links.value.size)
    }
}
