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
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE timestamp >= :start AND timestamp < :end AND type = :type"
    )
    fun observeTotalBetween(start: Long, end: Long, type: String): Flow<Double>

    // Sum for a category within a time range (used for budget checks).
    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions " +
            "WHERE timestamp >= :start AND timestamp < :end AND type = :type AND category = :category"
    )
    suspend fun sumForCategoryBetween(start: Long, end: Long, type: String, category: String): Double

    @Query(
        "SELECT category, COALESCE(SUM(amount), 0) AS total FROM transactions " +
            "WHERE timestamp >= :since AND type = :type " +
            "GROUP BY category ORDER BY total DESC"
    )
    fun observeCategoryTotalsSince(since: Long, type: String): Flow<List<CategoryTotal>>

    @Query(
        "SELECT category, COALESCE(SUM(amount), 0) AS total FROM transactions " +
            "WHERE timestamp >= :start AND timestamp < :end AND type = :type " +
            "GROUP BY category ORDER BY total DESC"
    )
    fun observeCategoryTotalsBetween(start: Long, end: Long, type: String): Flow<List<CategoryTotal>>

    // Transactions within a range, for export.
    @Query("SELECT * FROM transactions WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp")
    suspend fun transactionsBetween(start: Long, end: Long): List<Transaction>

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

    @Query("SELECT * FROM monthly_settings")
    fun observeAllSettings(): Flow<List<MonthlySetting>>

    // Budgets (per month) --------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey")
    fun observeBudgetsForMonth(monthKey: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey")
    suspend fun budgetsForMonth(monthKey: String): List<Budget>

    @Query("SELECT amount FROM budgets WHERE monthKey = :monthKey AND category = :category")
    suspend fun budgetFor(monthKey: String, category: String): Double?
}
