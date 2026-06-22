package com.upi.expensetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.upi.expensetracker.data.AppDatabase
import com.upi.expensetracker.data.Categories
import com.upi.expensetracker.data.CategoryTotal
import com.upi.expensetracker.data.CustomCategory
import com.upi.expensetracker.data.MonthlySetting
import com.upi.expensetracker.data.Transaction
import com.upi.expensetracker.data.TxnType
import com.upi.expensetracker.util.TimeRanges
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).transactionDao()

    private val weekStart = TimeRanges.startOfWeek()
    private val monthStart = TimeRanges.startOfMonth()
    private val monthKey = TimeRanges.currentMonthKey()

    val transactions = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Opening balance for the current month.
     * - If you've explicitly set it for this month, that value is used.
     * - Otherwise it auto-carries: it's the closing balance carried forward,
     *   i.e. the latest explicit opening you set in an earlier month plus every
     *   credit minus debit since then (or simply the net of all past
     *   transactions if you never set one).
     * You can always edit it to correct the exact amount.
     */
    val openingBalance = combine(
        dao.observeAll(),
        dao.observeAllSettings()
    ) { txns, settings ->
        computeOpening(txns, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private fun computeOpening(
        txns: List<Transaction>,
        settings: List<MonthlySetting>
    ): Double {
        val curStart = monthStart
        val applicable = settings.filter { it.monthKey <= monthKey }.maxByOrNull { it.monthKey }
        return when {
            applicable == null ->
                netBetween(txns, Long.MIN_VALUE, curStart)
            applicable.monthKey == monthKey ->
                applicable.openingBalance
            else ->
                applicable.openingBalance +
                    netBetween(txns, TimeRanges.startOfMonthKey(applicable.monthKey), curStart)
        }
    }

    private fun netBetween(txns: List<Transaction>, from: Long, until: Long): Double =
        txns.filter { it.timestamp in from until until }
            .sumOf { if (it.type == TxnType.CREDIT) it.amount else -it.amount }

    val customCategories = dao.observeCustomCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<CustomCategory>())

    val weekSpend = dao.observeTotalSince(weekStart, TxnType.DEBIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val monthSpend = dao.observeTotalSince(monthStart, TxnType.DEBIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val weekIncome = dao.observeTotalSince(weekStart, TxnType.CREDIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val monthIncome = dao.observeTotalSince(monthStart, TxnType.CREDIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val weekByCategory = dao.observeCategoryTotalsSince(weekStart, TxnType.DEBIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<CategoryTotal>())
    val monthByCategory = dao.observeCategoryTotalsSince(monthStart, TxnType.DEBIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<CategoryTotal>())

    /** Update category and/or note (the "what for") of an existing transaction. */
    fun updateTransaction(transaction: Transaction, category: String, note: String) {
        viewModelScope.launch {
            rememberCustomCategory(category, transaction.type)
            dao.update(transaction.copy(category = category, note = note))
        }
    }

    fun addManual(amount: Double, payee: String, note: String, type: String, category: String) {
        viewModelScope.launch {
            rememberCustomCategory(category, type)
            dao.insert(
                Transaction(
                    amount = amount,
                    payee = payee.ifBlank { if (type == TxnType.CREDIT) "Income" else "Expense" },
                    note = note,
                    type = type,
                    category = category,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch { dao.delete(transaction) }
    }

    fun setOpeningBalance(value: Double) {
        viewModelScope.launch {
            dao.setMonthlySetting(MonthlySetting(monthKey = monthKey, openingBalance = value))
        }
    }

    /** Persist a user-typed category so it stays available in the future. */
    private suspend fun rememberCustomCategory(category: String, type: String) {
        val predefined = Categories.forType(type)
        if (category.isNotBlank() &&
            category !in predefined &&
            category != Categories.UNCATEGORIZED &&
            category != Categories.INCOME
        ) {
            dao.insertCustomCategory(CustomCategory(name = category, type = type))
        }
    }
}
