package com.upi.expensetracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upi.expensetracker.data.CategoryTotal

@Composable
fun ReportsScreen(viewModel: ExpenseViewModel, modifier: Modifier = Modifier) {
    val weekSpend by viewModel.weekSpend.collectAsState()
    val weekIncome by viewModel.weekIncome.collectAsState()
    val monthSpend by viewModel.monthSpend.collectAsState()
    val monthIncome by viewModel.monthIncome.collectAsState()
    val weekByCat by viewModel.weekByCategory.collectAsState()
    val monthByCat by viewModel.monthByCategory.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ReportCard("This week", weekIncome, weekSpend, weekByCat)
        ReportCard("This month", monthIncome, monthSpend, monthByCat)
    }
}

@Composable
private fun ReportCard(
    title: String,
    income: Double,
    spend: Double,
    spendByCategory: List<CategoryTotal>
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            SummaryLine("Received", money(income))
            SummaryLine("Spent", money(spend))
            SummaryLine("Balance", money(income - spend))

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Spending by category", style = MaterialTheme.typography.labelLarge)

            if (spendByCategory.isEmpty()) {
                Text(
                    "No spends recorded yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                spendByCategory.forEach { row ->
                    val pct = if (spend > 0) (row.total / spend * 100).toInt() else 0
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${row.category}  ($pct%)")
                        Text(money(row.total))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
