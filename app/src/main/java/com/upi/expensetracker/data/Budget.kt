package com.upi.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A monthly spending target for a category (recurring every month).
 * e.g. category = "Food", amount = 5000.
 */
@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: String,
    val amount: Double
)
