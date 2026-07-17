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

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransaction(id: Long): Transaction?

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

    @Query(
        "SELECT note FROM transactions WHERE payee = :payee AND ABS(amount - :amount) < 0.005 " +
            "AND note != '' ORDER BY timestamp DESC LIMIT 1"
    )
    suspend fun lastNoteForPayeeAmount(payee: String, amount: Double): String?

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

    // Backup / restore -----------------------------------------------------

    @Query("SELECT * FROM transactions")
    suspend fun allTransactions(): List<Transaction>

    @Query("SELECT * FROM custom_categories")
    suspend fun allCustomCategories(): List<CustomCategory>

    @Query("SELECT * FROM monthly_settings")
    suspend fun allSettings(): List<MonthlySetting>

    @Query("SELECT * FROM budgets")
    suspend fun allBudgets(): List<Budget>

    @Query("SELECT * FROM category_icons")
    suspend fun allCategoryIcons(): List<CategoryIcon>

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM custom_categories")
    suspend fun clearCustomCategories()

    @Query("DELETE FROM monthly_settings")
    suspend fun clearSettings()

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()

    @Query("DELETE FROM category_icons")
    suspend fun clearCategoryIcons()

    // Category icons -------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCategoryStyle(icon: CategoryIcon)

    @Query("SELECT * FROM category_icons")
    fun observeCategoryIcons(): Flow<List<CategoryIcon>>

    // SIPs -----------------------------------------------------------------

    @Insert
    suspend fun insertSip(sip: Sip): Long

    @Update
    suspend fun updateSip(sip: Sip)

    @Delete
    suspend fun deleteSip(sip: Sip)

    @Query("SELECT * FROM sips ORDER BY dayOfMonth")
    fun observeSips(): Flow<List<Sip>>

    @Query("SELECT * FROM sips WHERE active = 1")
    suspend fun activeSips(): List<Sip>

    @Query("SELECT * FROM sips")
    suspend fun allSips(): List<Sip>
}
