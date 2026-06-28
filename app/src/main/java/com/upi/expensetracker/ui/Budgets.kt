package com.upi.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.upi.expensetracker.data.Categories
import com.upi.expensetracker.data.TxnType

@Composable
fun BudgetsScreen(viewModel: ExpenseViewModel, modifier: Modifier = Modifier) {
    val budgets by viewModel.budgetsForMonth.collectAsState()
    val monthLabel by viewModel.budgetMonthLabel.collectAsState()
    val custom by viewModel.customCategories.collectAsState()

    val allCategories = remember(custom) {
        (Categories.EXPENSE + custom.filter { it.type == TxnType.DEBIT }.map { it.name }).distinct()
    }
    val budgetMap = budgets.associate { it.category to it.amount }
    // Show only categories you've chosen (those with a budget this month).
    val shown = allCategories.filter { budgetMap.containsKey(it) }
    val available = allCategories.filter { !budgetMap.containsKey(it) }

    var editing by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var iconEditing by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        item {
            Text("Monthly budgets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Track only the categories you want. Add a category to set its target; " +
                    "set a budget to 0 to remove it from this list.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.changeBudgetMonth(-1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                }
                Text(monthLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.changeBudgetMonth(1) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        items(shown, key = { it }) { cat ->
            BudgetRow(cat, budgetMap[cat], onIconClick = { iconEditing = cat }) { editing = cat }
        }

        item {
            Spacer(Modifier.height(12.dp))
            if (available.isNotEmpty()) {
                OutlinedButton(
                    onClick = { adding = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add a category")
                }
            }
            if (budgets.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.copyPreviousMonthBudgets() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy last month's budgets")
                }
            }
        }
    }

    editing?.let { cat ->
        BudgetDialog(
            category = cat,
            month = monthLabel,
            current = budgetMap[cat] ?: 0.0,
            onDismiss = { editing = null },
            onSave = { amount ->
                viewModel.setBudget(cat, amount)
                editing = null
            }
        )
    }

    if (adding) {
        AddCategoryDialog(
            available = available,
            onDismiss = { adding = false },
            onPick = { cat ->
                adding = false
                editing = cat
            }
        )
    }

    iconEditing?.let { cat ->
        IconPickerDialog(
            category = cat,
            onDismiss = { iconEditing = null },
            onPick = { key ->
                viewModel.setCategoryIcon(cat, key)
                iconEditing = null
            }
        )
    }
}

@Composable
private fun IconPickerDialog(
    category: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Icon for $category") },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.heightIn(max = 360.dp)
            ) {
                gridItems(IconPack.keys, key = { it }) { key ->
                    val vector = IconPack.icon(key)
                    if (vector != null) {
                        Box(
                            modifier = Modifier
                                .padding(6.dp)
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onPick(key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(vector, contentDescription = key, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddCategoryDialog(
    available: List<String>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Add a category") },
        text = {
            androidx.compose.foundation.layout.FlowRow {
                available.forEach { cat ->
                    androidx.compose.material3.AssistChip(
                        onClick = { onPick(cat) },
                        label = { Text(cat) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun BudgetRow(category: String, amount: Double?, onIconClick: () -> Unit, onClick: () -> Unit) {
    val style = categoryStyleFor(category)
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
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .background(style.color.copy(alpha = 0.15f))
                    .clickable { onIconClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(style.icon, contentDescription = "Change icon", tint = style.color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(category, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(
                if (amount != null && amount > 0) money(amount) else "Set",
                color = if (amount != null && amount > 0) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun BudgetDialog(
    category: String,
    month: String,
    current: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var text by remember { mutableStateOf(if (current > 0) current.toLong().toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(text.toDoubleOrNull() ?: 0.0) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("$category budget") },
        text = {
            Column {
                Text(
                    "Target for $category in $month. Set to 0 to remove it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (\u20B9)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}
