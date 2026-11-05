package com.watchinstead.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueSelectorTest {
    private fun link(id: Long, createdAt: Long, shown: Long? = null, watched: Long? = null) = SavedLink(
        id = id, url = "u$id", platform = Platform.OTHER, category = Category.OTHER,
        createdAt = createdAt, lastShownAt = shown, watchedAt = watched,
    )

    @Test fun `never shown come first, oldest first, then least recently shown`() {
        val ordered = QueueSelector.order(
            listOf(link(1, 10, shown = 200), link(2, 30), link(3, 20), link(4, 5, shown = 100)),
        )
        assertEquals(listOf(3L, 2L, 4L, 1L), ordered.map { it.id })
    }

    @Test fun `watched links are excluded`() {
        assertEquals(emptyList<SavedLink>(), QueueSelector.order(listOf(link(1, 1, watched = 2))))
    }
}
