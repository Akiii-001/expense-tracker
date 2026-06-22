package com.upi.expensetracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.upi.expensetracker.data.Categories
import com.upi.expensetracker.data.Transaction
import com.upi.expensetracker.data.TxnType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryPickerDialog(
    transaction: Transaction,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val isCredit = transaction.type == TxnType.CREDIT
    val title = if (isCredit) {
        "Where did \u20B9%.0f come from?".format(transaction.amount)
    } else {
        "Why did you pay \u20B9%.0f?".format(transaction.amount)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text(title) },
        text = {
            Column {
                Text(if (isCredit) "From: ${transaction.payee}" else "To: ${transaction.payee}")
                FlowRow(modifier = Modifier.padding(top = 12.dp)) {
                    Categories.forType(transaction.type).forEach { cat ->
                        AssistChip(
                            onClick = { onPick(cat) },
                            label = { Text(cat) },
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTransactionDialog(
    onDismiss: () -> Unit,
    onAdd: (Double, String, String, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var payee by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TxnType.DEBIT) }
    var category by remember { mutableStateOf(Categories.EXPENSE.first()) }

    val categories = Categories.forType(type)
    if (category !in categories) category = categories.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val value = amount.toDoubleOrNull()
                    if (value != null && value > 0) onAdd(value, payee, type, category)
                }
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Add transaction") },
        text = {
            Column {
                FlowRow {
                    FilterChip(
                        selected = type == TxnType.DEBIT,
                        onClick = { type = TxnType.DEBIT },
                        label = { Text("Spent") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = type == TxnType.CREDIT,
                        onClick = { type = TxnType.CREDIT },
                        label = { Text("Received") }
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (\u20B9)") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = payee,
                    onValueChange = { payee = it },
                    label = { Text(if (type == TxnType.CREDIT) "Received from" else "Paid to") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                FlowRow(modifier = Modifier.padding(top = 12.dp)) {
                    categories.forEach { cat ->
                        AssistChip(
                            onClick = { category = cat },
                            label = { Text(if (cat == category) "\u2713 $cat" else cat) },
                            modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                        )
                    }
                }
            }
        }
    )
}
