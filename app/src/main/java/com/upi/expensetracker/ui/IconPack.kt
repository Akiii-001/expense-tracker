package com.upi.expensetracker.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BeachAccess
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector

/** A pack of selectable icons users can assign to any category. */
object IconPack {

    val icons: Map<String, ImageVector> = linkedMapOf(
        "food" to Icons.Filled.Fastfood,
        "restaurant" to Icons.Filled.Restaurant,
        "cafe" to Icons.Filled.LocalCafe,
        "coffee" to Icons.Filled.Coffee,
        "bar" to Icons.Filled.LocalBar,
        "pizza" to Icons.Filled.LocalPizza,
        "cake" to Icons.Filled.Cake,
        "groceries" to Icons.Filled.LocalGroceryStore,
        "cart" to Icons.Filled.ShoppingCart,
        "bag" to Icons.Filled.ShoppingBag,
        "store" to Icons.Filled.Store,
        "clothes" to Icons.Filled.Checkroom,
        "car" to Icons.Filled.DirectionsCar,
        "bus" to Icons.Filled.DirectionsBus,
        "train" to Icons.Filled.Train,
        "bike" to Icons.Filled.DirectionsBike,
        "scooter" to Icons.Filled.TwoWheeler,
        "flight" to Icons.Filled.Flight,
        "fuel" to Icons.Filled.LocalGasStation,
        "luggage" to Icons.Filled.Luggage,
        "vacation" to Icons.Filled.BeachAccess,
        "home" to Icons.Filled.Home,
        "furniture" to Icons.Filled.Chair,
        "electricity" to Icons.Filled.Bolt,
        "lightbulb" to Icons.Filled.Lightbulb,
        "water" to Icons.Filled.WaterDrop,
        "laundry" to Icons.Filled.LocalLaundryService,
        "cleaning" to Icons.Filled.CleaningServices,
        "wifi" to Icons.Filled.Wifi,
        "phone" to Icons.Filled.Phone,
        "bills" to Icons.Filled.Receipt,
        "card" to Icons.Filled.CreditCard,
        "bank" to Icons.Filled.AccountBalance,
        "wallet" to Icons.Filled.AccountBalanceWallet,
        "savings" to Icons.Filled.Savings,
        "invest" to Icons.Filled.TrendingUp,
        "exchange" to Icons.Filled.CurrencyExchange,
        "health" to Icons.Filled.LocalHospital,
        "medicine" to Icons.Filled.Medication,
        "fitness" to Icons.Filled.FitnessCenter,
        "heart" to Icons.Filled.Favorite,
        "spa" to Icons.Filled.Spa,
        "movie" to Icons.Filled.Movie,
        "music" to Icons.Filled.MusicNote,
        "games" to Icons.Filled.SportsEsports,
        "tv" to Icons.Filled.Tv,
        "subscriptions" to Icons.Filled.Subscriptions,
        "education" to Icons.Filled.School,
        "work" to Icons.Filled.Work,
        "business" to Icons.Filled.BusinessCenter,
        "family" to Icons.Filled.FamilyRestroom,
        "group" to Icons.Filled.Groups,
        "kids" to Icons.Filled.ChildCare,
        "pets" to Icons.Filled.Pets,
        "gift" to Icons.Filled.CardGiftcard,
        "reward" to Icons.Filled.Redeem,
        "celebration" to Icons.Filled.Celebration,
        "trophy" to Icons.Filled.EmojiEvents,
        "star" to Icons.Filled.Star,
        "cloud" to Icons.Filled.Cloud,
        "other" to Icons.Filled.Category
    )

    val keys: List<String> = icons.keys.toList()

    fun icon(key: String): ImageVector? = icons[key]
}
