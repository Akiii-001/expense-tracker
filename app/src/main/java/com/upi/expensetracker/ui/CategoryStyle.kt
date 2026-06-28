package com.upi.expensetracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.upi.expensetracker.data.CategoryIcon

/** User-chosen styles (category name -> icon/color override), provided at the app root. */
val LocalCategoryStyles = compositionLocalOf { emptyMap<String, CategoryIcon>() }

/** Selectable colors for category customization. */
object Palette {
    val colors: List<String> = listOf(
        "#EF6C00", "#D32F2F", "#C2185B", "#8E24AA", "#5E35B1", "#3949AB",
        "#1E88E5", "#039BE5", "#00ACC1", "#00897B", "#43A047", "#7CB342",
        "#F9A825", "#FB8C00", "#6D4C41", "#546E7A", "#4F46E5", "#1B873F"
    )

    fun parse(hex: String): Color? =
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()
}

/**
 * Resolves a category's style, honoring user-chosen icon and color overrides
 * if set, otherwise falling back to the built-in defaults.
 */
@Composable
fun categoryStyleFor(category: String): CategoryStyle.Style {
    val overrides = LocalCategoryStyles.current
    val base = CategoryStyle.of(category)
    val o = overrides[category]
    val icon = o?.iconKey?.takeIf { it.isNotBlank() }?.let { IconPack.icon(it) } ?: base.icon
    val color = o?.colorHex?.let { Palette.parse(it) } ?: base.color
    return CategoryStyle.Style(icon, color)
}

/** Icon + accent color for each category, used across the UI. */
object CategoryStyle {

    data class Style(val icon: ImageVector, val color: Color)

    private val map: Map<String, Style> = mapOf(
        "Food" to Style(Icons.Filled.Fastfood, Color(0xFFEF6C00)),
        "Groceries" to Style(Icons.Filled.ShoppingCart, Color(0xFF2E7D32)),
        "Travel" to Style(Icons.Filled.DirectionsCar, Color(0xFF1565C0)),
        "Transport" to Style(Icons.Filled.DirectionsBus, Color(0xFF0277BD)),
        "Fuel" to Style(Icons.Filled.LocalGasStation, Color(0xFF6D4C41)),
        "Shopping" to Style(Icons.Filled.ShoppingBag, Color(0xFFC2185B)),
        "Bills" to Style(Icons.Filled.Receipt, Color(0xFF00838F)),
        "Rent" to Style(Icons.Filled.Home, Color(0xFF5D4037)),
        "Entertainment" to Style(Icons.Filled.Movie, Color(0xFF7B1FA2)),
        "Subscriptions" to Style(Icons.Filled.Subscriptions, Color(0xFF512DA8)),
        "Health" to Style(Icons.Filled.LocalHospital, Color(0xFFD32F2F)),
        "Education" to Style(Icons.Filled.School, Color(0xFF303F9F)),
        "Personal Care" to Style(Icons.Filled.Spa, Color(0xFF00897B)),
        "Gifts" to Style(Icons.Filled.CardGiftcard, Color(0xFFD81B60)),
        "Investments" to Style(Icons.Filled.TrendingUp, Color(0xFF388E3C)),
        "Family" to Style(Icons.Filled.FamilyRestroom, Color(0xFF8E24AA)),
        "Team Outing" to Style(Icons.Filled.Groups, Color(0xFFEF6C00)),
        "Reimbursement" to Style(Icons.Filled.CurrencyExchange, Color(0xFF1B873F)),
        "Electricity" to Style(Icons.Filled.Bolt, Color(0xFFF9A825)),
        "Vacation" to Style(Icons.Filled.BeachAccess, Color(0xFF00ACC1)),
        "Other" to Style(Icons.Filled.Category, Color(0xFF607D8B)),
        // Income
        "Income" to Style(Icons.Filled.Payments, Color(0xFF1B873F)),
        "Salary" to Style(Icons.Filled.AccountBalance, Color(0xFF1B873F)),
        "Refund" to Style(Icons.Filled.Undo, Color(0xFF00897B)),
        "Cashback" to Style(Icons.Filled.Savings, Color(0xFF2E7D32)),
        "Transfer In" to Style(Icons.Filled.AccountBalanceWallet, Color(0xFF0277BD)),
        "Interest" to Style(Icons.Filled.TrendingUp, Color(0xFF388E3C)),
        "Other Income" to Style(Icons.Filled.Redeem, Color(0xFF607D8B)),
        "Uncategorized" to Style(Icons.Filled.HelpOutline, Color(0xFF9E9E9E))
    )

    private val fallback = Style(Icons.Filled.Category, Color(0xFF607D8B))

    fun of(category: String): Style = map[category] ?: fallback
}
