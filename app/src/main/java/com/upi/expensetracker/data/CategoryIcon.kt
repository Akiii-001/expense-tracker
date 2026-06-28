package com.upi.expensetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-chosen style for a category: an icon (from the icon pack) and/or a
 * color (hex string like "#4F46E5"). Either may be null to keep the default.
 */
@Entity(tableName = "category_icons")
data class CategoryIcon(
    @PrimaryKey val category: String,
    val iconKey: String? = null,
    val colorHex: String? = null
)
