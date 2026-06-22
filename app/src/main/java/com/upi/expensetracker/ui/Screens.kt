package com.upi.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upi.expensetracker.data.Transaction
import com.upi.expensetracker.data.TxnType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val IncomeGreen = Color(0xFF1B873F)
private val SpendRed = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar() {
    TopAppBar(title = { Text("UPI Expense Tracker") })
}

@Composable
fun AppBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selected == 0,
            onClick = { onSelect(0) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Activity") }
        )
        NavigationBarItem(
            selected = selected == 1,
            onClick = { onSelect(1) },
            icon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
            label = { Text("Reports") }
        )
    }
}

internal fun money(value: Double): String = "\u20B9%,.0f".format(value)

@Composable
fun HomeScreen(
    viewModel: ExpenseViewModel,
    focusTransactionId: Long,
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.transactions.collectAsState()
    val monthSpend by viewModel.monthSpend.collectAsState()
    val monthIncome by viewModel.monthIncome.collectAsState()
    val weekSpend by viewModel.weekSpend.collectAsState()

    var editing by remember { mutableStateOf<Transaction?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    // If opened from a notification, jump straight to that transaction.
    LaunchedEffect(focusTransactionId, transactions) {
        if (focusTransactionId > 0 && editing == null) {
            transactions.firstOrNull { it.id == focusTransactionId }?.let { editing = it }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BalanceCard(income = monthIncome, spend = monthSpend, weekSpend = weekSpend)

            if (transactions.isEmpty()) {
                Text(
                    "Nothing yet. Transactions appear here automatically when a bank SMS arrives, or add one with +.",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                items(transactions) { txn ->
                    TransactionRow(txn) { editing = txn }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAdd = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add transaction")
        }
    }

    editing?.let { txn ->
        CategoryPickerDialog(
            transaction = txn,
            onDismiss = { editing = null },
            onPick = { category ->
                viewModel.setCategory(txn, category)
                editing = null
            }
        )
    }

    if (showAdd) {
        AddTransactionDialog(
            onDismiss = { showAdd = false },
            onAdd = { amount, payee, type, category ->
                viewModel.addManual(amount, payee, type, category)
                showAdd = false
            }
        )
    }
}

@Composable
private fun BalanceCard(income: Double, spend: Double, weekSpend: Double) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("This month", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Figure("Received", income, IncomeGreen)
                Figure("Spent", spend, SpendRed)
                Figure("Balance", income - spend, MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "Spent this week: ${money(weekSpend)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun Figure(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            money(amount),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun TransactionRow(txn: Transaction, onClick: () -> Unit) {
    val df = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val isCredit = txn.type == TxnType.CREDIT
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(txn.payee, fontWeight = FontWeight.SemiBold)
                Text(
                    "${txn.category}  \u00B7  ${df.format(Date(txn.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                (if (isCredit) "+" else "-") + money(txn.amount),
                fontWeight = FontWeight.Bold,
                color = if (isCredit) IncomeGreen else SpendRed
            )
        }
    }
}
