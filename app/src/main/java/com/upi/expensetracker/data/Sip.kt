package com.upi.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A recurring SIP (Systematic Investment Plan) the user wants to monitor.
 * dayOfMonth is the debit day (1-31). lastPaidMonth ("yyyy-MM") marks the month
 * it was last detected as paid, so alerts skip it once done.
 */
@Entity(tableName = "sips")
data class Sip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val dayOfMonth: Int,
    val lastPaidMonth: String? = null,
    val active: Boolean = true
)
