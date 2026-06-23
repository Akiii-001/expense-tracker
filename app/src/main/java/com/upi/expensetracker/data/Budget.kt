package com.upi.expensetracker.data

import androidx.room.Entity

/**
 * A spending target for a category in a specific month.
 * monthKey is "yyyy-MM", e.g. monthKey = "2026-07", category = "Food", amount = 5000.
 */
@Entity(tableName = "budgets", primaryKeys = ["monthKey", "category"])
data class Budget(
    val monthKey: String,
    val category: String,
    val amount: Double
)
