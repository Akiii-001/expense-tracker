package com.upi.expensetracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.upi.expensetracker.data.AppDatabase
import com.upi.expensetracker.data.CategoryTotal
import com.upi.expensetracker.data.Transaction
import com.upi.expensetracker.data.TxnType
import com.upi.expensetracker.util.TimeRanges
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.get(app).transactionDao()

    private val weekStart = TimeRanges.startOfWeek()
    private val monthStart = TimeRanges.startOfMonth()

    val transactions = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usedCategories = dao.observeUsedCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
            dao.update(transaction.copy(category = category, note = note))
        }
    }

    fun addManual(amount: Double, payee: String, note: String, type: String, category: String) {
        viewModelScope.launch {
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
}
