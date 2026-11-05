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

    @Test fun `editing corrects the note and category of the same video`() {
        viewModel.onInputChange("https://example.com/a")
        viewModel.onNoteChange("teh typo")
        viewModel.save()
        viewModel.reset()
        val saved = links.links.value.single()

        viewModel.startEditing(saved)
        viewModel.onNoteChange("the fix")
        viewModel.onCategoryChange(Category.PRODUCTIVITY)
        viewModel.save()

        assertTrue(viewModel.state.value.isSaved)
        val edited = links.links.value.single()
        assertEquals(saved.id, edited.id)
        assertEquals("the fix", edited.note)
        assertEquals(Category.PRODUCTIVITY, edited.category)
    }

    @Test fun `editing a link into another saved video is refused`() {
        listOf("https://example.com/a", "https://example.com/b").forEach {
            viewModel.onInputChange(it)
            viewModel.save()
            viewModel.reset()
        }
        viewModel.startEditing(links.links.value.last())
        viewModel.onInputChange("https://example.com/a")
        viewModel.save()
        assertTrue(viewModel.state.value.isDuplicate)
        assertEquals("https://example.com/b", links.links.value.last().url)
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
