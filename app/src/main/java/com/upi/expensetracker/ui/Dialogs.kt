package com.upi.expensetracker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.upi.expensetracker.data.Categories
import com.upi.expensetracker.data.Transaction
import com.upi.expensetracker.data.TxnType

/** Predefined categories for the type, plus any custom ones the user has used. */
private fun categoriesFor(type: String, used: List<String>): List<String> {
    val base = Categories.forType(type)
    val extras = used.filter { it !in base && it != Categories.UNCATEGORIZED && it != Categories.INCOME }
    return base + extras
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var showCustom by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf("") }

    FlowRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        options.forEach { cat ->
            FilterChip(
                selected = cat == selected,
                onClick = { onSelect(cat) },
                label = { Text(cat) },
                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
            )
        }
        FilterChip(
            selected = showCustom,
            onClick = { showCustom = !showCustom },
            label = { Text("+ Custom") },
            modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
        )
    }
    if (showCustom) {
        OutlinedTextField(
            value = custom,
            onValueChange = {
                custom = it
                if (it.isNotBlank()) onSelect(it.trim())
            },
            label = { Text("New category name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    usedCategories: List<String>,
    onDismiss: () -> Unit,
    onAdd: (Double, String, String, String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amount by remember { mutableStateOf("") }
    var payee by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TxnType.DEBIT) }

    val options = categoriesFor(type, usedCategories)
    var category by remember { mutableStateOf(options.first()) }
    if (category !in options) category = options.first()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            Text("Add transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == TxnType.DEBIT,
                    onClick = { type = TxnType.DEBIT; category = categoriesFor(TxnType.DEBIT, usedCategories).first() },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Spent") }
                SegmentedButton(
                    selected = type == TxnType.CREDIT,
                    onClick = { type = TxnType.CREDIT; category = categoriesFor(TxnType.CREDIT, usedCategories).first() },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Received") }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Amount (\u20B9)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(if (type == TxnType.CREDIT) "What for? (e.g. salary)" else "What for? (e.g. biscuit)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = payee,
                onValueChange = { payee = it },
                label = { Text(if (type == TxnType.CREDIT) "Received from (optional)" else "Paid to (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Text("Category", style = MaterialTheme.typography.labelLarge)
            CategoryChips(options = options, selected = category, onSelect = { category = it })

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val value = amount.toDoubleOrNull()
                    if (value != null && value > 0) onAdd(value, payee, note, type, category)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add transaction") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    transaction: Transaction,
    usedCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isCredit = transaction.type == TxnType.CREDIT
    var note by remember { mutableStateOf(transaction.note) }
    val options = categoriesFor(transaction.type, usedCategories)
    var category by remember {
        mutableStateOf(if (transaction.category in options) transaction.category else options.first())
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            Text(
                if (isCredit) "Where did this come from?" else "What did you pay for?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                (if (isCredit) "+" else "-") + money(transaction.amount) +
                    (if (transaction.payee.isNotBlank()) "  \u00B7  ${transaction.payee}" else ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                label = { Text(if (isCredit) "What for? (e.g. salary)" else "What for? (e.g. biscuit)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Text("Category", style = MaterialTheme.typography.labelLarge)
            CategoryChips(options = options, selected = category, onSelect = { category = it })

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(category, note) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
        }
    }
}
