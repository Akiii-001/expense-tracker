package com.upi.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single UPI transaction captured from a bank SMS (or added manually).
 * Can be a DEBIT (money out) or a CREDIT (money in, e.g. salary).
 * Stored only in the app's private on-device database.
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val payee: String,
    val type: String = TxnType.DEBIT,
    val category: String = Categories.UNCATEGORIZED,
    val note: String = "",
    val timestamp: Long,
    // Raw sender id (e.g. "AX-HDFCBK"). Kept only to help debugging/parsing,
    // never the full message body.
    val sender: String = ""
)

object TxnType {
    const val DEBIT = "DEBIT"
    const val CREDIT = "CREDIT"
}

object Categories {
    const val UNCATEGORIZED = "Uncategorized"
    const val INCOME = "Income"

    /** Categories shown for money you spend. */
    val EXPENSE = listOf(
        "Travel",
        "Food",
        "Groceries",
        "Shopping",
        "Bills",
        "Entertainment",
        "Health",
        "Other"
    )

    /** Categories shown for money you receive. */
    val CREDIT = listOf(
        "Salary",
        "Refund",
        "Cashback",
        "Transfer In",
        "Other Income"
    )

    fun forType(type: String): List<String> =
        if (type == TxnType.CREDIT) CREDIT else EXPENSE
}
