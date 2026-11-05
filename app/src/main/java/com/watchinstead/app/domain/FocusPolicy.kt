package com.watchinstead.app.domain

import java.time.LocalDateTime

object FocusPolicy {
    private const val MINUTES_PER_DAY = 1_440

    /** First enabled schedule covering [now]. Overnight schedules belong to the day they start on. */
    fun activeSchedule(schedules: List<FocusSchedule>, now: LocalDateTime): FocusSchedule? {
        val today = now.dayOfWeek.value
        val yesterday = now.dayOfWeek.minus(1).value
        val minute = now.hour * 60 + now.minute
        return schedules.firstOrNull { schedule ->
            val start = schedule.startMinute
            val end = schedule.endMinute
            when {
                !schedule.enabled -> false
                start < end -> today in schedule.days && minute >= start && minute < end
                start > end ->
                    (today in schedule.days && minute >= start) || (yesterday in schedule.days && minute < end)
                else -> false
            }
        }
    }

    fun isValid(schedule: FocusSchedule): Boolean =
        schedule.name.isNotBlank() &&
            schedule.name.length <= GuardSettings.MAX_SCHEDULE_NAME_LENGTH &&
            schedule.days.isNotEmpty() &&
            schedule.days.all { it in 1..7 } &&
            schedule.startMinute in 0 until MINUTES_PER_DAY &&
            schedule.endMinute in 0 until MINUTES_PER_DAY &&
            schedule.startMinute != schedule.endMinute
}
