package com.lockin.app.domain

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusPolicyTest {
    // 2026-11-02 is a Monday.
    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, 1 + day, hour, minute)
    private val study = FocusSchedule("s", "Study", setOf(1, 3), 9 * 60, 17 * 60)
    private val night = FocusSchedule("n", "Night", setOf(1), 22 * 60, 6 * 60)

    @Test fun `active only on chosen days inside the hours, end excluded`() {
        assertEquals(study, FocusPolicy.activeSchedule(listOf(study), at(1, 9)))
        assertEquals(study, FocusPolicy.activeSchedule(listOf(study), at(1, 16, 59)))
        assertNull(FocusPolicy.activeSchedule(listOf(study), at(1, 17)))
        assertNull(FocusPolicy.activeSchedule(listOf(study), at(1, 8, 59)))
        assertNull(FocusPolicy.activeSchedule(listOf(study), at(2, 10)))
    }

    @Test fun `overnight schedule carries into the next morning only`() {
        assertEquals(night, FocusPolicy.activeSchedule(listOf(night), at(1, 23)))
        assertEquals(night, FocusPolicy.activeSchedule(listOf(night), at(2, 5, 59)))
        assertNull(FocusPolicy.activeSchedule(listOf(night), at(2, 6)))
        assertNull(FocusPolicy.activeSchedule(listOf(night), at(2, 23)))
        assertNull(FocusPolicy.activeSchedule(listOf(night), at(1, 3)))
    }

    @Test fun `overnight schedule from Sunday wraps to Monday`() {
        val sunday = night.copy(days = setOf(7))
        assertEquals(sunday, FocusPolicy.activeSchedule(listOf(sunday), at(1, 3)))
    }

    @Test fun `disabled schedules never apply`() =
        assertNull(FocusPolicy.activeSchedule(listOf(study.copy(enabled = false)), at(1, 10)))

    @Test fun `validation rejects unusable schedules`() {
        assertTrue(FocusPolicy.isValid(study))
        assertFalse(FocusPolicy.isValid(study.copy(name = " ")))
        assertFalse(FocusPolicy.isValid(study.copy(days = emptySet())))
        assertFalse(FocusPolicy.isValid(study.copy(days = setOf(8))))
        assertFalse(FocusPolicy.isValid(study.copy(endMinute = study.startMinute)))
        assertFalse(FocusPolicy.isValid(study.copy(endMinute = 1_440)))
    }
}
