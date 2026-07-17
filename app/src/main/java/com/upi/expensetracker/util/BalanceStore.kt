package com.upi.expensetracker.util

import android.content.Context

/** Stores the latest known bank balance (from SMS "Avl bal" or manual entry). */
object BalanceStore {

    private fun prefs(context: Context) =
        context.getSharedPreferences("balance", Context.MODE_PRIVATE)

    /** Latest known balance, or null if never set. */
    fun get(context: Context): Double? {
        val p = prefs(context)
        return if (p.contains("value")) p.getFloat("value", -1f).toDouble() else null
    }

    fun updatedAt(context: Context): Long = prefs(context).getLong("updatedAt", 0L)

    fun set(context: Context, value: Double) {
        prefs(context).edit()
            .putFloat("value", value.toFloat())
            .putLong("updatedAt", System.currentTimeMillis())
            .apply()
    }
}
