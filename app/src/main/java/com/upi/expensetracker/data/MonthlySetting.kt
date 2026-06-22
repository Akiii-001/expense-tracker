package com.upi.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-month settings. Currently holds the opening balance you carry into a
 * month (e.g. money left over from the previous month). monthKey is "yyyy-MM".
 */
@Entity(tableName = "monthly_settings")
data class MonthlySetting(
    @PrimaryKey val monthKey: String,
    val openingBalance: Double
)
