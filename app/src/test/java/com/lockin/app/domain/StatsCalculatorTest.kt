package com.lockin.app.domain

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsCalculatorTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 11, 5)

    private fun millis(daysAgo: Long, hour: Int = 12) =
        today.minusDays(daysAgo).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun event(daysAgo: Long, outcome: PromptOutcome, pkg: String = "insta", reason: OpenReason? = null) =
        PromptEvent(millis(daysAgo), pkg, outcome, reason)

    private fun calculate(events: List<PromptEvent>, watched: List<Long> = emptyList()) =
        StatsCalculator.calculate(events, watched, today, zone)

    @Test fun `no data is all zero with seven empty days`() {
        val stats = calculate(emptyList())
        assertEquals(0, stats.currentStreak)
        assertEquals(7, stats.days.size)
        assertEquals(today, stats.days.last().date)
        assertFalse(stats.hasData)
    }

    @Test fun `streak counts consecutive days and today being empty does not break it`() {
        val stats = calculate(listOf(1L, 2L, 3L, 5L).map { event(it, PromptOutcome.WENT_HOME) })
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
    }

    @Test fun `streak ends after a full missed day but best is remembered`() {
        val stats = calculate(listOf(2L, 3L).map { event(it, PromptOutcome.WATCHED) })
        assertEquals(0, stats.currentStreak)
        assertEquals(2, stats.bestStreak)
    }

    @Test fun `continuing anyway never earns a streak day`() {
        val stats = calculate(listOf(event(0, PromptOutcome.CONTINUED)))
        assertEquals(0, stats.currentStreak)
        assertEquals(1, stats.weekContinued)
        assertTrue(stats.hasData)
    }

    @Test fun `videos watched from the library count towards the streak and the week`() {
        val stats = calculate(emptyList(), watched = listOf(millis(0), millis(30)))
        assertEquals(1, stats.currentStreak)
        assertEquals(1, stats.weekWatched)
    }

    @Test fun `week excludes the eighth day back and groups by local day`() {
        val stats = calculate(
            listOf(event(6, PromptOutcome.WENT_HOME), event(7, PromptOutcome.WENT_HOME), event(0, PromptOutcome.WATCHED)),
        )
        assertEquals(2, stats.weekResisted)
        assertEquals(1, stats.days.first().resisted)
        assertEquals(1, stats.days.last().resisted)
    }

    @Test fun `late evening stays on the local date`() {
        val india = ZoneId.of("Asia/Kolkata")
        val lateNight = today.atTime(23, 30).atZone(india).toInstant().toEpochMilli()
        val stats = StatsCalculator.calculate(
            listOf(PromptEvent(lateNight, "a", PromptOutcome.WENT_HOME)), emptyList(), today, india,
        )
        assertEquals(1, stats.days.last().resisted)
    }

    @Test fun `reasons and apps are ranked by count`() {
        val stats = calculate(
            listOf(
                event(0, PromptOutcome.CONTINUED, "tiktok", OpenReason.BORED),
                event(1, PromptOutcome.WENT_HOME, "tiktok", OpenReason.BORED),
                event(1, PromptOutcome.WENT_HOME, "insta", OpenReason.HABIT),
            ),
        )
        assertEquals(OpenReason.BORED to 2, stats.reasons.first())
        assertEquals("tiktok" to 2, stats.apps.first())
    }
}
