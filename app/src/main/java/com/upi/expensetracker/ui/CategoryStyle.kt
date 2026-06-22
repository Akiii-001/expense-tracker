package com.upi.expensetracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

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
