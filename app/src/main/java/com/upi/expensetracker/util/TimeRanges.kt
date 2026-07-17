package com.upi.expensetracker.util

import java.util.Calendar

/** Helpers for the start-of-week and start-of-month timestamps. */
object TimeRanges {

    /** Current month as "yyyy-MM", used to key per-month settings. */
    fun currentMonthKey(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return "%04d-%02d".format(year, month)
    }

    /** "yyyy-MM" for a given timestamp. */
    fun monthKeyOf(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    /** Day-of-month for a given timestamp. */
    fun dayOfMonth(timestamp: Long): Int {
        return Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_MONTH)
    }

    /** Start-of-month timestamp for a "yyyy-MM" key. */
    fun startOfMonthKey(monthKey: String): Long {
        val parts = monthKey.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: return 0L
        val month = parts.getOrNull(1)?.toIntOrNull() ?: return 0L
        val cal = Calendar.getInstance().apply {
            clear()
            set(year, month - 1, 1, 0, 0, 0)
        }
        return cal.timeInMillis
    }

    private fun monthCalendar(offset: Int): Calendar =
        Calendar.getInstance().apply {
            add(Calendar.MONTH, offset)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    /** [start, end) timestamps for a month, offset from the current month (0 = this month, -1 = last). */
    fun monthRange(offset: Int): Pair<Long, Long> {
        val start = monthCalendar(offset)
        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        return start.timeInMillis to end.timeInMillis
    }

    /** "yyyy-MM" for a month offset from the current month. */
    fun monthKeyForOffset(offset: Int): String {
        val cal = monthCalendar(offset)
        return "%04d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    /** Human label e.g. "June 2026" for a month offset. */
    fun monthLabel(offset: Int): String {
        val cal = monthCalendar(offset)
        val fmt = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
        return fmt.format(cal.time)
    }

    fun startOfWeek(): Long {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        return cal.atStartOfDay()
    }

    fun startOfMonth(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return cal.atStartOfDay()
    }

    private fun Calendar.atStartOfDay(): Long {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        return timeInMillis
    }
}
