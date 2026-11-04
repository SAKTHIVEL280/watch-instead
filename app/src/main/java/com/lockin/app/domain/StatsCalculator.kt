package com.lockin.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DayStat(val date: LocalDate, val resisted: Int)

data class Stats(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val weekResisted: Int = 0,
    val weekWatched: Int = 0,
    val weekContinued: Int = 0,
    val days: List<DayStat> = emptyList(),
    val reasons: List<Pair<OpenReason, Int>> = emptyList(),
    val apps: List<Pair<String, Int>> = emptyList(),
) {
    val hasData: Boolean get() = bestStreak > 0 || weekContinued > 0
}

/** All figures are derived here, in the user's time zone, so that they can be tested without Android. */
object StatsCalculator {
    private const val WEEK_DAYS = 7L
    private const val TOP_APPS = 3

    /**
     * [watchedTimes] comes from saved links, so that videos finished from the library count too.
     * A day joins the streak when a prompt did not end in "continue anyway", or a video was watched.
     */
    fun calculate(events: List<PromptEvent>, watchedTimes: List<Long>, today: LocalDate, zone: ZoneId): Stats {
        fun dateOf(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

        val resistedDates = events.filter { it.outcome != PromptOutcome.CONTINUED }.map { dateOf(it.timestamp) }
        val watchedDates = watchedTimes.map(::dateOf)
        val activeDays = (resistedDates + watchedDates).toSet()

        val weekStart = today.minusDays(WEEK_DAYS - 1)
        fun inWeek(date: LocalDate) = !date.isBefore(weekStart) && !date.isAfter(today)
        val weekEvents = events.filter { inWeek(dateOf(it.timestamp)) }
        val resistedByDay = resistedDates.filter(::inWeek).groupingBy { it }.eachCount()

        return Stats(
            currentStreak = currentStreak(activeDays, today),
            bestStreak = bestStreak(activeDays),
            weekResisted = resistedByDay.values.sum(),
            weekWatched = watchedDates.count(::inWeek),
            weekContinued = weekEvents.count { it.outcome == PromptOutcome.CONTINUED },
            days = (0 until WEEK_DAYS).map { offset ->
                val date = weekStart.plusDays(offset)
                DayStat(date, resistedByDay[date] ?: 0)
            },
            reasons = weekEvents.mapNotNull { it.reason }.groupingBy { it }.eachCount()
                .toList().sortedByDescending { it.second },
            apps = weekEvents.groupingBy { it.packageName }.eachCount()
                .toList().sortedByDescending { it.second }.take(TOP_APPS),
        )
    }

    /** A streak survives until the end of the next day, so that it does not read 0 every morning. */
    private fun currentStreak(activeDays: Set<LocalDate>, today: LocalDate): Int {
        var day = if (today in activeDays) today else today.minusDays(1)
        var length = 0
        while (day in activeDays) {
            length++
            day = day.minusDays(1)
        }
        return length
    }

    private fun bestStreak(activeDays: Set<LocalDate>): Int {
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in activeDays.sorted()) {
            run = if (previous?.plusDays(1) == day) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        return best
    }
}
