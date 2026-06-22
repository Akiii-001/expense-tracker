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
