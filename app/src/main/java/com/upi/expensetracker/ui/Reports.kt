package com.upi.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upi.expensetracker.data.CategoryTotal
import com.upi.expensetracker.ui.theme.IncomeGreen
import com.upi.expensetracker.ui.theme.SpendRed

@Composable
fun ReportsScreen(viewModel: ExpenseViewModel, modifier: Modifier = Modifier) {
    val weekSpend by viewModel.weekSpend.collectAsState()
    val weekIncome by viewModel.weekIncome.collectAsState()
    val monthSpend by viewModel.monthSpend.collectAsState()
    val monthIncome by viewModel.monthIncome.collectAsState()
    val weekByCat by viewModel.weekByCategory.collectAsState()
    val monthByCat by viewModel.monthByCategory.collectAsState()
    val opening by viewModel.openingBalance.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ReportCard("This week", 0.0, weekIncome, weekSpend, weekByCat)
        Spacer(Modifier.height(16.dp))
        ReportCard("This month", opening, monthIncome, monthSpend, monthByCat)
    }
}

@Composable
private fun ReportCard(
    title: String,
    opening: Double,
    income: Double,
    spend: Double,
    spendByCategory: List<CategoryTotal>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Pill("Received", money(income), IncomeGreen, Modifier.weight(1f))
                Pill("Spent", money(spend), SpendRed, Modifier.weight(1f))
                Pill("Balance", money(opening + income - spend), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            }
            if (opening != 0.0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Includes opening balance of ${money(opening)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))
            Text("Where it went", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            if (spendByCategory.isEmpty()) {
                Text(
                    "No spends recorded yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                spendByCategory.forEach { row ->
                    CategoryBar(row.category, row.total, spend)
                }
            }
        }
    }
}

@Composable
private fun Pill(label: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
private fun CategoryBar(category: String, amount: Double, total: Double) {
    val fraction = if (total > 0) (amount / total).toFloat().coerceIn(0f, 1f) else 0f
    val pct = (fraction * 100).toInt()
    val style = CategoryStyle.of(category)

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(style.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${money(amount)}  ($pct%)", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            // Progress track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(style.color)
                )
            }
        }
    }
}
