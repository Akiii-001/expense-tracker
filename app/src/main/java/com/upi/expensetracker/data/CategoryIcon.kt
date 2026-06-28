package com.upi.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** A user-chosen icon (from the icon pack) for a category. */
@Entity(tableName = "category_icons")
data class CategoryIcon(
    @PrimaryKey val category: String,
    val iconKey: String
)
