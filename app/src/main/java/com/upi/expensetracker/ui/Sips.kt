package com.upi.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.upi.expensetracker.data.Sip
import com.upi.expensetracker.ui.theme.SpendRed
import com.upi.expensetracker.util.TimeRanges

@Composable
fun SipsScreen(viewModel: ExpenseViewModel, modifier: Modifier = Modifier) {
    val sips by viewModel.sips.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val monthKey = remember { TimeRanges.currentMonthKey() }

    LaunchedEffect(Unit) { viewModel.refreshBalance() }

    var editing by remember { mutableStateOf<Sip?>(null) }
    var adding by remember { mutableStateOf(false) }
    var editingBalance by remember { mutableStateOf(false) }

    val monthlyTotal = sips.filter { it.active }.sumOf { it.amount }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            BalanceCardSip(
                balance = balance,
                monthlyTotal = monthlyTotal,
                onEdit = { editingBalance = true }
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Your SIPs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { adding = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Add SIP")
                }
            }
            Spacer(Modifier.height(4.dp))
            if (sips.isEmpty()) {
                Text(
                    "Add your SIPs with their monthly date. The app warns you 2 days before " +
                        "a date if your balance looks too low, so a SIP won't bounce.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
        items(sips, key = { it.id }) { sip ->
            SipRow(sip, paidThisMonth = sip.lastPaidMonth == monthKey) { editing = sip }
        }
    }

    if (adding) {
        SipDialog(
            existing = null,
            onDismiss = { adding = false },
            onSave = { name, amount, day, _ ->
                viewModel.addSip(name, amount, day)
                adding = false
            },
            onDelete = {}
        )
    }

    editing?.let { sip ->
        SipDialog(
            existing = sip,
            onDismiss = { editing = null },
            onSave = { name, amount, day, active ->
                viewModel.updateSip(sip.copy(name = name, amount = amount, dayOfMonth = day, active = active))
                editing = null
            },
            onDelete = {
                viewModel.deleteSip(sip)
                editing = null
            }
        )
    }

    if (editingBalance) {
        BalanceDialog(
            current = balance,
            onDismiss = { editingBalance = false },
            onSave = { value ->
                viewModel.setBalance(value)
                editingBalance = false
            }
        )
    }
}

@Composable
private fun BalanceCardSip(balance: Double?, monthlyTotal: Double, onEdit: () -> Unit) {
    val insufficient = balance != null && balance < monthlyTotal
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Known account balance",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                if (balance != null) money(balance) else "Not set",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Monthly SIP total: ${money(monthlyTotal)}",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (insufficient) {
                Text(
                    "\u26A0 Balance is below your monthly SIP total.",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onEdit) { Text("Update balance") }
        }
    }
}

@Composable
private fun SipRow(sip: Sip, paidThisMonth: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(sip.name, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(
                    "${money(sip.amount)}  \u00B7  every ${sip.dayOfMonth}${daySuffix(sip.dayOfMonth)}" +
                        if (!sip.active) "  \u00B7  paused" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (paidThisMonth) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Paid this month",
                    tint = com.upi.expensetracker.ui.theme.IncomeGreen,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

private fun daySuffix(d: Int): String = when {
    d in 11..13 -> "th"
    d % 10 == 1 -> "st"
    d % 10 == 2 -> "nd"
    d % 10 == 3 -> "rd"
    else -> "th"
}

@Composable
private fun SipDialog(
    existing: Sip?,
    onDismiss: () -> Unit,
    onSave: (String, Double, Int, Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var amount by remember { mutableStateOf(existing?.let { amountText(it.amount) } ?: "") }
    var day by remember { mutableStateOf(existing?.dayOfMonth?.toString() ?: "") }
    var active by remember { mutableStateOf(existing?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull()
                val d = day.toIntOrNull()
                if (name.isNotBlank() && amt != null && amt > 0 && d != null && d in 1..31) {
                    onSave(name.trim(), amt, d, active)
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text(if (existing == null) "Add SIP" else "Edit SIP") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Fund name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = sanitizeAmount(it) },
                    label = { Text("Amount (\u20B9)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = day,
                    onValueChange = { day = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Day of month (1-31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (existing != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilterChip(
                            selected = active,
                            onClick = { active = !active },
                            label = { Text(if (active) "Active" else "Paused") }
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = onDelete,
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = SpendRed
                            )
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun BalanceDialog(current: Double?, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var text by remember { mutableStateOf(current?.let { amountText(it) } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { text.toDoubleOrNull()?.let(onSave) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Update balance") },
        text = {
            Column {
                Text(
                    "Set your current bank balance. It also updates automatically from " +
                        "bank SMS that include an available balance.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = sanitizeAmount(it) },
                    label = { Text("Balance (\u20B9)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
