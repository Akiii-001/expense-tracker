package com.upi.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.upi.expensetracker.data.Transaction
import com.upi.expensetracker.data.TxnType
import com.upi.expensetracker.ui.theme.IncomeGreen
import com.upi.expensetracker.ui.theme.SpendRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar() {
    TopAppBar(
        title = { Text("My Money", fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
fun AppBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
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
    val customCategories by viewModel.customCategories.collectAsState()
    val monthSpend by viewModel.monthSpend.collectAsState()
    val monthIncome by viewModel.monthIncome.collectAsState()
    val weekSpend by viewModel.weekSpend.collectAsState()

    var editing by remember { mutableStateOf<Transaction?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    LaunchedEffect(focusTransactionId, transactions) {
        if (focusTransactionId > 0 && editing == null) {
            transactions.firstOrNull { it.id == focusTransactionId }?.let { editing = it }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
        ) {
            item {
                BalanceCard(income = monthIncome, spend = monthSpend, weekSpend = weekSpend)
                Spacer(Modifier.height(20.dp))
                Text(
                    "Recent activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
            }

            if (transactions.isEmpty()) {
                item { EmptyState() }
            } else {
                items(transactions, key = { it.id }) { txn ->
                    TransactionRow(txn) { editing = txn }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }

    editing?.let { txn ->
        EditTransactionSheet(
            transaction = txn,
            customCategories = customCategories,
            onDismiss = { editing = null },
            onSave = { category, note ->
                viewModel.updateTransaction(txn, category, note)
                editing = null
            },
            onDelete = {
                viewModel.deleteTransaction(txn)
                editing = null
            }
        )
    }

    if (showAdd) {
        AddTransactionSheet(
            customCategories = customCategories,
            onDismiss = { showAdd = false },
            onAdd = { amount, payee, note, type, category ->
                viewModel.addManual(amount, payee, note, type, category)
                showAdd = false
            }
        )
    }
}

@Composable
private fun BalanceCard(income: Double, spend: Double, weekSpend: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Balance this month",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                money(income - spend),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniStat("Received", money(income), IncomeGreen, Modifier.weight(1f))
                MiniStat("Spent", money(spend), SpendRed, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Spent this week: ${money(weekSpend)}",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(12.dp)
    ) {
        Column {
            Text(label, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f), style = MaterialTheme.typography.labelMedium)
            Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.ReceiptLong,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "No transactions yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "They appear automatically from bank SMS, or tap Add to log one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp)
        )
    }
}

@Composable
private fun TransactionRow(txn: Transaction, onClick: () -> Unit) {
    val df = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val isCredit = txn.type == TxnType.CREDIT
    val style = CategoryStyle.of(txn.category)

    // Title is "what for" (note) when present, else the payee/merchant.
    val title = txn.note.ifBlank { txn.payee }
    val subtitle = buildString {
        append(txn.category)
        if (txn.note.isNotBlank() && txn.payee.isNotBlank()) append("  \u00B7  ${txn.payee}")
        append("  \u00B7  ${df.format(Date(txn.timestamp))}")
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(style.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                (if (isCredit) "+" else "-") + money(txn.amount),
                fontWeight = FontWeight.Bold,
                color = if (isCredit) IncomeGreen else SpendRed
            )
        }
    }
}
