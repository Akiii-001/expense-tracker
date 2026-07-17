package com.upi.expensetracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.upi.expensetracker.data.AppDatabase
import com.upi.expensetracker.notify.Notifications
import com.upi.expensetracker.util.BalanceCalculator
import com.upi.expensetracker.util.TimeRanges
import java.util.Calendar
import kotlin.math.min

/**
 * Runs about once a day. If any SIP is due within the next 2 days and the latest
 * known balance can't cover the upcoming SIP total, it posts a warning so the
 * user can top up and avoid a bounce charge.
 */
class SipCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val dao = AppDatabase.get(ctx).transactionDao()
        val balance = BalanceCalculator.currentBalance(dao)
        val monthKey = TimeRanges.currentMonthKey()

        val upcoming = dao.activeSips().filter { sip ->
            sip.lastPaidMonth != monthKey && daysUntil(sip.dayOfMonth) in 0..2
        }
        if (upcoming.isEmpty()) return Result.success()

        val needed = upcoming.sumOf { it.amount }
        if (balance < needed) {
            val names = upcoming.joinToString(", ") { it.name }
            Notifications.showSipAlert(
                ctx,
                "Low balance for upcoming SIP",
                "SIP(s) due within 2 days total \u20B9%.0f (%s), but your balance is only \u20B9%.0f. Top up to avoid a bounce charge."
                    .format(needed, names, balance)
            )
        }
        return Result.success()
    }

    private fun daysUntil(day: Int): Int {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        fun dueFor(monthOffset: Int): Calendar {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, monthOffset)
            c.set(Calendar.DAY_OF_MONTH, min(day, c.getActualMaximum(Calendar.DAY_OF_MONTH)))
            c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
            return c
        }
        var due = dueFor(0)
        if (due.before(today)) due = dueFor(1)
        return ((due.timeInMillis - today.timeInMillis) / 86_400_000L).toInt()
    }
}
