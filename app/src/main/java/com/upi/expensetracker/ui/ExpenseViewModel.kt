package com.upi.expensetracker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.upi.expensetracker.data.AppDatabase
import com.upi.expensetracker.data.Budget
import com.upi.expensetracker.data.Categories
import com.upi.expensetracker.data.CategoryTotal
import com.upi.expensetracker.data.CustomCategory
import com.upi.expensetracker.data.MonthlySetting
import com.upi.expensetracker.data.Transaction
import com.upi.expensetracker.data.TxnType
import com.upi.expensetracker.notify.Notifications
import com.upi.expensetracker.util.Exporter
import com.upi.expensetracker.util.TimeRanges
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** All figures for one month, used by the Reports screen. */
data class ReportData(
    val offset: Int = 0,
    val label: String = "",
    val opening: Double = 0.0,
    val income: Double = 0.0,
    val spend: Double = 0.0,
    val byCategory: List<CategoryTotal> = emptyList(),
    val budgets: Map<String, Double> = emptyMap()
)

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).transactionDao()

    private val weekStart = TimeRanges.startOfWeek()
    private val monthStart = TimeRanges.startOfMonth()
    private val monthKey = TimeRanges.currentMonthKey()

    val transactions = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCategories = dao.observeCustomCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<CustomCategory>())

    val budgets = dao.observeBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList<Budget>())

    val openingBalance = combine(dao.observeAll(), dao.observeAllSettings()) { txns, settings ->
        computeOpeningFor(monthKey, monthStart, txns, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val weekSpend = dao.observeTotalSince(weekStart, TxnType.DEBIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val monthSpend = dao.observeTotalSince(monthStart, TxnType.DEBIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val monthIncome = dao.observeTotalSince(monthStart, TxnType.CREDIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // -------- Reports with month history --------

    private val reportOffset = MutableStateFlow(0)
    val reportMonthOffset = reportOffset.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val reportData: kotlinx.coroutines.flow.StateFlow<ReportData> =
        reportOffset.flatMapLatest { offset ->
            val (start, end) = TimeRanges.monthRange(offset)
            val key = TimeRanges.monthKeyForOffset(offset)
            combine(
                dao.observeTotalBetween(start, end, TxnType.CREDIT),
                dao.observeTotalBetween(start, end, TxnType.DEBIT),
                dao.observeCategoryTotalsBetween(start, end, TxnType.DEBIT),
                dao.observeBudgets(),
                combine(dao.observeAll(), dao.observeAllSettings()) { txns, settings ->
                    computeOpeningFor(key, start, txns, settings)
                }
            ) { income, spend, cats, budgetList, opening ->
                ReportData(
                    offset = offset,
                    label = TimeRanges.monthLabel(offset),
                    opening = opening,
                    income = income,
                    spend = spend,
                    byCategory = cats,
                    budgets = budgetList.associate { it.category to it.amount }
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportData(label = TimeRanges.monthLabel(0)))

    fun changeReportMonth(delta: Int) {
        // Don't allow going into the future beyond the current month.
        val next = (reportOffset.value + delta).coerceAtMost(0)
        reportOffset.value = next
    }

    // -------- Mutations --------

    fun updateTransaction(transaction: Transaction, category: String, note: String) {
        viewModelScope.launch {
            rememberCustomCategory(category, transaction.type)
            dao.update(transaction.copy(category = category, note = note))
            checkBudget(category, transaction.type)
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
            checkBudget(category, type)
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

    fun setBudget(category: String, amount: Double) {
        viewModelScope.launch {
            if (amount > 0) dao.setBudget(Budget(category, amount))
            else dao.deleteBudget(Budget(category, 0.0))
        }
    }

    /** Build a CSV or PDF for a month and return a shareable Uri (or null). */
    suspend fun export(offset: Int, pdf: Boolean): Uri? {
        val (start, end) = TimeRanges.monthRange(offset)
        val txns = dao.transactionsBetween(start, end)
        val label = TimeRanges.monthLabel(offset)
        val income = txns.filter { it.type == TxnType.CREDIT }.sumOf { it.amount }
        val spend = txns.filter { it.type == TxnType.DEBIT }.sumOf { it.amount }
        return if (pdf) {
            Exporter.writePdf(getApplication(), label, income, spend, txns)
        } else {
            Exporter.writeCsv(getApplication(), label, txns)
        }
    }

    // -------- Helpers --------

    private suspend fun checkBudget(category: String, type: String) {
        if (type != TxnType.DEBIT) return
        val budget = dao.budgetFor(category) ?: return
        if (budget <= 0) return
        val (start, end) = TimeRanges.monthRange(0)
        val spent = dao.sumForCategoryBetween(start, end, TxnType.DEBIT, category)
        when {
            spent >= budget ->
                Notifications.showBudgetAlert(getApplication(), category, spent, budget, over = true)
            spent >= budget * 0.8 ->
                Notifications.showBudgetAlert(getApplication(), category, spent, budget, over = false)
        }
    }

    private fun computeOpeningFor(
        key: String,
        start: Long,
        txns: List<Transaction>,
        settings: List<MonthlySetting>
    ): Double {
        val applicable = settings.filter { it.monthKey <= key }.maxByOrNull { it.monthKey }
        return when {
            applicable == null -> netBetween(txns, Long.MIN_VALUE, start)
            applicable.monthKey == key -> applicable.openingBalance
            else -> applicable.openingBalance +
                netBetween(txns, TimeRanges.startOfMonthKey(applicable.monthKey), start)
        }
    }

    private fun netBetween(txns: List<Transaction>, from: Long, until: Long): Double =
        txns.filter { it.timestamp in from until until }
            .sumOf { if (it.type == TxnType.CREDIT) it.amount else -it.amount }

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
