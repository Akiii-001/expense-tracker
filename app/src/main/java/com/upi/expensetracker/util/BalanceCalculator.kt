package com.upi.expensetracker.util

import com.upi.expensetracker.data.MonthlySetting
import com.upi.expensetracker.data.Transaction
import com.upi.expensetracker.data.TransactionDao
import com.upi.expensetracker.data.TxnType

/**
 * Computes the same "Balance this month" shown on the Activity tab:
 * opening balance for the current month + income this month - spend this month.
 * Used by the background SIP worker so it monitors the exact figure the user sees.
 */
object BalanceCalculator {

    suspend fun currentBalance(dao: TransactionDao): Double {
        val monthStart = TimeRanges.startOfMonth()
        val monthKey = TimeRanges.currentMonthKey()
        val txns = dao.allTransactions()
        val settings = dao.allSettings()

        val opening = computeOpening(monthKey, monthStart, txns, settings)
        val income = txns.filter { it.type == TxnType.CREDIT && it.timestamp >= monthStart }.sumOf { it.amount }
        val spend = txns.filter { it.type == TxnType.DEBIT && it.timestamp >= monthStart }.sumOf { it.amount }
        return opening + income - spend
    }

    private fun computeOpening(
        key: String,
        start: Long,
        txns: List<Transaction>,
        settings: List<MonthlySetting>
    ): Double {
        val applicable = settings.filter { it.monthKey <= key }.maxByOrNull { it.monthKey }
        return when {
            applicable == null -> netBetween(txns, Long.MIN_VALUE, start)
            applicable.monthKey == key -> applicable.openingBalance
            else -> applicable.openingBalance +
                netBetween(txns, TimeRanges.startOfMonthKey(applicable.monthKey), start)
        }
    }

    private fun netBetween(txns: List<Transaction>, from: Long, until: Long): Double =
        txns.filter { it.timestamp in from until until }
            .sumOf { if (it.type == TxnType.CREDIT) it.amount else -it.amount }
}
