package com.upi.expensetracker.data

import androidx.room.Entity

/**
 * A category the user typed themselves. Stored separately so it persists even
 * if the transactions that used it are deleted. (A full app uninstall still
 * clears it, like all on-device data.)
 */
@Entity(tableName = "custom_categories", primaryKeys = ["name", "type"])
data class CustomCategory(
    val name: String,
    val type: String
)
