package com.upi.expensetracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(
    val category: String,
    val total: Double
)

@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<Transaction>>

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE timestamp >= :since AND type = :type"
    )
    fun observeTotalSince(since: Long, type: String): Flow<Double>

    @Query(
        "SELECT category, COALESCE(SUM(amount), 0) AS total FROM transactions " +
            "WHERE timestamp >= :since AND type = :type " +
            "GROUP BY category ORDER BY total DESC"
    )
    fun observeCategoryTotalsSince(since: Long, type: String): Flow<List<CategoryTotal>>

    // Auto-learn helpers ---------------------------------------------------

    // The category last used for this payee (ignoring uncategorized).
    @Query(
        "SELECT category FROM transactions WHERE payee = :payee " +
            "AND category != '${Categories.UNCATEGORIZED}' ORDER BY timestamp DESC LIMIT 1"
    )
    suspend fun lastCategoryForPayee(payee: String): String?

    // How many DIFFERENT categories this payee has ever had. If this is > 1,
    // the payee is inconsistent and we should NOT auto-guess its category.
    @Query(
        "SELECT COUNT(DISTINCT category) FROM transactions WHERE payee = :payee " +
            "AND category != '${Categories.UNCATEGORIZED}'"
    )
    suspend fun distinctCategoriesForPayee(payee: String): Int

    // Custom categories ----------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCustomCategory(category: CustomCategory)

    @Query("SELECT * FROM custom_categories ORDER BY name")
    fun observeCustomCategories(): Flow<List<CustomCategory>>

    // Opening balance ------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setMonthlySetting(setting: MonthlySetting)

    @Query("SELECT openingBalance FROM monthly_settings WHERE monthKey = :monthKey")
    fun observeOpeningBalance(monthKey: String): Flow<Double?>
}
